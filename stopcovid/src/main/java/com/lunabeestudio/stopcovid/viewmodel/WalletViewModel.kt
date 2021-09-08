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
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.extension.isOld
import com.lunabeestudio.stopcovid.extension.isRecent
import com.lunabeestudio.stopcovid.manager.Blacklist2DDOCManager
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.manager.WalletManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate

class WalletViewModel constructor(
    private val robertManager: RobertManager,
    private val keystoreDataSource: LocalKeystoreDataSource,
    private val blacklistDCCManager: BlacklistDCCManager,
    private val blacklist2DDOCManager: Blacklist2DDOCManager,
) : ViewModel() {

    private val _scrollEvent: MutableLiveData<Event<WalletCertificate>> = MutableLiveData()
    val scrollEvent: LiveData<Event<WalletCertificate>>
        get() = _scrollEvent

    private var previousCertificatesId = emptyList<String>()
    val certificates: LiveData<List<WalletCertificate>?>
        get() = WalletManager.walletCertificateLiveData
            .map { certificates ->
                if (previousCertificatesId.isNotEmpty()) {
                    certificates
                        ?.find { it.id !in previousCertificatesId }
                        ?.let { _scrollEvent.value = Event(it) }
                }
                previousCertificatesId = certificates?.map { it.id }.orEmpty()
                certificates
            }

    val blacklistDCC: LiveData<List<String>?>
        get() = blacklistDCCManager.blacklistedDCCHashes

    val blacklist2DDOC: LiveData<List<String>?>
        get() = blacklist2DDOCManager.blacklisted2DDOCHashes

    val certificatesCount: LiveData<Int> = WalletManager.walletCertificateLiveData.map { it?.size ?: 0 }

    val recentCertificates: List<WalletCertificate>?
        get() = WalletManager.walletCertificateLiveData.value?.filter {
            it.isRecent(robertManager.configuration) &&
                (it as? EuropeanCertificate)?.isFavorite != true
        }?.sortedByDescending { it.timestamp }

    val olderCertificates: List<WalletCertificate>?
        get() = WalletManager.walletCertificateLiveData.value?.filter {
            it.isOld(robertManager.configuration) &&
                (it as? EuropeanCertificate)?.isFavorite != true
        }?.sortedByDescending { it.timestamp }

    val favoriteCertificates: List<WalletCertificate>?
        get() = WalletManager.walletCertificateLiveData.value?.filter {
            (it as? EuropeanCertificate)?.isFavorite == true
        }?.sortedByDescending { it.timestamp }

    init {
        WalletManager.refreshWalletIfNeeded(keystoreDataSource)
    }

    fun removeCertificate(certificate: WalletCertificate) {
        WalletManager.deleteCertificate(keystoreDataSource, certificate)
    }

    fun isEmpty(): Boolean {
        return WalletManager.walletCertificateLiveData.value.isNullOrEmpty()
    }

    fun saveCertificate(walletCertificate: WalletCertificate) {
        WalletManager.saveCertificate(
            keystoreDataSource,
            walletCertificate,
        )
    }

    fun toggleFavorite(
        walletCertificate: EuropeanCertificate
    ) {
        WalletManager.toggleFavorite(
            keystoreDataSource,
            walletCertificate
        )
    }

    fun isBlacklisted(certificate: WalletCertificate): Boolean {
        return when (certificate) {
            is FrenchCertificate -> blacklist2DDOC.value?.contains(certificate.sha256) == true
            is EuropeanCertificate -> blacklistDCC.value?.contains(certificate.sha256) == true
        }
    }
}

class WalletViewModelFactory(
    private val robertManager: RobertManager,
    private val secureKeystoreDataSource: SecureKeystoreDataSource,
    private val blacklistDCCManager: BlacklistDCCManager,
    private val blacklist2DDOCManager: Blacklist2DDOCManager,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WalletViewModel(robertManager, secureKeystoreDataSource, blacklistDCCManager, blacklist2DDOCManager) as T
    }
}
