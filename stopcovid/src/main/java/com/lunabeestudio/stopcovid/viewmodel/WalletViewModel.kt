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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.isOld
import com.lunabeestudio.stopcovid.extension.isRecent
import com.lunabeestudio.stopcovid.manager.WalletManager
import com.lunabeestudio.stopcovid.model.WalletCertificate

class WalletViewModel(private val robertManager: RobertManager, private val keystoreDataSource: LocalKeystoreDataSource) : ViewModel() {

    val certificates: LiveData<List<WalletCertificate>?>
        get() = WalletManager.walletCertificateLiveData

    val recentCertificates: List<WalletCertificate>?
        get() = certificates.value?.filter {
            it.isRecent(robertManager.configuration)
        }?.sortedByDescending { it.timestamp }

    val olderCertificates: List<WalletCertificate>?
        get() = certificates.value?.filter {
            it.isOld(robertManager.configuration)
        }?.sortedByDescending { it.timestamp }

    fun removeCertificate(certificate: WalletCertificate) {
        WalletManager.deleteCertificate(keystoreDataSource, certificate)
    }

    fun isEmpty(): Boolean {
        return certificates.value.isNullOrEmpty()
    }
}

class WalletViewModelFactory(private val robertManager: RobertManager, private val secureKeystoreDataSource: SecureKeystoreDataSource) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WalletViewModel(robertManager, secureKeystoreDataSource) as T
    }
}