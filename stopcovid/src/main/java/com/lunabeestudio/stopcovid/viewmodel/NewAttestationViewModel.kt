/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.domain.model.FormEntry
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.manager.FormManager
import com.lunabeestudio.stopcovid.model.CovidException
import com.lunabeestudio.stopcovid.model.FormField
import com.lunabeestudio.stopcovid.model.UnknownException
import com.lunabeestudio.stopcovid.repository.AttestationRepository
import com.lunabeestudio.stopcovid.widgetshomescreen.AttestationWidget
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone

class NewAttestationViewModel(
    private val secureKeystoreDataSource: SecureKeystoreDataSource,
    private val attestationRepository: AttestationRepository,
    private val formManager: FormManager
) : ViewModel() {

    val attestationGeneratedSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val covidException: SingleLiveEvent<CovidException> = SingleLiveEvent()
    var shouldSaveInfos: Boolean = secureKeystoreDataSource.saveAttestationData ?: false
    val infos: MutableMap<String, FormEntry> = (secureKeystoreDataSource.savedAttestationData ?: mapOf()).toMutableMap()
    private val dateFormat: DateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT)
    private val timeFormat: DateFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)

    fun getInfoForFormField(formField: FormField): FormEntry? {
        return infos[formField.dataKeyValue]?.takeIf { it.key == formField.key }
    }

    fun generateQrCode(robertManager: RobertManager, strings: LocalizedStrings, context: Context?) {
        viewModelScope.launch {
            try {
                secureKeystoreDataSource.saveAttestationData = shouldSaveInfos
                infos.keys.forEach { key ->
                    infos[key] = FormEntry(infos[key]?.value?.trim(), infos[key]!!.type, key)
                }
                val infosCopy = infos.toMutableMap()
                val now = Calendar.getInstance().time
                timeFormat.apply {
                    timeZone = TimeZone.getDefault()
                }
                infosCopy[Constants.Attestation.KEY_CREATION_DATE] = FormEntry(
                    dateFormat.format(now),
                    "text",
                    Constants.Attestation.KEY_CREATION_DATE
                )
                infosCopy[Constants.Attestation.KEY_CREATION_HOUR] = FormEntry(
                    timeFormat.format(now),
                    "text",
                    Constants.Attestation.KEY_CREATION_HOUR
                )

                attestationRepository.addAttestation(robertManager, secureKeystoreDataSource, strings, infosCopy)
                context?.let { AttestationWidget.updateWidget(it, true, infos) }

                if (shouldSaveInfos) {
                    infos.remove(Constants.Attestation.KEY_DATE_TIME)
                    infos.remove(Constants.Attestation.DATA_KEY_REASON)
                    secureKeystoreDataSource.savedAttestationData = infos
                }
                attestationGeneratedSuccess.postValue(null)
            } catch (e: Exception) {
                covidException.postValue((e as? CovidException) ?: UnknownException(e.message ?: ""))
            }
        }
    }

    fun pickFormEntry(key: String, formEntry: FormEntry) {
        infos[key] = formEntry
    }

    fun resetInfos() {
        infos.clear()
        infos.putAll(secureKeystoreDataSource.savedAttestationData ?: mapOf())
    }

    fun areInfosValid(): Boolean {
        return formManager.form.value?.peekContent()?.all { formFields ->
            formFields.all { formField ->
                !infos[formField.dataKeyValue]?.value.isNullOrBlank()
            }
        } ?: false
    }
}

class NewAttestationViewModelFactory(
    private val secureKeystoreDataSource: SecureKeystoreDataSource,
    private val attestationRepository: AttestationRepository,
    private val formManager: FormManager,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NewAttestationViewModel(secureKeystoreDataSource, attestationRepository, formManager) as T
    }
}