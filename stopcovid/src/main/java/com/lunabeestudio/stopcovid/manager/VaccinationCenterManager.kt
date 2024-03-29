/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/04/02 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.core.util.AtomicFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.currentVaccinationReferenceDepartmentCode
import com.lunabeestudio.stopcovid.extension.currentVaccinationReferenceLatitude
import com.lunabeestudio.stopcovid.extension.currentVaccinationReferenceLongitude
import com.lunabeestudio.stopcovid.extension.hasChosenPostalCode
import com.lunabeestudio.stopcovid.extension.location
import com.lunabeestudio.stopcovid.extension.zipGeolocVersion
import com.lunabeestudio.stopcovid.model.PostalCodeDetails
import com.lunabeestudio.stopcovid.model.VaccinationCenter
import com.lunabeestudio.stopcovid.model.VaccinationCenterLastUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type
import kotlin.math.abs

class VaccinationCenterManager(private val serverManager: ServerManager) {

    private var gson: Gson = Gson()
    private val postalCodesDetailsType: Type = object : TypeToken<Map<String, PostalCodeDetails>>() {}.type
    private fun getUrl(): String = ConfigConstant.Vaccination.URL

    private val vaccinationCentersType: Type = object : TypeToken<List<VaccinationCenter>>() {}.type

    private val _vaccinationCenters: MutableLiveData<Event<List<VaccinationCenter>>> = MutableLiveData()
    val vaccinationCenters: LiveData<Event<List<VaccinationCenter>>>
        get() = _vaccinationCenters

    fun vaccinationCentersToDisplay(robertManager: RobertManager, sharedPreferences: SharedPreferences): List<VaccinationCenter>? {
        var result = _vaccinationCenters.value?.peekContent()
        currentVaccinationLocation(sharedPreferences)?.let { currentLocation ->
            result = result?.filter { vaccinationCenter ->
                vaccinationCenter.location != null
            }?.sortedBy { vaccinationCenter ->
                currentLocation.distanceTo(vaccinationCenter.location)
            }?.take(robertManager.configuration.vaccinationCentersCount)
        }
        return result
    }

    suspend fun initialize(context: Context, sharedPreferences: SharedPreferences) {
        initializeCurrentDepartmentIfNeeded(context, sharedPreferences)
        loadLocalAndRefresh(context, sharedPreferences)
    }

    private suspend fun loadLocalAndRefresh(context: Context, sharedPreferences: SharedPreferences) {
        if (sharedPreferences.hasChosenPostalCode) {
            loadLocal(context, sharedPreferences)?.let { vaccinationCenters ->
                if (_vaccinationCenters.value?.peekContent() != vaccinationCenters) {
                    _vaccinationCenters.postValue(Event(vaccinationCenters))
                }
            }
        }
    }

    private suspend fun initializeCurrentDepartmentIfNeeded(context: Context, sharedPreferences: SharedPreferences) {
        if (
            (
                sharedPreferences.currentVaccinationReferenceDepartmentCode == null
                    || sharedPreferences.zipGeolocVersion < ZIP_GEOLOC_VERSION
                )
            && sharedPreferences.chosenPostalCode != null
        ) {
            postalCodeDidUpdate(context, sharedPreferences, sharedPreferences.chosenPostalCode)
        }
    }

    suspend fun postalCodeDidUpdate(context: Context, sharedPreferences: SharedPreferences, postalCode: String?) {
        if (postalCode == null) {
            // Clear all data
            Timber.d("Updating postal code to null")
            clearAllData(context)
        } else {
            // Postal code changed, update department and location
            Timber.d("Postal code did update")
            val foundDetails: PostalCodeDetails? = postalCodeDetails(context, postalCode)
            sharedPreferences.currentVaccinationReferenceLatitude = foundDetails?.latitude
            sharedPreferences.currentVaccinationReferenceLongitude = foundDetails?.longitude
            sharedPreferences.zipGeolocVersion = ZIP_GEOLOC_VERSION
            if (foundDetails?.department != sharedPreferences.currentVaccinationReferenceDepartmentCode
                || _vaccinationCenters.value?.peekContent().isNullOrEmpty()
            ) {
                // Department changed, let's download the new infos
                Timber.d("Department code did update")
                sharedPreferences.currentVaccinationReferenceDepartmentCode = foundDetails?.department
                _vaccinationCenters.postValue(Event(emptyList()))
                loadLocalAndRefresh(context, sharedPreferences)
                fetchLastAndRefresh(context, sharedPreferences)
            } else {
                // Department didn't changed, refresh the list in case location change the order of the centers
                Timber.d("Department code didn't update")
                _vaccinationCenters.value?.peekContent()?.let {
                    _vaccinationCenters.postValue(Event(it))
                }
            }
        }
    }

