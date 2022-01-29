/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.local.datasource

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.ErrorEventName
import com.lunabeestudio.domain.model.AtRiskStatus
import com.lunabeestudio.domain.model.Attestation
import com.lunabeestudio.domain.model.Calibration
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.FormEntry
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.framework.extension.setOrRemove
import com.lunabeestudio.framework.local.AppDatabase
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.framework.local.model.ActivityPassRoom
import com.lunabeestudio.framework.local.model.AttestationRoom
import com.lunabeestudio.framework.local.model.CertificateRoom
import com.lunabeestudio.framework.local.model.VenueRoom
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.reflect.Type
import java.util.UUID

class SecureKeystoreDataSource(
    context: Context,
    private val cryptoManager: LocalCryptoManager,
    private val cache: MutableMap<String, Any>?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LocalKeystoreDataSource {

    private val gson: Gson = Gson()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
    val db: AppDatabase = AppDatabase.build(context, DB_NAME)

    override var configuration: Configuration?
        get() = getValue(SHARED_PREF_KEY_CONFIGURATION, Configuration::class.java)
        set(value) = setValue(SHARED_PREF_KEY_CONFIGURATION, value)

    override var calibration: Calibration?
        get() = getValue(SHARED_PREF_KEY_CALIBRATION, Calibration::class.java)
        set(value) = setValue(SHARED_PREF_KEY_CALIBRATION, value)

    override var shouldReloadBleSettings: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_SHOULD_RELOAD_BLE_SETTINGS, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_SHOULD_RELOAD_BLE_SETTINGS, value)

    override var isRegistered: Boolean
        get() {
            val value: Boolean? = getEncryptedValue(SHARED_PREF_KEY_IS_REGISTERED, Boolean::class.java)
            return value ?: (kA != null && kEA != null).let {
                isRegistered = it
                it
            }
        }
        set(value) = setEncryptedValue(SHARED_PREF_KEY_IS_REGISTERED, value)

    override var kA: ByteArray?
        get() = getEncryptedValue(SHARED_PREF_KEY_KA, ByteArray::class.java, useCache = false)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_KA, value, useCache = false)

    override var kEA: ByteArray?
        get() = getEncryptedValue(SHARED_PREF_KEY_KEA, ByteArray::class.java, useCache = false)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_KEA, value, useCache = false)

    override var timeStart: Long?
        get() = getAndMigrateOldUnencryptedLong(SHARED_PREF_KEY_TIME_START, SHARED_PREF_KEY_TIME_START_ENCRYPTED)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_TIME_START_ENCRYPTED, value)

    override var atRiskLastRefresh: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_AT_RISK_LAST_REFRESH, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_AT_RISK_LAST_REFRESH, value)

    override var atRiskLastError: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_AT_RISK_LAST_ERROR, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_AT_RISK_LAST_ERROR, value)

    override var atRiskStatus: AtRiskStatus?
        get() = getEncryptedValue(SHARED_PREF_KEY_AT_RISK_STATUS, AtRiskStatus::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_AT_RISK_STATUS, value)

    override var currentRobertAtRiskStatus: AtRiskStatus?
        get() = getEncryptedValue(SHARED_PREF_KEY_CURRENT_ROBERT_AT_RISK_STATUS, AtRiskStatus::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_CURRENT_ROBERT_AT_RISK_STATUS, value)

    override var currentWarningAtRiskStatus: AtRiskStatus?
        get() = getEncryptedValue(SHARED_PREF_KEY_CURRENT_WARNING_AT_RISK_STATUS, AtRiskStatus::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_CURRENT_WARNING_AT_RISK_STATUS, value)

    override var cleaLastStatusIteration: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_CLEA_LAST_STATUS_ITERATION, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_CLEA_LAST_STATUS_ITERATION, value)

    override var atRiskModelVersion: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_AT_RISK_MODEL_VERSION, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_AT_RISK_MODEL_VERSION, value)

    override var deprecatedLastRiskReceivedDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_LAST_RISK_RECEIVED_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_LAST_RISK_RECEIVED_DATE, value)

    override var deprecatedLastExposureTimeframe: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME, Int::class.java, useCache = false)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME, value, useCache = false)

    override var proximityActive: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_PROXIMITY_ACTIVE, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_PROXIMITY_ACTIVE, value)

    override var saveAttestationData: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_SAVE_ATTESTATION_DATA, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_SAVE_ATTESTATION_DATA, value)

    override var savedAttestationData: Map<String, FormEntry>?
        get() = getEncryptedValue(SHARED_PREF_KEY_SAVED_ATTESTATION_DATA, object : TypeToken<Map<String, FormEntry>>() {}.type)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_SAVED_ATTESTATION_DATA, value)

    override val attestationsFlow: Flow<List<Attestation>>
        get() = db.attestationRoomDao().getAllFlow().map { attestationsRoom ->
            withContext(ioDispatcher) {
                attestationsRoom.mapNotNull { attestationRoom ->
                    kotlin.runCatching {
                        val decryptedString = cryptoManager.decryptToString(attestationRoom.encryptedValue)
                        gson.fromJson(decryptedString, Attestation::class.java)
                    }.getOrNull()
                }
            }
        }

    override suspend fun attestations(): TacResult<List<Attestation>> {
        migrateAttestations()

        var lastError: Exception? = null
        val attestations = withContext(ioDispatcher) {
            db.attestationRoomDao().getAll().mapNotNull { (_, encryptedValue) ->
                try {
                    val decryptedString = cryptoManager.decryptToString(encryptedValue)
                    gson.fromJson(decryptedString, Attestation::class.java)
                } catch (e: Exception) {
                    Timber.e(e)
                    lastError = e
                    null
                }
            }
        }

        return if (lastError == null) {
            TacResult.Success(attestations)
        } else {
            TacResult.Failure(lastError, attestations)
        }
    }

    override suspend fun insertAllAttestations(vararg attestations: Attestation) {
        withContext(ioDispatcher) {
            db.attestationRoomDao().insertAll(
                *attestations.map { attestation ->
                    val encryptedString = cryptoManager.encryptToString(gson.toJson(attestation))
                    AttestationRoom(attestation.id, encryptedString)
                }.toTypedArray()
            )
        }
    }

    override suspend fun deleteAttestation(attestationId: String) {
        withContext(ioDispatcher) {
            db.attestationRoomDao().delete(attestationId)
        }
    }

    override suspend fun deleteAllAttestations() {
        withContext(ioDispatcher) {
            db.attestationRoomDao().deleteAll()
        }
    }

    @Suppress("DEPRECATION")
    override fun deleteDeprecatedAttestations() {
        deprecatedAttestations = null
        deprecatedAttestations2 = null
    }

    @Suppress("DEPRECATION")
    // id migration (null due to reflection)
    private suspend fun migrateAttestations() {
        if (sharedPreferences.contains(SHARED_PREF_KEY_ATTESTATIONS_V2)) {
            deprecatedAttestations2?.map {
                it.copy(id = it.id ?: UUID.randomUUID().toString())
            }?.toTypedArray()?.let { deprecatedAttestations ->
                insertAllAttestations(*deprecatedAttestations)
                deprecatedAttestations2 = null
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated(message = "Old storage", replaceWith = ReplaceWith("attestations()"), level = DeprecationLevel.WARNING)
    private var deprecatedAttestations2: List<Attestation>?
        get() = getEncryptedValue(SHARED_PREF_KEY_ATTESTATIONS_V2, object : TypeToken<List<Attestation>>() {}.type)
        set(value) {
            setEncryptedValue(SHARED_PREF_KEY_ATTESTATIONS_V2, value)
        }

    override fun getCertificateByIdFlow(id: String): Flow<RawWalletCertificate?> {
        return db.certificateRoomDao().getByIdFlow(id).map { certificateRoom ->
            withContext(ioDispatcher) {
                certificateRoom?.let {
                    val decryptedString = cryptoManager.decryptToString(certificateRoom.encryptedValue)
                    gson.fromJson(decryptedString, RawWalletCertificate::class.java)
                }
            }
        }
    }

    override suspend fun getCertificateById(id: String): RawWalletCertificate? {
        return withContext(ioDispatcher) {
            db.certificateRoomDao().getById(id)?.let { certificateRoom ->
                val decryptedString = cryptoManager.decryptToString(certificateRoom.encryptedValue)
                gson.fromJson(decryptedString, RawWalletCertificate::class.java)
            }
        }
    }

    override val rawWalletCertificatesFlow: Flow<TacResult<List<RawWalletCertificate>>>
        get() = db.certificateRoomDao().getAllFlow().map { certificatesRoom ->
            withContext(ioDispatcher) {
                var lastError: Exception? = null
                val rawWalletCertificates = certificatesRoom.mapNotNull { (_, encryptedValue) ->
                    try {
                        val decryptedString = cryptoManager.decryptToString(encryptedValue)
                        gson.fromJson(decryptedString, RawWalletCertificate::class.java)
                    } catch (e: Exception) {
                        Timber.e(e)
                        lastError = e
                        null
                    }
                }

                if (lastError == null) {
                    TacResult.Success(rawWalletCertificates)
                } else {
                    TacResult.Failure(lastError, rawWalletCertificates)
                }
            }
        }

    override suspend fun getRawActivityPassForRootId(id: String, timestamp: Long): RawWalletCertificate? {
        return withContext(ioDispatcher) {
            db.activityPassRoomDao().getForRootIdAndTime(id, timestamp)?.let {
                activityPassRoomToRawWalletCertificate(it)
            }
        }
    }

    private fun activityPassRoomToRawWalletCertificate(activityPassRoom: ActivityPassRoom) = kotlin.runCatching {
        val decryptedString = cryptoManager.decryptToString(activityPassRoom.encryptedValue)
        gson.fromJson(decryptedString, RawWalletCertificate::class.java).apply {
            this.expireAt = activityPassRoom.expireAt
            this.rootWalletCertificateId = activityPassRoom.rootUid
        }
    }.getOrNull()

    override suspend fun insertAllRawWalletCertificates(vararg certificates: RawWalletCertificate) {
        withContext(ioDispatcher) {
            val splitCertificates = certificates.groupBy { it.type == WalletCertificateType.DCC_LIGHT }
            splitCertificates[true]?.mapNotNull { certificate ->
                val encryptedString = cryptoManager.encryptToString(gson.toJson(certificate))
                val expireAt = certificate.expireAt
                val rootWalletCertificateId = certificate.rootWalletCertificateId
                if (expireAt != null && rootWalletCertificateId != null) {
                    ActivityPassRoom(certificate.id, encryptedString, expireAt, rootWalletCertificateId)
                } else if (expireAt == null) {
                    Timber.e("expireAt must not be null for activity pass")
                    null
                } else {
                    Timber.e("rootWalletCertificateId must not be null for activity pass")
                    null
                }
            }?.let {
                db.activityPassRoomDao().insertAll(*it.toTypedArray())
            }

            splitCertificates[false]?.map { certificate ->
                val encryptedString = cryptoManager.encryptToString(gson.toJson(certificate))
                CertificateRoom(certificate.id, encryptedString)
            }?.let {
                db.certificateRoomDao().insertAll(*it.toTypedArray())
            }
        }
    }

    override suspend fun updateNonLightRawWalletCertificate(vararg certificates: RawWalletCertificate) {
        withContext(ioDispatcher) {
            val roomCertificates = certificates.filter { it.type != WalletCertificateType.DCC_LIGHT }.map { certificate ->
                val encryptedString = cryptoManager.encryptToString(gson.toJson(certificate))
                CertificateRoom(certificate.id, encryptedString)
            }

            db.certificateRoomDao().updateAll(*roomCertificates.toTypedArray())
        }
    }

    override suspend fun deleteRawWalletCertificate(certificateId: String) {
        withContext(ioDispatcher) {
            db.certificateRoomDao().delete(certificateId)
        }
    }

    override suspend fun deleteAllRawWalletCertificates() {
        withContext(ioDispatcher) {
            db.certificateRoomDao().deleteAll()
        }
    }

    override suspend fun deleteAllActivityPassForCertificate(certificateId: String) {
        withContext(ioDispatcher) {
            db.activityPassRoomDao().deleteAllForRootId(certificateId)
        }
    }

    override suspend fun deleteAllExpiredActivityPass(timestamp: Long) {
        withContext(ioDispatcher) {
            db.activityPassRoomDao().deleteExpired(timestamp)
        }
    }

    override suspend fun countValidActivityPassForCertificate(certificateId: String, timestamp: Long): Int {
        return withContext(ioDispatcher) {
            db.activityPassRoomDao().countForRootIdAndNotExpired(certificateId, timestamp)
        }
    }

    override suspend fun getAllActivityPassDistinctByRootId(): List<RawWalletCertificate> {
        return withContext(ioDispatcher) {
            db.activityPassRoomDao().getAllDistinctByRootId().mapNotNull(::activityPassRoomToRawWalletCertificate)
        }
    }

    override suspend fun getAllActivityPassForRootId(rootCertificateId: String): List<RawWalletCertificate> {
        return withContext(ioDispatcher) {
            db.activityPassRoomDao().getAllActivityPassForRootId(rootCertificateId).mapNotNull(::activityPassRoomToRawWalletCertificate)
        }
    }

    override suspend fun deleteActivityPass(vararg activityPassId: String) {
        withContext(ioDispatcher) {
            db.activityPassRoomDao().deleteActivityPass(*activityPassId)
        }
    }

    @Suppress("DEPRECATION")
    override fun deleteDeprecatedCertificates() {
        deprecatedRawWalletCertificates = null
    }

    @Suppress("DEPRECATION")
    @Deprecated(message = "Old storage", replaceWith = ReplaceWith("rawWalletCertificates()"), level = DeprecationLevel.WARNING)
    private var deprecatedRawWalletCertificates: List<RawWalletCertificate>?
        get() = getEncryptedValue(SHARED_PREF_KEY_WALLET_CERTIFICATES, object : TypeToken<List<RawWalletCertificate>>() {}.type)
        set(value) {
            setEncryptedValue(SHARED_PREF_KEY_WALLET_CERTIFICATES, value)
        }

    @Suppress("DEPRECATION")
    // id + isFavorite migration (null due to reflection)
    override suspend fun migrateCertificates(analyticsManager: AnalyticsManager): List<RawWalletCertificate> {
        var migratedCertificates = emptyList<RawWalletCertificate>()
        if (sharedPreferences.contains(SHARED_PREF_KEY_WALLET_CERTIFICATES)) {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_WALLET_CERTIFICATES, null) ?: ""
            try {
                val decryptedString = cryptoManager.decryptToString(encryptedText)
                try {
                    val decryptedDeprecatedRawWalletCertificates =
                        gson.fromJson<List<RawWalletCertificate>>(decryptedString, object : TypeToken<List<RawWalletCertificate>>() {}.type)
                    decryptedDeprecatedRawWalletCertificates?.map {
                        it.copy(id = it.id ?: UUID.randomUUID().toString(), isFavorite = it.isFavorite ?: false)
                    }?.toTypedArray()?.let { deprecatedCertificates ->
                        migratedCertificates = deprecatedCertificates.toList()
                        try {
                            insertAllRawWalletCertificates(*deprecatedCertificates)
                            deprecatedRawWalletCertificates = null
                        } catch (e: Exception) {
                            Timber.e(e)
                            analyticsManager.reportErrorEvent(ErrorEventName.ERR_WALLET_INSERT_DB)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    analyticsManager.reportErrorEvent(ErrorEventName.ERR_WALLET_MIG_CONVERT)
                }
            } catch (e: Exception) {
                Timber.e(e)
                analyticsManager.reportErrorEvent(ErrorEventName.ERR_WALLET_MIG_DECRYPT)
            }
        }
        return migratedCertificates
    }

    @Suppress("DEPRECATION")
    @Deprecated(message = "Old model", replaceWith = ReplaceWith("attestations"), level = DeprecationLevel.WARNING)
    override var deprecatedAttestations: List<Map<String, FormEntry>>?
        get() = getEncryptedValue(SHARED_PREF_KEY_ATTESTATIONS, object : TypeToken<List<Map<String, FormEntry>>>() {}.type)
        set(value) {
            setEncryptedValue(SHARED_PREF_KEY_ATTESTATIONS, value)
        }

    override var reportDate: Long?
        get() = getAndMigrateOldUnencryptedLong(SHARED_PREF_KEY_REPORT_DATE, SHARED_PREF_KEY_REPORT_DATE_ENCRYPTED)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_DATE_ENCRYPTED, value)

    override var reportValidationToken: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_VALIDATION_TOKEN, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_VALIDATION_TOKEN, value)

    override suspend fun venuesQrCode(): TacResult<List<VenueQrCode>> {
        var lastError: Exception? = null
        val rawWalletCertificates = withContext(ioDispatcher) {
            db.venueRoomDao().getAll().mapNotNull { (_, encryptedValue) ->
                try {
                    val decryptedString = cryptoManager.decryptToString(encryptedValue)
                    gson.fromJson(decryptedString, VenueQrCode::class.java)
                } catch (e: Exception) {
                    Timber.e(e)
                    lastError = e
                    null
                }
            }
        }

        return if (lastError == null) {
            TacResult.Success(rawWalletCertificates)
        } else {
            TacResult.Failure(lastError, rawWalletCertificates)
        }
    }

    override val venuesQrCodeFlow: Flow<List<VenueQrCode>>
        get() = db.venueRoomDao().getAllFlow().map { venuesRoom ->
            withContext(ioDispatcher) {
                venuesRoom.mapNotNull { venueRoom ->
                    kotlin.runCatching {
                        val decryptedString = cryptoManager.decryptToString(venueRoom.encryptedValue)
                        gson.fromJson(decryptedString, VenueQrCode::class.java)
                    }.getOrNull()
                }
            }
        }

    override suspend fun insertAllVenuesQrCode(vararg venuesQrCode: VenueQrCode) {
        withContext(ioDispatcher) {
            db.venueRoomDao().insertAll(
                *venuesQrCode.map { venue ->
                    val encryptedString = cryptoManager.encryptToString(gson.toJson(venue))
                    VenueRoom(venue.id, encryptedString)
                }.toTypedArray()
            )
        }
    }

    override suspend fun deleteVenueQrCode(venueQrCodeId: String) {
        withContext(ioDispatcher) {
            db.venueRoomDao().delete(venueQrCodeId)
        }
    }

    override suspend fun deleteAllVenuesQrCode() {
        withContext(ioDispatcher) {
            db.venueRoomDao().deleteAll()
        }
    }

    @Suppress("DEPRECATION")
    override fun deleteDeprecatedVenuesQrCode() {
        deprecatedVenuesQrCode = null
    }

    override suspend fun getCertificateCount(): Int {
        return withContext(ioDispatcher) {
            db.certificateRoomDao().getAllCount()
        }
    }

    override val certificateCountFlow: Flow<Int>
        get() = db.certificateRoomDao().getAllCountFlow()

    @Suppress("DEPRECATION")
    @Deprecated(message = "Old storage", replaceWith = ReplaceWith("venuesQrCode()"), level = DeprecationLevel.WARNING)
    private var deprecatedVenuesQrCode: List<VenueQrCode>?
        get() = getEncryptedValue(SHARED_PREF_KEY_VENUES_QR_CODE, object : TypeToken<List<VenueQrCode>>() {}.type)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_VENUES_QR_CODE, value)

    @Suppress("DEPRECATION")
    // id migration (null due to reflection)
    private suspend fun migrateVenuesQrCode() {
        if (sharedPreferences.contains(SHARED_PREF_KEY_VENUES_QR_CODE)) {
            deprecatedVenuesQrCode?.map {
                it.copy(id = it.id ?: UUID.randomUUID().toString())
            }?.toTypedArray()?.let { deprecatedVenues ->
                insertAllVenuesQrCode(*deprecatedVenues)
                deprecatedVenuesQrCode = null
            }
        }
    }

    override var reportToSendStartTime: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_TO_SEND_START_TIME, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_TO_SEND_START_TIME, value)

    override var reportToSendEndTime: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_TO_SEND_END_TIME, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_TO_SEND_END_TIME, value)

    override var reportPositiveTestDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_POSITIVE_TEST_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_POSITIVE_TEST_DATE, value)

    override var reportSymptomsStartDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_SYMPTOMS_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_SYMPTOMS_DATE, value)

    override var declarationToken: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_DECLARATION_TOKEN, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_DECLARATION_TOKEN, value)

    // Isolation vars
    override var isolationFormState: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_FORM_STATE, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_FORM_STATE, value)

    override var isolationLastContactDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_LAST_CONTACT_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_LAST_CONTACT_DATE, value)

    override var isolationIsKnownIndexAtHome: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_KNOWN_INDEX_AT_HOME, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_KNOWN_INDEX_AT_HOME, value)

    override var isolationKnowsIndexSymptomsEndDate: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_KNOWS_SYMPTOMS_END_DATE, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_KNOWS_SYMPTOMS_END_DATE, value)

    override var isolationIndexSymptomsEndDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_INDEX_SYMPTOMS_END_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_INDEX_SYMPTOMS_END_DATE, value)

    override var isolationLastFormValidationDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_LAST_FORM_VALIDATION_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_LAST_FORM_VALIDATION_DATE, value)

    override var isolationIsTestNegative: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_TEST_NEGATIVE, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_TEST_NEGATIVE, value)

    override var isolationPositiveTestingDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_POSITIVE_TESTING_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_POSITIVE_TESTING_DATE, value)

    override var isolationIsHavingSymptoms: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_HAVING_SYMPTOMS, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_HAVING_SYMPTOMS, value)

    override var isolationSymptomsStartDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_SYMPTOMS_START_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_SYMPTOMS_START_DATE, value)

    override var isolationIsStillHavingFever: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_STILL_HAVING_FEVER, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_STILL_HAVING_FEVER, value)

    override var isolationIsFeverReminderScheduled: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_FEVER_REMINDER_SCHEDULES, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_FEVER_REMINDER_SCHEDULES, value)

    // GENERIC METHODS
    private fun <T> getEncryptedValue(key: String, type: Type, useCache: Boolean = true): T? {
        @Suppress("UNCHECKED_CAST")
        val cachedValue = if (useCache) {
            cache?.get(key) as? T?
        } else {
            null
        }
        return if (cachedValue != null) {
            cachedValue
        } else {
            val encryptedText = sharedPreferences.getString(key, null)
            return if (encryptedText != null) {
                val result = kotlin.runCatching {
                    if (type == ByteArray::class.java) {
                        @Suppress("UNCHECKED_CAST")
                        cryptoManager.decrypt(encryptedText) as? T
                    } else {
                        val decryptedString = cryptoManager.decryptToString(encryptedText)
                        gson.fromJson<T>(decryptedString, type)
                    }
                }
                if (useCache && result.isSuccess) {
                    cache?.setOrRemove(key, result.getOrNull())
                }
                if (result.isFailure) {
                    Timber.e(result.exceptionOrNull(), "Fail to get encrypted $key")
                }
                result.getOrNull()
            } else {
                null
            }
        }
    }

    private fun setEncryptedValue(key: String, value: Any?, useCache: Boolean = true) {
        if (useCache) {
            val typedValue = when (value) {
                is List<*> -> value.toList()
                is Map<*, *> -> value.toMap()
                else -> value
            }
            cache?.setOrRemove(key, typedValue)
        }
        if (value != null) {
            sharedPreferences.edit {
                putString(
                    key,
                    if (value is ByteArray) {
                        cryptoManager.encryptToString(value)
                    } else {
                        cryptoManager.encryptToString(gson.toJson(value))
                    }
                )
            }
        } else {
            sharedPreferences.edit().remove(key).apply()
        }
    }

    private fun <T> getValue(@Suppress("SameParameterValue") key: String, type: Type): T? {
        @Suppress("UNCHECKED_CAST")
        return (cache?.get(key) as? T?) ?: run {
            val text = sharedPreferences.getString(key, null)
            return if (text != null) {
                val result = kotlin.runCatching {
                    gson.fromJson<T>(text, type)
                }
                if (result.isSuccess) {
                    cache?.setOrRemove(key, result.getOrNull())
                }
                result.getOrNull()
            } else {
                null
            }
        }
    }

    private fun setValue(@Suppress("SameParameterValue") key: String, value: Any?) {
        val typedValue = when (value) {
            is List<*> -> value.toList()
            is Map<*, *> -> value.toMap()
            else -> value
        }
        cache?.setOrRemove(key, typedValue)
        if (value != null) {
            sharedPreferences.edit()
                .putString(
                    key,
                    gson.toJson(value)
                )
                .apply()
        } else {
            sharedPreferences.edit().remove(key).apply()
        }
    }

    private fun getAndMigrateOldUnencryptedLong(oldKey: String, newKey: String): Long? {
        return if (sharedPreferences.contains(oldKey)) {
            val prevLong = sharedPreferences.getLong(oldKey, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
            setEncryptedValue(newKey, prevLong)
            sharedPreferences.edit { remove(oldKey) }
            prevLong
        } else {
            getEncryptedValue(newKey, Long::class.java)
        }
    }

    override suspend fun forceRefreshCertificatesFlow() {
        val dao = db.certificateRoomDao()
        withContext(ioDispatcher) {
            dao.updateFirstCertificateUid(uid = UUID.randomUUID().toString())
        }
    }

    override suspend fun deleteLostCertificates() {
        val certificateRoomDao = db.certificateRoomDao()
        withContext(ioDispatcher) {
            val certificates = certificateRoomDao.getAll()
            val certificatesToDelete = certificates.filter {
                kotlin.runCatching { cryptoManager.decryptToString(it.encryptedValue) }.isFailure
            }.toTypedArray()
            certificateRoomDao.delete(*certificatesToDelete)
        }
    }

    override suspend fun forceRefreshAttestations() {
        val dao = db.attestationRoomDao()
        withContext(ioDispatcher) {
            dao.updateFirstAttestationUid(uid = UUID.randomUUID().toString())
        }
    }

    override suspend fun deleteLostAttestations() {
        val attestationRoomDao = db.attestationRoomDao()
        withContext(ioDispatcher) {
            val attestations = attestationRoomDao.getAll()
            val attestationsToDelete = attestations.filter {
                kotlin.runCatching { cryptoManager.decryptToString(it.encryptedValue) }.isFailure
            }.toTypedArray()
            attestationRoomDao.delete(*attestationsToDelete)
        }
    }

    override suspend fun forceRefreshVenues() {
        val dao = db.venueRoomDao()
        withContext(ioDispatcher) {
            dao.updateFirstVenueUid(uid = UUID.randomUUID().toString())
        }
    }

    override suspend fun deleteLostVenues() {
        val venueRoomDao = db.venueRoomDao()
        withContext(ioDispatcher) {
            val venues = venueRoomDao.getAll()
            val venuesToDelete = venues.filter {
                kotlin.runCatching { cryptoManager.decryptToString(it.encryptedValue) }.isFailure
            }.toTypedArray()
            venueRoomDao.delete(*venuesToDelete)
        }
    }

    override fun resetKeyGeneratedFlag() {
        cryptoManager.resetKeyGeneratedFlag()
    }

    companion object {
        private const val DB_NAME = "robert_db"
        const val SHARED_PREF_NAME: String = "robert_prefs"
        private const val SHARED_PREF_KEY_CONFIGURATION = "shared.pref.configuration"
        private const val SHARED_PREF_KEY_CALIBRATION = "shared.pref.calibration"
        private const val SHARED_PREF_KEY_SHOULD_RELOAD_BLE_SETTINGS = "shared.pref.should_reload_ble_settings"
        private const val SHARED_PREF_KEY_IS_REGISTERED = "shared.pref.is_registered"
        private const val SHARED_PREF_KEY_KA = "shared.pref.ka"
        private const val SHARED_PREF_KEY_KEA = "shared.pref.kea"
        private const val SHARED_PREF_KEY_TIME_START = "shared.pref.time_start"
        private const val SHARED_PREF_KEY_TIME_START_ENCRYPTED = "shared.pref.time_start_encrypted"
        private const val SHARED_PREF_KEY_AT_RISK_LAST_REFRESH = "shared.pref.at_risk_last_refresh"
        private const val SHARED_PREF_KEY_AT_RISK_LAST_ERROR = "shared.pref.at_risk_last_error"
        private const val SHARED_PREF_KEY_AT_RISK_STATUS = "shared.pref.at_risk_status"
        private const val SHARED_PREF_KEY_CURRENT_ROBERT_AT_RISK_STATUS = "shared.pref.current_robert_at_risk_status"
        private const val SHARED_PREF_KEY_CURRENT_WARNING_AT_RISK_STATUS = "shared.pref.current_warning_at_risk_status"
        private const val SHARED_PREF_KEY_AT_RISK_MODEL_VERSION = "shared.pref.at_risk_model_version"
        private const val SHARED_PREF_KEY_LAST_RISK_RECEIVED_DATE = "shared.pref.last_risk_received_date"
        private const val SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME = "shared.pref.last_exposure_timeframe"
        private const val SHARED_PREF_KEY_PROXIMITY_ACTIVE = "shared.pref.proximity_active"
        private const val SHARED_PREF_KEY_SAVE_ATTESTATION_DATA = "shared.pref.save_attestation_data"
        private const val SHARED_PREF_KEY_SAVED_ATTESTATION_DATA = "shared.pref.saved_attestation_data"

        @Deprecated(message = "Use attestations in Room instead")
        internal const val SHARED_PREF_KEY_ATTESTATIONS_V2 = "shared.pref.attestations_v2"

        @Deprecated(message = "Use attestations in Room instead")
        private const val SHARED_PREF_KEY_ATTESTATIONS = "shared.pref.attestations"
        private const val SHARED_PREF_KEY_REPORT_DATE = "shared.pref.report_date"
        private const val SHARED_PREF_KEY_REPORT_DATE_ENCRYPTED = "shared.pref.report_date_encrypted"

        @Deprecated(message = "Use venues in Room instead")
        internal const val SHARED_PREF_KEY_VENUES_QR_CODE = "shared.pref.venues_qr_code"
        private const val SHARED_PREF_KEY_REPORT_VALIDATION_TOKEN = "shared.pref.report_validation_token"
        private const val SHARED_PREF_KEY_REPORT_TO_SEND_START_TIME = "shared.pref.report_to_send_start_time"
        private const val SHARED_PREF_KEY_REPORT_TO_SEND_END_TIME = "shared.pref.report_to_send_end_time"
        private const val SHARED_PREF_KEY_DECLARATION_TOKEN = "shared.pref.declaration_token"

        @Deprecated(message = "Use wallets in Room instead")
        internal const val SHARED_PREF_KEY_WALLET_CERTIFICATES = "shared.pref.wallet_certificates"

        // Clea
        private const val SHARED_PREF_KEY_CLEA_LAST_STATUS_ITERATION = "shared.pref.clea_last_status_iteration"

        // Add on to ROBERT for isolation
        private const val SHARED_PREF_KEY_REPORT_SYMPTOMS_DATE = "shared.pref.reportSymptomsDate"
        private const val SHARED_PREF_KEY_REPORT_POSITIVE_TEST_DATE = "shared.pref.reportPositiveTestDate"

        // Isolation keys
        private const val SHARED_PREF_KEY_ISOLATION_FORM_STATE = "shared.pref.isolationFormState"
        private const val SHARED_PREF_KEY_ISOLATION_LAST_CONTACT_DATE = "shared.pref.isolationLastContactDate"
        private const val SHARED_PREF_KEY_ISOLATION_IS_KNOWN_INDEX_AT_HOME = "shared.pref.isolationIsKnownIndexAtHome"
        private const val SHARED_PREF_KEY_ISOLATION_KNOWS_SYMPTOMS_END_DATE = "shared.pref.isolationKnowsIndexSymptomsEndDate"
        private const val SHARED_PREF_KEY_ISOLATION_INDEX_SYMPTOMS_END_DATE = "shared.pref.isolationIndexSymptomsEndDate"
        private const val SHARED_PREF_KEY_ISOLATION_LAST_FORM_VALIDATION_DATE = "shared.pref.isolationLastFormValidationDate"
        private const val SHARED_PREF_KEY_ISOLATION_IS_TEST_NEGATIVE = "shared.pref.isolationIsTestNegative"
        private const val SHARED_PREF_KEY_ISOLATION_POSITIVE_TESTING_DATE = "shared.pref.isolationPositiveTestingDate"
        private const val SHARED_PREF_KEY_ISOLATION_IS_HAVING_SYMPTOMS = "shared.pref.isolationIsHavingSymptoms"
        private const val SHARED_PREF_KEY_ISOLATION_SYMPTOMS_START_DATE = "shared.pref.isolationSymptomsStartDate"
        private const val SHARED_PREF_KEY_ISOLATION_IS_STILL_HAVING_FEVER = "shared.pref.isolationIsStillHavingFever"
        private const val SHARED_PREF_KEY_ISOLATION_IS_FEVER_REMINDER_SCHEDULES = "shared.pref.isolationIsFeverReminderScheduled"
    }
}
