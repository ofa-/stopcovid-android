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
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.extension.isOld
import com.lunabeestudio.stopcovid.extension.isRecent
import com.lunabeestudio.stopcovid.extension.raw
import com.lunabeestudio.stopcovid.manager.Blacklist2DDOCManager
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.model.TacResult
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.repository.WalletRepository
import com.lunabeestudio.stopcovid.usecase.GenerateActivityPassState
import com.lunabeestudio.stopcovid.usecase.GenerateActivityPassUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletViewModel(
    private val robertManager: RobertManager,
    private val blacklistDCCManager: BlacklistDCCManager,
    private val blacklist2DDOCManager: Blacklist2DDOCManager,
    private val walletRepository: WalletRepository,
    private val generateActivityPassUseCase: GenerateActivityPassUseCase,
) : ViewModel() {

    val error: SingleLiveEvent<Exception> = SingleLiveEvent()

    private val _scrollEvent: MutableLiveData<Event<WalletCertificate>> = MutableLiveData()
    val scrollEvent: LiveData<Event<WalletCertificate>>
        get() = _scrollEvent

    private var previousCertificatesId = emptyList<String>()
    val certificates: StateFlow<List<WalletCertificate>?> = walletRepository.walletCertificateFlow
        .map { certificates ->
            if (previousCertificatesId.isNotEmpty()) {
                certificates
                    ?.find { it.id !in previousCertificatesId }
                    ?.let { _scrollEvent.value = Event(it) }
            }
            previousCertificatesId = certificates?.map { it.id }.orEmpty()
            certificates
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val blacklistDCC: LiveData<List<String>?>
        get() = blacklistDCCManager.blacklistedDCCHashes

    val blacklist2DDOC: LiveData<List<String>?>
        get() = blacklist2DDOCManager.blacklisted2DDOCHashes

    val certificatesCount: LiveData<Int?> = walletRepository.certificateCountFlow.asLiveData()

    val migrationInProgress: LiveData<Boolean> = walletRepository.migrationInProgress

    val recentCertificates: List<WalletCertificate>?
        get() = certificates.value?.filter {
            it.isRecent(robertManager.configuration) &&
                (it as? EuropeanCertificate)?.isFavorite != true
        }?.sortedByDescending { it.timestamp }

    val olderCertificates: List<WalletCertificate>?
        get() = certificates.value?.filter {
            it.isOld(robertManager.configuration) &&
                (it as? EuropeanCertificate)?.isFavorite != true
        }?.sortedByDescending { it.timestamp }

    val favoriteCertificates: List<WalletCertificate>?
        get() = certificates.value?.filter {
            (it as? EuropeanCertificate)?.isFavorite == true
        }?.sortedByDescending { it.timestamp }

    fun removeCertificate(certificate: WalletCertificate) {
        viewModelScope.launch {
            walletRepository.deleteCertificate(certificate)
        }
    }

    fun deleteDeprecatedCertificates() {
        walletRepository.deleteDeprecatedCertificated()
    }

    fun isEmpty(): Boolean {
        return certificates.value.isNullOrEmpty()
    }

    suspend fun saveCertificate(walletCertificate: WalletCertificate) {
        walletRepository.saveCertificate(walletCertificate)
    }

    fun toggleFavorite(
        walletCertificate: EuropeanCertificate
    ) {
        viewModelScope.launch {
            try {
                walletRepository.toggleFavorite(walletCertificate)
            } catch (e: Exception) {
                error.postValue(e)
            }
        }
    }

    fun isDuplicated(certificate: WalletCertificate): Boolean {
        return walletRepository.certificateExists(certificate)
    }

    fun isBlacklisted(certificate: WalletCertificate): Boolean {
        return when (certificate) {
            is FrenchCertificate -> blacklist2DDOC.value?.contains(certificate.sha256) == true
            is EuropeanCertificate -> blacklistDCC.value?.contains(certificate.sha256) == true
        }
    }

    suspend fun getCertificatesCount(): Int {
        return walletRepository.getCertificateCount()
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
}

class WalletViewModelFactory(
    private val robertManager: RobertManager,
    private val blacklistDCCManager: BlacklistDCCManager,
    private val blacklist2DDOCManager: Blacklist2DDOCManager,
    private val walletRepository: WalletRepository,
    private val generateActivityPassUseCase: GenerateActivityPassUseCase,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WalletViewModel(
            robertManager,
            blacklistDCCManager,
            blacklist2DDOCManager,
            walletRepository,
            generateActivityPassUseCase,
        ) as T
    }
}