    private suspend fun postalCodeDetails(context: Context, postalCode: String): PostalCodeDetails? {
        Timber.d("looking for nearest postal code")
        return postalCodesDetails(context)?.let { postalCodesDetails ->
            var foundDetails = postalCodesDetails[postalCode]
            if (foundDetails == null) {
                Timber.d("We couldn't find the exact postal code, let's look for the nearest")
                postalCode.toIntOrNull()?.let { postalCodeInt ->
                    val sameDepartmentPostalCodes = postalCodesDetails
                        .filter {
                            val range = IntRange(0, 1)
                            it.key.substring(range) == postalCode.substring(range)
                        }
                        .mapNotNull { it.key.toIntOrNull() }
                        .sortedBy { abs(it - postalCodeInt) }
                    foundDetails = postalCodesDetails[sameDepartmentPostalCodes.firstOrNull().toString()]
                }
            }
            Timber.d("Postal code entered : $postalCode, postal code found : ${foundDetails?.department ?: "Not found"}")
            foundDetails
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun postalCodesDetails(context: Context): Map<String, PostalCodeDetails>? {
        return withContext(Dispatchers.IO) {
            gson.fromJson(
                context.assets.open(ConfigConstant.Vaccination.ASSET_ZIP_GEOLOC_FILE_PATH).use {
                    it.readBytes().toString(Charsets.UTF_8)
                },
                postalCodesDetailsType
            )
        }
    }

    suspend fun onAppForeground(context: Context, sharedPreferences: SharedPreferences) {
        fetchLastAndRefresh(context, sharedPreferences)
    }

    private suspend fun fetchLastAndRefresh(context: Context, sharedPreferences: SharedPreferences) {
        if (sharedPreferences.hasChosenPostalCode && sharedPreferences.currentVaccinationReferenceDepartmentCode != null) {
            if (fetchLast(context, sharedPreferences)) {
                loadLocalAndRefresh(context, sharedPreferences)
            }
        }
    }

    private fun currentVaccinationLocation(sharedPreferences: SharedPreferences): Location? {
        val currentVaccinationReferenceLatitude = sharedPreferences.currentVaccinationReferenceLatitude
        val currentVaccinationReferenceLongitude = sharedPreferences.currentVaccinationReferenceLongitude
        return if (currentVaccinationReferenceLatitude != null && currentVaccinationReferenceLongitude != null) {
            Location("").apply {
                latitude = currentVaccinationReferenceLatitude
                longitude = currentVaccinationReferenceLongitude
            }
        } else {
            null
        }
    }

    fun clearAllData(context: Context) {
        Timber.d("Clearing everything related to vaccination centers")
        _vaccinationCenters.postValue(Event(emptyList()))
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.chosenPostalCode = null
        sharedPreferences.currentVaccinationReferenceDepartmentCode = null
        sharedPreferences.currentVaccinationReferenceLatitude = null
        sharedPreferences.currentVaccinationReferenceLongitude = null
    }

    private suspend fun loadLocal(context: Context, sharedPreferences: SharedPreferences): List<VaccinationCenter>? {
        val centersFile = localCentersFile(context, sharedPreferences)
        return if (!centersFile.exists()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    Timber.v("Loading $centersFile to object")
                    gson.fromJson<List<VaccinationCenter>>(centersFile.readText(), vaccinationCentersType)
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
        }
    }

    @SuppressLint("BinaryOperationInTimber")
    private suspend fun fetchLast(context: Context, sharedPreferences: SharedPreferences): Boolean {
        val atomicLastUpdateFile = AtomicFile(localLastUpdateFile(context, sharedPreferences))

        return try {
            val previousVaccinationCenterLastUpdate = if (atomicLastUpdateFile.baseFile.exists()) {
                gson.fromJson(
                    atomicLastUpdateFile.baseFile.readText(),
                    VaccinationCenterLastUpdate::class.java
                )
            } else {
                null
            }

            return serverManager.saveTo(
                "${getUrl()}${sharedPreferences.currentVaccinationReferenceDepartmentCode}/$lastUpdateFileName",
                atomicLastUpdateFile
            ) { data ->
                val vaccinationCenterLastUpdate = gson.fromJson(
                    data.decodeToString(),
                    VaccinationCenterLastUpdate::class.java
                )

                if (vaccinationCenterLastUpdate.sha1 != previousVaccinationCenterLastUpdate?.sha1) {
                    Timber.v(
                        "Downloaded Sha1 (${vaccinationCenterLastUpdate.sha1}) is different than our file " +
                            "Sha1 (${previousVaccinationCenterLastUpdate?.sha1}). Let's fetch the new file"
                    )
                    fetchLastCenters(context, sharedPreferences)
                } else {
                    Timber.d("Previous Sha1 is the same. No need to fetch new centers.")
                    false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Fetching fail for $lastUpdateFileName")
            false
        }
    }

    private suspend fun fetchLastCenters(context: Context, sharedPreferences: SharedPreferences): Boolean {
        val atomicCentersFile = AtomicFile(localCentersFile(context, sharedPreferences))

        return try {
            serverManager.saveTo(
                "${getUrl()}${sharedPreferences.currentVaccinationReferenceDepartmentCode}/$centersFileName",
                atomicCentersFile
            ) { data ->
                val list = gson.fromJson<List<VaccinationCenter?>>(data.decodeToString(), vaccinationCentersType)
                list.all { it != null }
            }
        } catch (e: Exception) {
            Timber.e(e, "Fetching fail for $centersFileName")
            false
        }
    }

    private fun localCentersFile(context: Context, sharedPreferences: SharedPreferences): File = File(
        context.filesDir,
        "${sharedPreferences.currentVaccinationReferenceDepartmentCode}${ConfigConstant.Vaccination.CENTER_SUFFIX}"
    )

    private fun localLastUpdateFile(context: Context, sharedPreferences: SharedPreferences): File = File(
        context.filesDir,
        "${sharedPreferences.currentVaccinationReferenceDepartmentCode}${ConfigConstant.Vaccination.LAST_UPDATE_SUFFIX}"
    )

    companion object {
        private const val centersFileName: String = ConfigConstant.Vaccination.CENTER_FILENAME
        private const val lastUpdateFileName: String = ConfigConstant.Vaccination.CENTER_LAST_UPDATE_FILENAME
        private const val ZIP_GEOLOC_VERSION: Int = 1
    }
}
