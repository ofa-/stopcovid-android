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
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.isOld
import com.lunabeestudio.stopcovid.extension.isRecent
import com.lunabeestudio.stopcovid.manager.DccCertificatesManager
import com.lunabeestudio.stopcovid.manager.WalletManager
import com.lunabeestudio.stopcovid.model.WalletCertificate

class WalletViewModel(
    private val robertManager: RobertManager,
    private val keystoreDataSource: LocalKeystoreDataSource,
    private val dccCertificatesManager: DccCertificatesManager,
) : ViewModel() {

    val certificates: LiveData<List<WalletCertificate>?>
        get() = WalletManager.walletCertificateLiveData

    val certificatesCount: LiveData<Int> = certificates.map { it?.size ?: 0 }

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

    suspend fun processCodeValue(
        context: Context,
        certificateCode: String,
        certificateFormat: WalletCertificateType.Format?
    ): WalletCertificate {
        val certificate = WalletManager.processCertificateCode(
            robertManager,
            keystoreDataSource,
            certificateCode,
            dccCertificatesManager.certificates,
            certificateFormat,
        )
        AnalyticsManager.reportAppEvent(context, AppEventName.e13, null)

        return certificate
    }
}

class WalletViewModelFactory(
    private val robertManager: RobertManager,
    private val secureKeystoreDataSource: SecureKeystoreDataSource,
    private val dccCertificatesManager: DccCertificatesManager
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WalletViewModel(robertManager, secureKeystoreDataSource, dccCertificatesManager) as T
    }
}