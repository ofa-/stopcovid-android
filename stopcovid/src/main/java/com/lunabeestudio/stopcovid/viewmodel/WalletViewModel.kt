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
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.extension.isBlacklisted
import com.lunabeestudio.stopcovid.extension.isOld
import com.lunabeestudio.stopcovid.extension.isRecent
import com.lunabeestudio.stopcovid.extension.raw
import com.lunabeestudio.stopcovid.manager.Blacklist2DDOCManager
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.model.SmartWalletMap
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.repository.WalletRepository
import com.lunabeestudio.stopcovid.usecase.GenerateActivityPassState
import com.lunabeestudio.stopcovid.usecase.GenerateActivityPassUseCase
import com.lunabeestudio.stopcovid.usecase.GetSmartWalletMapUseCase
import com.lunabeestudio.stopcovid.usecase.GetSmartWalletStateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletViewModel(
    private val robertManager: RobertManager,
    private val blacklistDCCManager: BlacklistDCCManager,
    private val blacklist2DDOCManager: Blacklist2DDOCManager,
    private val walletRepository: WalletRepository,
    private val generateActivityPassUseCase: GenerateActivityPassUseCase,
    getSmartWalletMapUseCase: GetSmartWalletMapUseCase,
    private val getSmartWalletStateUseCase: GetSmartWalletStateUseCase,
) : ViewModel() {

    val error: SingleLiveEvent<Exception> = SingleLiveEvent()
    val loading: SingleLiveEvent<Boolean> = SingleLiveEvent()

    private val _scrollEvent: MutableLiveData<Event<WalletCertificate>> = MutableLiveData()
    val scrollEvent: LiveData<Event<WalletCertificate>>
        get() = _scrollEvent

    private var previousCertificatesId = emptyList<String>()
    val certificates: StateFlow<TacResult<List<WalletCertificate>>> = walletRepository.walletCertificateFlow
        .map { certificates ->
            if (previousCertificatesId.isNotEmpty()) {
                certificates
                    .data
                    ?.find { it.id !in previousCertificatesId }
                    ?.let { _scrollEvent.value = Event(it) }
            }
            previousCertificatesId = certificates.data?.map { it.id }.orEmpty()
            loading.postValue(false)
            certificates
        }.stateIn(viewModelScope, SharingStarted.Eagerly, TacResult.Loading(emptyList()))

    val smartWalletProfiles: StateFlow<SmartWalletMap?> = getSmartWalletMapUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val blacklistUpdateEvent: LiveData<Event<Unit>> = MediatorLiveData<Event<Unit>>().apply {
        addSource(blacklistDCCManager.blacklistUpdateEvent) { this.value = it }
        addSource(blacklist2DDOCManager.blacklistUpdateEvent) { this.value = it }
    }

    val certificatesCount: LiveData<Int?> = walletRepository.certificateCountFlow.combine(certificates) { encryptedCount, certificates ->
        certificates.data?.size?.let { encryptedCount.coerceAtMost(it) } ?: encryptedCount
    }.asLiveData()

    val migrationInProgress: LiveData<Boolean> = walletRepository.migrationInProgress

    val recentCertificates: List<WalletCertificate>?
        get() = certificates.value.data?.filter {
            it.isRecent(robertManager.configuration, getSmartWalletStateUseCase) &&
                (it as? EuropeanCertificate)?.isFavorite != true
        }?.sortedByDescending { it.timestamp }

    val olderCertificates: List<WalletCertificate>?
        get() = certificates.value.data?.filter {
            it.isOld(robertManager.configuration, getSmartWalletStateUseCase) &&
                (it as? EuropeanCertificate)?.isFavorite != true
        }?.sortedByDescending { it.timestamp }

    val favoriteCertificates: List<WalletCertificate>?
        get() = certificates.value.data?.filter {
            (it as? EuropeanCertificate)?.isFavorite == true
        }?.sortedByDescending { it.timestamp }

    fun removeCertificate(certificate: WalletCertificate) {
        viewModelScope.launch {
            loading.postValue(true)
            walletRepository.deleteCertificate(certificate)
        }
    }

    fun deleteDeprecatedCertificates() {
        walletRepository.deleteDeprecatedCertificated()
    }

    suspend fun saveCertificate(walletCertificate: WalletCertificate) {
        walletRepository.saveCertificate(walletCertificate)
    }

    fun toggleFavorite(
        walletCertificate: EuropeanCertificate
    ) {
        viewModelScope.launch {
            try {
                loading.postValue(true)
                walletRepository.toggleFavorite(walletCertificate)
            } catch (e: Exception) {
                loading.postValue(false)
                error.postValue(e)
            }
        }
    }

    fun isDuplicated(certificate: WalletCertificate): Boolean {
        return walletRepository.certificateExists(certificate)
    }

    suspend fun isBlacklisted(certificate: WalletCertificate): Boolean {
        return when (certificate) {
            is FrenchCertificate -> certificate.isBlacklisted(blacklist2DDOCManager)
            is EuropeanCertificate -> certificate.isBlacklisted(blacklistDCCManager)
        }
    }

    suspend fun getCertificatesCount(): Int {
        return certificatesCount.value ?: walletRepository.getCertificateCount()
    }

    suspend fun convert2ddocToDcc(certificate: FrenchCertificate): RobertResultData<String> {
        return walletRepository.convertCertificate(
            robertManager,
            certificate.raw,
            WalletCertificateType.Format.WALLET_DCC
        )
    }

    suspend fun getNotExpiredActivityPass(rootCertificateId: String): EuropeanCertificate? {
        return walletRepository.getActivityPass(rootCertificateId, System.currentTimeMillis())
    }

    fun generateActivityPass(certificate: EuropeanCertificate): Flow<TacResult<GenerateActivityPassState>> {
        return generateActivityPassUseCase(certificate)
    }

    suspend fun forceRefreshCertificates() {
        walletRepository.forceRefreshCertificatesFlow()
    }

    fun deleteLostCertificates() {
        viewModelScope.launch {
            walletRepository.deleteLostCertificates()
        }
    }

    fun resetWalletCryptoKeyGeneratedFlag() {
        walletRepository.resetKeyCryptoGeneratedFlag()
    }

    fun requestScrollToId(scrollCertificateId: String?) {
        _scrollEvent.value = certificates.value.data?.find { it.id == scrollCertificateId }?.let {
            Event(it)
        }
    }
}

class WalletViewModelFactory(
    private val robertManager: RobertManager,
    private val blacklistDCCManager: BlacklistDCCManager,
    private val blacklist2DDOCManager: Blacklist2DDOCManager,
    private val walletRepository: WalletRepository,
    private val generateActivityPassUseCase: GenerateActivityPassUseCase,
    private val getSmartWalletMapUseCase: GetSmartWalletMapUseCase,
    private val getSmartWalletStateUseCase: GetSmartWalletStateUseCase,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WalletViewModel(
            robertManager,
            blacklistDCCManager,
            blacklist2DDOCManager,
            walletRepository,
            generateActivityPassUseCase,
            getSmartWalletMapUseCase,
            getSmartWalletStateUseCase,
        ) as T
    }
}
