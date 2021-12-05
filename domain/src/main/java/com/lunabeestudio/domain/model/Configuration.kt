/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.domain.model

class Configuration(
    var version: Int,
    val versionCalibrationBle: Int,
    val apiVersion: String,
    val cleaReportApiVersion: String,
    val cleaStatusApiVersion: String,
    var displayAttestation: Boolean,
    var displayVaccination: Boolean,
    val dataRetentionPeriod: Int,
    val quarantinePeriod: Int,
    val checkStatusFrequencyHour: Float,
    val minStatusRetryDuration: Float,
    val randomStatusHour: Float,
    val preSymptomsSpan: Int,
    val minHourContactNotif: Int,
    val maxHourContactNotif: Int,
    var displayDepartmentLevel: Boolean,
    val dontUseScannerHardwareBatching: List<String>?,
    val filterConfig: String,
    val filterMode: String,
    val serviceUUID: String,
    val characteristicUUID: String,
    val backgroundServiceManufacturerData: String,
    val qrCodeDeletionHours: Float,
    val qrCodeExpiredHours: Float,
    val qrCodeFormattedString: String,
    val qrCodeFormattedStringDisplayed: String,
    val qrCodeFooterString: String,
    val venuesTimestampRoundingInterval: Int,
    val proximityReactivationReminderHours: List<Int>,
    val venuesRetentionPeriod: Int,
    val privateEventVenueType: String,
    var displayRecordVenues: Boolean,
    var displayPrivateEvent: Boolean,
    var displayIsolation: Boolean,
    var isolationMinRiskLevel: Float,
    val positiveSampleSpan: Int,
    val isolationDuration: Long,
    val postIsolationDuration: Long,
    val covidIsolationDuration: Long,
    val venuesSalt: Int,
    val allowNoAdvertisingDevice: Boolean,
    val unsupportedDevices: List<String>?,
    val vaccinationCentersCount: Int,
    val scanReportDelay: Int,
    val contagiousSpan: Int,
    val ameliUrl: String?,
    var displaySanitaryCertificatesWallet: Boolean,
    var walletOldCertificateThresholdInDays: Map<String, Float>,
    var walletPublicKeys: List<WalletPublicKey>,
    var isAnalyticsOn: Boolean,
    var analyticsApiVersion: String,
    var testCertificateValidityThresholds: List<Int>,
    val cleaUrls: List<String>,
    val covidPlusWarning: Int,
    val covidPlusNoTracing: Int,
    var displayCertificateConversion: Boolean,
    var daysAfterCompletion: Map<String, Int>,
    var certificateConversionSidepOnlyCode: List<String>,
    var conversionPublicKey: Map<String, String>,
    var conversionApiVersion: Int,
    var maxCertBeforeWarning: Int,
    var noWaitDoses: Map<String, Int>,
    var ratingsKeyFiguresOpeningThreshold: Int,
    var displayUrgentDgs: Boolean,
    var notification: Notification?,
    val generationServerPublicKey: String,
    val activityPassSkipNegTestHours: Int,
    val displayActivityPass: Boolean,
    val renewThreshold: Int,
    val smartWalletAges: SmartWalletAges?,
    val smartWalletExp: SmartWalletExp?,
    val smartWalletElg: SmartWalletElg?,
    val smartWalletVacc: SmartWalletVacc?,
    var smartWalletNotif: Boolean,
    var isSmartWalletOn: Boolean,
    var colorsCompareKeyFigures: ColorsCompareKeyFigures?,
    var keyFiguresCombination: List<KeyFigureCombination>?,
) {
    class Notification(val title: String, val subtitle: String, val url: String, val version: Int)
    class ColorsCompareKeyFigures(val colorKeyFigure1: ColorCompareKeyFigures, val colorKeyFigure2: ColorCompareKeyFigures)
    class ColorCompareKeyFigures(val darkColor: String?, val lightColor: String?)
    class KeyFigureCombination(val title: String?, val keyFigureLabel1: String?, val keyFigureLabel2: String?)
}
