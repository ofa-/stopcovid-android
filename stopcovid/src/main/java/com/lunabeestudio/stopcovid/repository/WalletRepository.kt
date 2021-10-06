/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/22/03 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.repository

import android.content.Context
import android.net.Uri
import android.net.UrlQuerySanitizer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.lunabeestudio.domain.extension.walletPublicKey
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.framework.manager.DebugManager
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.datasource.RemoteCertificateDataSource
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.stopcovid.extension.isFrench
import com.lunabeestudio.stopcovid.extension.raw
import com.lunabeestudio.stopcovid.model.DccCertificates
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificateMalformedException
import com.lunabeestudio.stopcovid.model.WalletCertificateNoKeyException
import com.lunabeestudio.stopcovid.model.getForKeyId
import com.lunabeestudio.stopcovid.widgetshomescreen.DccWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class WalletRepository(
    context: Context,
    private val localKeystoreDataSource: LocalKeystoreDataSource,
    private val debugManager: DebugManager,
    private val remoteDataSource: RemoteCertificateDataSource,
    coroutineScope: CoroutineScope,
) {
    val walletCertificateFlow: StateFlow<List<WalletCertificate>>

    val certificateCountFlow: Flow<Int>
        get() = localKeystoreDataSource.certificateCountFlow

    private val _migrationInProgress: MutableLiveData<Boolean> = MutableLiveData(false)
    val migrationInProgress: LiveData<Boolean>
        get() = _migrationInProgress

    init {
        // SharingStarted.Eagerly to use the state flow as cache
        walletCertificateFlow = localKeystoreDataSource.rawWalletCertificatesFlow.map { rawWalletList ->
            rawWalletList.toWalletCertificates()
        }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())
        walletCertificateFlow.asLiveData(timeoutInMs = 0L).observeForever {
            DccWidget.updateWidget(context)
        }

        coroutineScope.launch {
            _migrationInProgress.postValue(true)
            debugManager.logCertificateMigrated(localKeystoreDataSource.migrateCertificates())
            _migrationInProgress.postValue(false)
        }
    }

    fun extractCertificateDataFromUrl(urlValue: String): Pair<String, WalletCertificateType.Format?> {
        val uri = Uri.parse(urlValue)
        var code = uri.fragment

        if (code == null) { // Try the old way
            val sanitizer = UrlQuerySanitizer()
            sanitizer.registerParameter("v") {
                it // Do nothing since there are plenty of non legal characters in this value
            }
            sanitizer.parseUrl(sanitizer.unescape(urlValue))
            code = sanitizer.getValue("v")
        }

        val certificateFormat = uri.lastPathSegment?.let { WalletCertificateType.Format.fromValue(it) }

        return (code ?: throw WalletCertificateMalformedException()) to certificateFormat
    }

    suspend fun verifyAndGetCertificateCodeValue(
        configuration: Configuration,
        codeValue: String,
        dccCertificates: DccCertificates,
        certificateFormat: WalletCertificateType.Format?,
    ): WalletCertificate {
        val walletCertificate = getCertificateFromValue(codeValue)

        if (walletCertificate == null ||
            (certificateFormat != null && walletCertificate.type.format != certificateFormat)
        ) {
            throw WalletCertificateMalformedException()
        }

        walletCertificate.parse()

        val key: String? = when (walletCertificate) {
            is EuropeanCertificate -> dccCertificates.getForKeyId(walletCertificate.keyCertificateId)
            is FrenchCertificate -> configuration.walletPublicKey(walletCertificate.keyAuthority, walletCertificate.keyCertificateId)
        }

        if (key != null) {
            walletCertificate.verifyKey(key)
        } else if ((walletCertificate as? EuropeanCertificate)?.greenCertificate?.isFrench == true
            || walletCertificate !is EuropeanCertificate
        ) {
            // Only check French certificates
            throw WalletCertificateNoKeyException()
        }

        return walletCertificate
    }

    suspend fun saveCertificate(localKeystoreDataSource: LocalKeystoreDataSource, walletCertificate: WalletCertificate) {
        localKeystoreDataSource.insertAllRawWalletCertificates(walletCertificate.raw)
    }

    suspend fun toggleFavorite(localKeystoreDataSource: LocalKeystoreDataSource, walletCertificate: EuropeanCertificate) {
        val walletCertificates = localKeystoreDataSource.rawWalletCertificates()
        var currentFavorite: RawWalletCertificate? = null
        val rawWalletCertificate: RawWalletCertificate? = walletCertificates.firstOrNull { it.id == walletCertificate.id }

        rawWalletCertificate?.let {
            if (!walletCertificate.isFavorite) {
                // Remove current favorite if there is one
                currentFavorite = walletCertificates.firstOrNull { it.isFavorite }
                currentFavorite?.let { currentFavorite ->
                    currentFavorite.isFavorite = false
                }
            }

            rawWalletCertificate.isFavorite = !rawWalletCertificate.isFavorite
        }

        localKeystoreDataSource.insertAllRawWalletCertificates(*listOfNotNull(currentFavorite, rawWalletCertificate).toTypedArray())
    }

    private suspend fun getCertificateFromValue(value: String): WalletCertificate? {
        return WalletCertificate.createCertificateFromValue(value)
    }

    suspend fun deleteCertificate(localKeystoreDataSource: LocalKeystoreDataSource, walletCertificate: WalletCertificate) {
        localKeystoreDataSource.deleteRawWalletCertificate(walletCertificate.id)
    }

    suspend fun deleteAllCertificates(localKeystoreDataSource: LocalKeystoreDataSource) {
        localKeystoreDataSource.deleteAllRawWalletCertificates()
    }

    suspend fun convertCertificate(
        robertManager: RobertManager,
        certificate: RawWalletCertificate,
        to: WalletCertificateType.Format
    ): RobertResultData<String> {

        val robertResultData = if (robertManager.configuration.conversionApiVersion == 2) {
            remoteDataSource.convertCertificateV2(
                robertManager = robertManager,
                encodedCertificate = certificate.value,
                from = certificate.type.format,
                to = to
            )
        } else {
            remoteDataSource.convertCertificateV1(
                encodedCertificate = certificate.value,
                from = certificate.type.format,
                to = to
            )
        }

        (robertResultData as? RobertResultData.Failure)?.error?.let { exception ->
            Timber.e(exception)
        }

        return robertResultData
    }

    suspend fun getCertificateCount(): Int {
        return localKeystoreDataSource.getCertificateCount()
    }

    fun certificateExists(certificate: WalletCertificate): Boolean {
        return walletCertificateFlow.value.any { certificate.value == it.value }
    }

    fun getById(certificateId: String): Flow<WalletCertificate?> =
        localKeystoreDataSource.getCertificateById(certificateId).map { it?.toWalletCertificate() }

    private suspend fun List<RawWalletCertificate>.toWalletCertificates() =
        mapNotNull { rawWallet ->
            rawWallet.toWalletCertificate()
        }

    private suspend fun RawWalletCertificate.toWalletCertificate() = try {
        WalletCertificate.createCertificateFromRaw(this)?.apply {
            parse()
        }
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}