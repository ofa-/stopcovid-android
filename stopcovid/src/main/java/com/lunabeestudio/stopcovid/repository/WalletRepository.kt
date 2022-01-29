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
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.analytics.model.ErrorEventName
import com.lunabeestudio.domain.model.DccLightData
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.framework.manager.DebugManager
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.datasource.RemoteConversionDataSource
import com.lunabeestudio.robert.datasource.RemoteDccLightDataSource
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.model.UnknownException
import com.lunabeestudio.stopcovid.extension.raw
import com.lunabeestudio.stopcovid.extension.smartWalletProfileId
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificateMalformedException
import com.lunabeestudio.stopcovid.widgetshomescreen.DccWidget
import com.lunabeestudio.stopcovid.worker.SmartWalletNotificationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class WalletRepository(
    context: Context,
    private val localKeystoreDataSource: LocalKeystoreDataSource,
    private val debugManager: DebugManager,
    private val remoteConversionDataSource: RemoteConversionDataSource,
    private val analyticsManager: AnalyticsManager,
    coroutineScope: CoroutineScope,
    private val remoteDccLightDataSource: RemoteDccLightDataSource,
    private val robertManager: RobertManager,
) {
    val walletCertificateFlow: StateFlow<TacResult<List<WalletCertificate>>>

    val certificateCountFlow: Flow<Int>
        get() = localKeystoreDataSource.certificateCountFlow

    private val _migrationInProgress: MutableLiveData<Boolean> = MutableLiveData(false)
    val migrationInProgress: LiveData<Boolean>
        get() = _migrationInProgress

    init {
        // SharingStarted.Eagerly to use the state flow as cache
        walletCertificateFlow = localKeystoreDataSource.rawWalletCertificatesFlow.map { rawWalletCertificatesResult ->
            rawWalletCertificatesResult.toWalletCertificatesResult()
        }.stateIn(coroutineScope, SharingStarted.Eagerly, TacResult.Loading())

        coroutineScope.launch(Dispatchers.Main) {
            walletCertificateFlow.collect {
                DccWidget.updateWidget(context)
            }
        }

        if (debugManager.oldCertificateInSharedPrefs()) {
            coroutineScope.launch {
                _migrationInProgress.postValue(true)
                debugManager.logCertificateMigrated(localKeystoreDataSource.migrateCertificates(analyticsManager))
                if (debugManager.oldCertificateInSharedPrefs()) {
                    analyticsManager.reportErrorEvent(ErrorEventName.ERR_WALLET_MIG)
                } else {
                    analyticsManager.reportAppEvent(AppEventName.e22)
                }
                _migrationInProgress.postValue(false)
            }
        }

        SmartWalletNotificationWorker.start(context)
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

    suspend fun saveCertificate(walletCertificate: WalletCertificate) {
        try {
            localKeystoreDataSource.insertAllRawWalletCertificates(walletCertificate.raw)
        } catch (e: Exception) {
            analyticsManager.reportErrorEvent(ErrorEventName.ERR_WALLET_INSERT_DB)
            throw e
        }
    }

    suspend fun toggleFavorite(walletCertificate: EuropeanCertificate) {
        val walletCertificates = localKeystoreDataSource.rawWalletCertificatesFlow.firstOrNull()?.data.orEmpty()
        var currentRawFavorite: RawWalletCertificate? = null
        val newRawFavorite: RawWalletCertificate? = walletCertificates.firstOrNull { it.id == walletCertificate.id }

        newRawFavorite?.let {
            if (!walletCertificate.isFavorite) {
                // Remove current favorite if there is one
                currentRawFavorite = walletCertificates.firstOrNull { it.isFavorite }
                currentRawFavorite?.let { currentFavorite ->
                    currentFavorite.isFavorite = false
                }
            }

            newRawFavorite.isFavorite = !newRawFavorite.isFavorite
        }

        localKeystoreDataSource.updateNonLightRawWalletCertificate(*listOfNotNull(currentRawFavorite, newRawFavorite).toTypedArray())
    }

    suspend fun deleteCertificate(walletCertificate: WalletCertificate) {
        localKeystoreDataSource.deleteRawWalletCertificate(walletCertificate.id)
    }

    suspend fun deleteAllCertificates() {
        localKeystoreDataSource.deleteAllRawWalletCertificates()
    }

    fun deleteDeprecatedCertificated() {
        localKeystoreDataSource.deleteDeprecatedCertificates()
    }

    suspend fun deleteAllActivityPassForCertificate(certificateId: String) {
        localKeystoreDataSource.deleteAllActivityPassForCertificate(certificateId)
    }

    suspend fun convertCertificate(
        robertManager: RobertManager,
        certificate: RawWalletCertificate,
        to: WalletCertificateType.Format
    ): RobertResultData<String> {

        val robertResultData = if (robertManager.configuration.conversionApiVersion == 2) {
            val serverKeyConfig = robertManager.configuration.conversionPublicKey.toList().firstOrNull()
                ?: return RobertResultData.Failure(UnknownException("No server conversion key found for conversion v2"))

            remoteConversionDataSource.convertCertificateV2(
                serverKeyConfig = serverKeyConfig,
                encodedCertificate = certificate.value,
                from = certificate.type.format,
                to = to
            )
        } else {
            remoteConversionDataSource.convertCertificateV1(
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
        return walletCertificateFlow.value.data?.any { certificate.value == it.value } == true
    }

    fun getCertificateByIdFlow(certificateId: String): Flow<WalletCertificate?> =
        localKeystoreDataSource.getCertificateByIdFlow(certificateId).map { it?.toWalletCertificate() }

    suspend fun getCertificateById(certificateId: String): WalletCertificate? =
        localKeystoreDataSource.getCertificateById(certificateId)?.toWalletCertificate()

    private suspend fun TacResult<List<RawWalletCertificate>>.toWalletCertificatesResult(): TacResult<List<WalletCertificate>> {
        val walletCertificates = data?.mapNotNull { rawWallet ->
            rawWallet.toWalletCertificate()
        }.orEmpty()
        return if (this is TacResult.Success) {
            TacResult.Success(walletCertificates)
        } else {
            TacResult.Failure((this as? TacResult.Failure)?.throwable, walletCertificates)
        }
    }

    private suspend fun RawWalletCertificate.toWalletCertificate() = try {
        WalletCertificate.createCertificateFromRaw(this)?.apply {
            parse()
        }
    } catch (e: Exception) {
        Timber.e(e)
        null
    }

    suspend fun getActivityPass(rootCertificateId: String, timestamp: Long): EuropeanCertificate? {
        return localKeystoreDataSource.getRawActivityPassForRootId(rootCertificateId, timestamp)
            ?.toWalletCertificate() as? EuropeanCertificate
    }

    suspend fun deleteAllExpiredActivityPass(timestamp: Long): Unit =
        localKeystoreDataSource.deleteAllExpiredActivityPass(timestamp)

    suspend fun countValidActivityPassForCertificate(certificateId: String, timestamp: Long): Int =
        localKeystoreDataSource.countValidActivityPassForCertificate(certificateId, timestamp)

    suspend fun getAllActivityPassDistinctByRootId(): List<EuropeanCertificate> =
        localKeystoreDataSource.getAllActivityPassDistinctByRootId().mapNotNull { rawWalletCertificate ->
            EuropeanCertificate.getCertificate(
                rawWalletCertificate.value,
                rawWalletCertificate.id,
                rawWalletCertificate.isFavorite,
                rawWalletCertificate.canRenewDccLight,
            )
        }

    suspend fun getAllActivityPassForRootId(rootCertificateId: String): List<EuropeanCertificate> =
        localKeystoreDataSource.getAllActivityPassForRootId(rootCertificateId).mapNotNull { rawWalletCertificate ->
            EuropeanCertificate.getCertificate(
                rawWalletCertificate.value,
                rawWalletCertificate.id,
                rawWalletCertificate.isFavorite,
                rawWalletCertificate.canRenewDccLight,
            )
        }

    suspend fun deleteActivityPass(vararg activityPassId: String) {
        localKeystoreDataSource.deleteActivityPass(*activityPassId)
    }

    suspend fun saveCertificate(vararg arrayOfRawWalletCertificates: RawWalletCertificate) {
        localKeystoreDataSource.insertAllRawWalletCertificates(*arrayOfRawWalletCertificates)
    }

    suspend fun updateCertificate(certificate: RawWalletCertificate) {
        localKeystoreDataSource.updateNonLightRawWalletCertificate(certificate)
    }

    suspend fun generateActivityPass(certificateValue: String): RobertResultData<DccLightData> {
        return remoteDccLightDataSource.generateActivityPass(
            robertManager.configuration.generationServerPublicKey,
            certificateValue,
        )
    }

    suspend fun generateMultipass(certificateValueList: List<String>): RobertResultData<String> {
        return remoteDccLightDataSource.generateMultipass(
            robertManager.configuration.generationServerPublicKey,
            certificateValueList,
        )
    }

    suspend fun forceRefreshCertificatesFlow() {
        localKeystoreDataSource.forceRefreshCertificatesFlow()
    }

    suspend fun deleteLostCertificates() {
        localKeystoreDataSource.deleteLostCertificates()
    }

    fun resetKeyCryptoGeneratedFlag() {
        localKeystoreDataSource.resetKeyGeneratedFlag()
    }

    fun findCertificateByProfileId(profileId: String): List<EuropeanCertificate> {
        return walletCertificateFlow.value.data
            ?.filterIsInstance<EuropeanCertificate>()
            ?.filter { dcc ->
                dcc.smartWalletProfileId() == profileId
            }.orEmpty()
    }
}
