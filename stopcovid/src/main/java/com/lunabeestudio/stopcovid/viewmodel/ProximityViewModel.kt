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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.extension.isExpired
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.model.CovidException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.hours

class ProximityViewModel(
    private val robertManager: RobertManager,
    keystoreDataSource: LocalKeystoreDataSource,
) : ViewModel() {

    val refreshConfigSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val activateProximitySuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val covidException: SingleLiveEvent<CovidException?> = SingleLiveEvent()
    val loadingInProgress: MutableLiveData<Boolean> = MutableLiveData(false)

    @OptIn(ExperimentalTime::class)
    val activeAttestationCount: LiveData<Int> = keystoreDataSource.attestationsLiveData.map {
        it?.filter { attestation ->
            !attestation.isExpired(robertManager)
        }?.count() ?: 0
    }

    fun refreshConfig(application: RobertApplication) {
        loadingInProgress.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = robertManager.refreshConfig(application)) {
                is RobertResult.Success -> {
                    refreshConfigSuccess.postValue(null)
                }
                is RobertResult.Failure -> {
                    covidException.postValue(result.error.toCovidException())
                }
            }
            loadingInProgress.postValue(false)
        }
    }

    fun activateProximity(application: RobertApplication) {
        loadingInProgress.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = robertManager.activateProximity(application)) {
                is RobertResult.Success -> {
                    activateProximitySuccess.postValue(null)
                }
                is RobertResult.Failure -> {
                    covidException.postValue(result.error.toCovidException())
                }
            }
            loadingInProgress.postValue(false)
        }
    }
}

class ProximityViewModelFactory(
    private val robertManager: RobertManager,
    private val keystoreDataSource: LocalKeystoreDataSource,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProximityViewModel(robertManager, keystoreDataSource) as T
    }
}
