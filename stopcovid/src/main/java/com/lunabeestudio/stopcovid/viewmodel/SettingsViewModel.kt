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
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.framework.manager.DebugManager
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.extension.listLogFiles
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.manager.IsolationManager
import com.lunabeestudio.stopcovid.manager.VaccinationCenterManager
import com.lunabeestudio.stopcovid.model.CovidException
import com.lunabeestudio.stopcovid.model.NeedRegisterException
import com.lunabeestudio.stopcovid.repository.VenueRepository
import com.lunabeestudio.stopcovid.repository.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    secureKeystoreDataSource: SecureKeystoreDataSource,
    private val robertManager: RobertManager,
    isolationManager: IsolationManager,
    vaccinationCenterManager: VaccinationCenterManager,
    venueRepository: VenueRepository,
    walletRepository: WalletRepository,
    private val analyticsManager: AnalyticsManager,
    private val debugManager: DebugManager,
) : CommonDataViewModel(
    secureKeystoreDataSource,
    robertManager,
    isolationManager,
    vaccinationCenterManager,
    venueRepository,
    walletRepository
) {

    val venuesQrCodeLiveData: LiveData<List<VenueQrCode>> = venueRepository.venuesQrCodeFlow.asLiveData(timeoutInMs = 0)

    val eraseLocalSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val eraseRemoteSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val deleteAnalyticsSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val quitStopCovidSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val covidException: SingleLiveEvent<CovidException> = SingleLiveEvent()
    val loadingInProgress: MutableLiveData<Boolean> = MutableLiveData(false)

    override fun eraseVenues(application: RobertApplication) {
        super.eraseVenues(application)
        eraseLocalSuccess.postValue(Unit)
    }

    override fun eraseAttestations(context: Context) {
        super.eraseAttestations(context)
        eraseLocalSuccess.postValue(Unit)
    }

    override fun eraseIsolation() {
        super.eraseIsolation()
        eraseLocalSuccess.postValue(Unit)
    }

    override fun eraseCertificates() {
        super.eraseCertificates()
        eraseLocalSuccess.postValue(Unit)
    }

    fun eraseLocalHistory() {
        if (robertManager.isRegistered) {
            if (loadingInProgress.value == false) {
                viewModelScope.launch(Dispatchers.IO) {
                    loadingInProgress.postValue(true)
                    when (val result = robertManager.eraseLocalHistory()) {
                        is RobertResult.Success -> eraseLocalSuccess.postValue(Unit)
                        is RobertResult.Failure -> covidException.postValue(result.error.toCovidException())
                    }
                    loadingInProgress.postValue(false)
                }
            }
        } else {
            covidException.postValue(NeedRegisterException())
        }
    }

    fun eraseRemoteExposureHistory(application: RobertApplication) {
        if (robertManager.isRegistered) {
            if (loadingInProgress.value == false) {
                viewModelScope.launch(Dispatchers.IO) {
                    loadingInProgress.postValue(true)
                    when (val result = robertManager.eraseRemoteExposureHistory(application)) {
                        is RobertResult.Success -> eraseRemoteSuccess.postValue(Unit)
                        is RobertResult.Failure -> covidException.postValue(result.error.toCovidException())
                    }
                    loadingInProgress.postValue(false)
                }
            }
        } else {
            covidException.postValue(NeedRegisterException())
        }
    }

    fun eraseRemoteAlert(application: RobertApplication) {
        clearNotifications(application)
    }

    fun requestDeleteAnalytics(application: RobertApplication) {
        if (robertManager.isRegistered) {
            if (loadingInProgress.value == false) {
                analyticsManager.requestDeleteAnalytics()
                viewModelScope.launch(Dispatchers.IO) {
                    loadingInProgress.postValue(true)
                    robertManager.updateStatus(application)
                    loadingInProgress.postValue(false)
                    deleteAnalyticsSuccess.postValue(Unit)
                }
            }
        } else {
            covidException.postValue(NeedRegisterException())
        }
    }

    fun quitStopCovid(application: RobertApplication) {
        if (robertManager.isRegistered) {
            if (loadingInProgress.value == false) {
                viewModelScope.launch(Dispatchers.IO) {
                    loadingInProgress.postValue(true)
                    when (val result = robertManager.quitStopCovid(application)) {
                        is RobertResult.Success -> {
                            clearLocalData(application)
                            quitStopCovidSuccess.postValue(Unit)
                        }
                        is RobertResult.Failure -> covidException.postValue(result.error.toCovidException())
                    }
                    loadingInProgress.postValue(false)
                }
            }
        } else {
            covidException.postValue(NeedRegisterException())
        }
    }

    fun exportLogs(context: Context, strings: LocalizedStrings) {
        viewModelScope.launch {
            loadingInProgress.postValue(true)
            val shareFile = File(context.cacheDir, "share_log.zip")
            DebugManager.zip(debugManager.logsDir.listLogFiles() ?: emptyList(), shareFile)
            shareLogs(context, shareFile, strings)
            loadingInProgress.postValue(false)
        }
    }

    private fun shareLogs(context: Context, file: File, strings: LocalizedStrings) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.share",
            file
        )

        ShareCompat.IntentBuilder(context)
            .setType("message/rfc822")
            .setEmailTo(arrayOf(strings["manageDataController.logs.email"] ?: "contact@tousanticovid.gouv.fr"))
            .setStream(uri)
            .setSubject(strings["manageDataController.logs.subject"] ?: "Partage des fichiers de logs TAC")
            .startChooser()
    }
}

class SettingsViewModelFactory(
    private val secureKeystoreDataSource: SecureKeystoreDataSource,
    private val robertManager: RobertManager,
    private val isolationManager: IsolationManager,
    private val vaccinationCenterManager: VaccinationCenterManager,
    private val venueRepository: VenueRepository,
    private val walletRepository: WalletRepository,
    private val analyticsManager: AnalyticsManager,
    private val debugManager: DebugManager,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(
            secureKeystoreDataSource,
            robertManager,
            isolationManager,
            vaccinationCenterManager,
            venueRepository,
            walletRepository,
            analyticsManager,
            debugManager
        ) as T
    }
}