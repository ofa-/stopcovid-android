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

import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.model.CovidException
import com.lunabeestudio.stopcovid.model.NeedRegisterException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ManageDataViewModel(
    private val secureKeystoreDataSource: SecureKeystoreDataSource,
    private val robertManager: RobertManager,
) : ViewModel() {

    val eraseAttestationsSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val eraseLocalSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val eraseRemoteSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val eraseAlertSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val quitStopCovidSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val covidException: SingleLiveEvent<CovidException> = SingleLiveEvent()
    val loadingInProgress: MutableLiveData<Boolean> = MutableLiveData(false)

    fun eraseAttestations() {
        secureKeystoreDataSource.savedAttestationData = null
        secureKeystoreDataSource.saveAttestationData = null
        secureKeystoreDataSource.attestations = null
        eraseAttestationsSuccess.postValue(null)
    }

    fun eraseLocalHistory() {
        if (robertManager.isRegistered) {
            if (loadingInProgress.value == false) {
                viewModelScope.launch(Dispatchers.IO) {
                    loadingInProgress.postValue(true)
                    when (val result = robertManager.eraseLocalHistory()) {
                        is RobertResult.Success -> eraseLocalSuccess.postValue(null)
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
                        is RobertResult.Success -> eraseRemoteSuccess.postValue(null)
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
        if (robertManager.isRegistered) {
            if (loadingInProgress.value == false) {
                viewModelScope.launch(Dispatchers.IO) {
                    loadingInProgress.postValue(true)
                    when (val result = robertManager.eraseRemoteAlert()) {
                        is RobertResult.Success -> {
                            clearNotifications(application)
                            eraseAlertSuccess.postValue(null)
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

    fun quitStopCovid(application: RobertApplication) {
        if (robertManager.isRegistered) {
            if (loadingInProgress.value == false) {
                viewModelScope.launch(Dispatchers.IO) {
                    loadingInProgress.postValue(true)
                    when (val result = robertManager.quitStopCovid(application)) {
                        is RobertResult.Success -> {
                            WorkManager.getInstance(application.getAppContext()).cancelUniqueWork(Constants.WorkerNames.NOTIFICATION)
                            clearNotifications(application)
                            eraseAttestations()
                            (application.getAppContext() as StopCovid).cancelReminder()
                            PreferenceManager.getDefaultSharedPreferences(application.getAppContext()).chosenPostalCode = null
                            quitStopCovidSuccess.postValue(null)
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

    private fun clearNotifications(application: RobertApplication) {
        val notificationManager = application.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(UiConstants.Notification.ERROR.notificationId)
        notificationManager.cancel(UiConstants.Notification.TIME.notificationId)
        notificationManager.cancel(UiConstants.Notification.AT_RISK.notificationId)
    }
}

class ManageDataViewModelFactory(private val secureKeystoreDataSource: SecureKeystoreDataSource, private val robertManager: RobertManager) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ManageDataViewModel(secureKeystoreDataSource, robertManager) as T
    }
}