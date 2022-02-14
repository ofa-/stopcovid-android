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

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.coreui.model.WalletState
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.extension.isExpired
import com.lunabeestudio.stopcovid.extension.showSmartWallet
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.manager.IsolationFormStateEnum
import com.lunabeestudio.stopcovid.manager.IsolationManager
import com.lunabeestudio.stopcovid.manager.VaccinationCenterManager
import com.lunabeestudio.stopcovid.model.CovidException
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.SmartWalletMap
import com.lunabeestudio.stopcovid.model.SmartWalletState
import com.lunabeestudio.stopcovid.repository.VenueRepository
import com.lunabeestudio.stopcovid.repository.WalletRepository
import com.lunabeestudio.stopcovid.usecase.GetSmartWalletMapUseCase
import com.lunabeestudio.stopcovid.usecase.GetSmartWalletStateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val robertManager: RobertManager,
    isolationManager: IsolationManager,
    keystoreDataSource: SecureKeystoreDataSource,
    vaccinationCenterManager: VaccinationCenterManager,
    venueRepository: VenueRepository,
    walletRepository: WalletRepository,
    private val sharedPreferences: SharedPreferences,
    getSmartWalletMapMapUseCase: GetSmartWalletMapUseCase,
    private val getSmartWalletStateUseCase: GetSmartWalletStateUseCase,
) : CommonDataViewModel(keystoreDataSource, robertManager, isolationManager, vaccinationCenterManager, venueRepository, walletRepository) {

    val activateProximitySuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val covidException: SingleLiveEvent<CovidException?> = SingleLiveEvent()
    val loadingInProgress: MutableLiveData<Boolean> = MutableLiveData(false)
    val isolationFormState: LiveData<Event<IsolationFormStateEnum?>> = isolationManager.currentFormState
    val isolationDataChanged: SingleLiveEvent<Unit> = isolationManager.changedEvent
    val clearDataSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()

    val activeAttestationCount: LiveData<Event<Int>> = keystoreDataSource.attestationsFlow.map {
        Event(
            it.filter { attestation ->
                !attestation.isExpired(robertManager.configuration)
            }.count()
        )
    }.asLiveData(timeoutInMs = 0L)

    val venuesQrCodeLiveData: LiveData<List<VenueQrCode>> = venueRepository.venuesQrCodeFlow.asLiveData(timeoutInMs = 0)

    val favoriteDcc: LiveData<Event<EuropeanCertificate?>> = walletRepository.walletCertificateFlow.map { list ->
        Event(
            list
                .data
                ?.filterIsInstance<EuropeanCertificate>()
                ?.firstOrNull { certificate ->
                    (certificate as? EuropeanCertificate)?.isFavorite == true
                }
        )
    }.asLiveData(timeoutInMs = 0)

    val smartWalletMap: StateFlow<SmartWalletMap?> = getSmartWalletMapMapUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    suspend fun refreshConfig(application: RobertApplication): Boolean {
        loadingInProgress.postValue(true)
        val result = robertManager.refreshConfig(application)
        loadingInProgress.postValue(false)
        return when (result) {
            is RobertResult.Success -> {
                true
            }
            is RobertResult.Failure -> {
                covidException.postValue(result.error.toCovidException())
                false
            }
        }
    }

    fun activateProximity(application: RobertApplication) {
        loadingInProgress.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = robertManager.activateProximity(application)) {
                is RobertResult.Success -> {
                    activateProximitySuccess.postValue(Unit)
                }
                is RobertResult.Failure -> {
                    covidException.postValue(result.error.toCovidException())
                }
            }
            loadingInProgress.postValue(false)
        }
    }

    fun clearData(application: RobertApplication) {
        loadingInProgress.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            clearLocalData(application)
            loadingInProgress.postValue(false)
            clearDataSuccess.postValue(Unit)
        }
    }

    fun getWalletState(): WalletState {
        var result = WalletState.NONE
        if (sharedPreferences.showSmartWallet && robertManager.configuration.isSmartWalletOn) {
            smartWalletMap.value?.forEach { (_, certificate) ->
                val smartWalletState = getSmartWalletStateUseCase(certificate)
                val state = when (smartWalletState) {
                    is SmartWalletState.Expired -> WalletState.ALERT
                    is SmartWalletState.ExpireSoon -> WalletState.WARNING
                    is SmartWalletState.Eligible -> WalletState.ELIGIBLE
                    is SmartWalletState.EligibleSoon -> WalletState.ELIGIBLE_SOON
                    is SmartWalletState.Valid -> WalletState.NONE
                }
                // Take highest state
                if (result.ordinal < state.ordinal) {
                    result = state
                }
            }
        }
        return result
    }
}

class HomeViewModelFactory(
    private val robertManager: RobertManager,
    private val isolationManager: IsolationManager,
    private val keystoreDataSource: SecureKeystoreDataSource,
    private val vaccinationCenterManager: VaccinationCenterManager,
    private val venueRepository: VenueRepository,
    private val walletRepository: WalletRepository,
    private val sharedPreferences: SharedPreferences,
    private val getSmartWalletMapUseCase: GetSmartWalletMapUseCase,
    private val getSmartWalletStateUseCase: GetSmartWalletStateUseCase,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(
            robertManager,
            isolationManager,
            keystoreDataSource,
            vaccinationCenterManager,
            venueRepository,
            walletRepository,
            sharedPreferences,
            getSmartWalletMapUseCase,
            getSmartWalletStateUseCase,
        ) as T
    }
}