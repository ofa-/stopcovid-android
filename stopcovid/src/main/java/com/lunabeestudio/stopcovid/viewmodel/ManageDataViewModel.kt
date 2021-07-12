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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.manager.IsolationManager
import com.lunabeestudio.stopcovid.model.CovidException
import com.lunabeestudio.stopcovid.model.NeedRegisterException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ManageDataViewModel(
    secureKeystoreDataSource: SecureKeystoreDataSource,
    private val robertManager: RobertManager,
    isolationManager: IsolationManager,
) : CommonDataViewModel(secureKeystoreDataSource, robertManager, isolationManager) {

    val eraseLocalSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val eraseRemoteSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val deleteAnalyticsSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val quitStopCovidSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val covidException: SingleLiveEvent<CovidException> = SingleLiveEvent()
    val loadingInProgress: MutableLiveData<Boolean> = MutableLiveData(false)

    override fun eraseVenues(application: RobertApplication) {
        super.eraseVenues(application)
        eraseLocalSuccess.postValue(null)
    }

    override fun eraseAttestations(context: Context) {
        super.eraseAttestations(context)
        eraseLocalSuccess.postValue(null)
    }

    override fun eraseIsolation() {
        super.eraseIsolation()
        eraseLocalSuccess.postValue(null)
    }

    override fun eraseCertificates() {
        super.eraseCertificates()
        eraseLocalSuccess.postValue(null)
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
        clearNotifications(application)
    }

    fun requestDeleteAnalytics(application: RobertApplication) {
        if (robertManager.isRegistered) {
            if (loadingInProgress.value == false) {
                AnalyticsManager.requestDeleteAnalytics(application.getAppContext())
                viewModelScope.launch(Dispatchers.IO) {
                    loadingInProgress.postValue(true)
                    robertManager.updateStatus(application)
                    loadingInProgress.postValue(false)
                    deleteAnalyticsSuccess.postValue(null)
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
}

class ManageDataViewModelFactory(
    private val secureKeystoreDataSource: SecureKeystoreDataSource,
    private val robertManager: RobertManager,
    private val isolationManager: IsolationManager,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ManageDataViewModel(secureKeystoreDataSource, robertManager, isolationManager) as T
    }
}