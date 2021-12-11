/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.datasource

import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.domain.model.AtRiskStatus
import com.lunabeestudio.domain.model.Attestation
import com.lunabeestudio.domain.model.Calibration
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.FormEntry
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.domain.model.VenueQrCode
import kotlinx.coroutines.flow.Flow

interface LocalKeystoreDataSource {
    var configuration: Configuration?
    var calibration: Calibration?
    var shouldReloadBleSettings: Boolean?
    var isRegistered: Boolean
    var kA: ByteArray?
    var kEA: ByteArray?
    var timeStart: Long?
    var atRiskLastRefresh: Long?
    var atRiskLastError: Long?
    var proximityActive: Boolean?
    var atRiskStatus: AtRiskStatus?
    var currentRobertAtRiskStatus: AtRiskStatus?
    var currentWarningAtRiskStatus: AtRiskStatus?
    var cleaLastStatusIteration: Int?
    var atRiskModelVersion: Int?
    var deprecatedLastRiskReceivedDate: Long?
    var deprecatedLastExposureTimeframe: Int?

    suspend fun attestations(): TacResult<List<Attestation>>
    val attestationsFlow: Flow<List<Attestation>>
    suspend fun insertAllAttestations(vararg attestations: Attestation)
    suspend fun deleteAttestation(attestationId: String)
    suspend fun deleteAllAttestations()
    fun deleteDeprecatedAttestations()

    val rawWalletCertificatesFlow: Flow<TacResult<List<RawWalletCertificate>>>
    suspend fun insertAllRawWalletCertificates(vararg certificates: RawWalletCertificate)
    suspend fun updateNonLightRawWalletCertificate(vararg certificates: RawWalletCertificate)
    suspend fun deleteRawWalletCertificate(certificateId: String)
    suspend fun deleteAllRawWalletCertificates()
    fun deleteDeprecatedCertificates()
    suspend fun deleteAllActivityPassForCertificate(certificateId: String)
    suspend fun deleteAllExpiredActivityPass(timestamp: Long)
    suspend fun countValidActivityPassForCertificate(certificateId: String, timestamp: Long): Int
    suspend fun migrateCertificates(analyticsManager: AnalyticsManager): List<RawWalletCertificate>

    var deprecatedAttestations: List<Map<String, FormEntry>>?
    var savedAttestationData: Map<String, FormEntry>?
    var saveAttestationData: Boolean?
    var reportDate: Long?
    var reportValidationToken: String?
    var reportToSendStartTime: Long?
    var reportToSendEndTime: Long?

    suspend fun venuesQrCode(): TacResult<List<VenueQrCode>>
    val venuesQrCodeFlow: Flow<List<VenueQrCode>>
    suspend fun insertAllVenuesQrCode(vararg venuesQrCode: VenueQrCode)
    suspend fun deleteVenueQrCode(venueQrCodeId: String)
    suspend fun deleteAllVenuesQrCode()
    fun deleteDeprecatedVenuesQrCode()
    suspend fun getCertificateCount(): Int
    val certificateCountFlow: Flow<Int>
    fun getCertificateByIdFlow(id: String): Flow<RawWalletCertificate?>
    suspend fun getCertificateById(id: String): RawWalletCertificate?
    suspend fun getRawActivityPassForRootId(id: String, timestamp: Long): RawWalletCertificate?
    suspend fun getAllActivityPassDistinctByRootId(): List<RawWalletCertificate>
    suspend fun getAllActivityPassForRootId(rootCertificateId: String): List<RawWalletCertificate>
    suspend fun deleteActivityPass(vararg activityPassId: String)

    var declarationToken: String?
    var reportPositiveTestDate: Long?
    var reportSymptomsStartDate: Long?

    // Isolation vars
    var isolationLastContactDate: Long?
    var isolationIsKnownIndexAtHome: Boolean?
    var isolationKnowsIndexSymptomsEndDate: Boolean?
    var isolationIndexSymptomsEndDate: Long?
    var isolationLastFormValidationDate: Long?
    var isolationIsTestNegative: Boolean?
    var isolationPositiveTestingDate: Long?
    var isolationIsHavingSymptoms: Boolean?
    var isolationSymptomsStartDate: Long?
    var isolationIsStillHavingFever: Boolean?
    var isolationIsFeverReminderScheduled: Boolean?
    var isolationFormState: Int?

    suspend fun forceRefreshCertificatesFlow()
    suspend fun deleteLostCertificates()
    suspend fun forceRefreshAttestations()
    suspend fun deleteLostAttestations()
    suspend fun forceRefreshVenues()
    suspend fun deleteLostVenues()

    fun resetKeyGeneratedFlag()
}
