/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.model.CovidException
import com.lunabeestudio.stopcovid.model.NeedRegisterException
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ManageDataViewModel(private val robertManager: RobertManager) : ViewModel() {

    val eraseLocalSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val eraseRemoteSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val eraseAlertSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val quitStopCovidSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val covidException: SingleLiveEvent<CovidException> = SingleLiveEvent()
    val loadingInProgress: MutableLiveData<Boolean> = MutableLiveData(false)

    fun eraseLocalHistory() {
        callWS(robertManager::eraseLocalHistory, eraseLocalSuccess)
    }

    fun eraseRemoteExposureHistory() {
        if (robertManager.isRegistered) {
            callWS(robertManager::eraseRemoteExposureHistory, eraseRemoteSuccess)
        } else {
            covidException.postValue(NeedRegisterException())
        }
    }

    fun eraseRemoteAlert() {
        callWS(robertManager::eraseRemoteAlert, eraseAlertSuccess)
    }

    fun quitStopCovid(application: RobertApplication) {
        if (robertManager.isRegistered) {
            if (loadingInProgress.value == false) {
                viewModelScope.launch(Dispatchers.IO) {
                    loadingInProgress.postValue(true)
                    when (val result = robertManager.quitStopCovid(application)) {
                        is RobertResult.Success -> quitStopCovidSuccess.postValue(null)
                        is RobertResult.Failure -> covidException.postValue(result.error.toCovidException())
                    }
                    loadingInProgress.postValue(false)
                }
            }
        } else {
            covidException.postValue(NeedRegisterException())
        }
    }

    private fun callWS(callWS: suspend () -> RobertResult, successLiveEvent: SingleLiveEvent<*>) {
        if (loadingInProgress.value == false) {
            viewModelScope.launch(Dispatchers.IO) {
                loadingInProgress.postValue(true)
                when (val result = callWS()) {
                    is RobertResult.Success -> successLiveEvent.postValue(null)
                    is RobertResult.Failure -> covidException.postValue(result.error.toCovidException())
                }
                loadingInProgress.postValue(false)
            }
        }
    }
}

class ManageDataViewModelFactory(private val robertManager: RobertManager) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ManageDataViewModel(robertManager) as T
    }
}