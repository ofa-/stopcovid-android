/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/17/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.SmartWalletAges
import com.lunabeestudio.domain.model.SmartWalletElg
import com.lunabeestudio.domain.model.SmartWalletExp
import com.lunabeestudio.domain.model.SmartWalletVacc
import com.lunabeestudio.domain.model.WalletPublicKey

internal class ApiConfiguration(
    @SerializedName("version")
    val version: Int,
    @SerializedName("versionCalibrationBle")
    val versionCalibrationBle: Int,
    @SerializedName("app.apiVersion")
    val apiVersion: String,
    @SerializedName("app.clea.statusApiVersion")
    val cleaStatusApiVersion: String,
    @SerializedName("app.clea.reportApiVersion")
    val cleaReportApiVersion: String,
    @SerializedName("app.displayAttestation")
    val displayAttestation: Boolean,
    @SerializedName("app.displayVaccination")
    val displayVaccination: Boolean,
    @SerializedName("app.dataRetentionPeriod")
    val dataRetentionPeriod: Int,
    @SerializedName("app.quarantinePeriod")
    val quarantinePeriod: Int,
    @SerializedName("app.checkStatusFrequency")
    val checkStatusFrequencyHour: Float,
    @SerializedName("app.minStatusRetryDuration")
    val minStatusRetryDuration: Float,
    @SerializedName("app.randomStatusHour")
    val randomStatusHour: Float,
    @SerializedName("app.preSymptomsSpan")
    val preSymptomsSpan: Int,
    @SerializedName("app.minHourContactNotif")
    val minHourContactNotif: Int,
    @SerializedName("app.maxHourContactNotif")
    val maxHourContactNotif: Int,
    @SerializedName("app.keyfigures.displayDepartmentLevel")
    val displayDepartmentLevel: Boolean,
    @SerializedName("ble.dontUseScannerHardwareBatching")
    val dontUseScannerHardwareBatching: String?,
    @SerializedName("ble.filterConfig")
    val filterConfig: String,
    @SerializedName("ble.filterMode")
    val filterMode: String,
    @SerializedName("ble.serviceUUID")
    val serviceUUID: String,
    @SerializedName("ble.characteristicUUID")
    val characteristicUUID: String,
    @SerializedName("ble.backgroundServiceManufacturerData")
    val backgroundServiceManufacturerData: String,
    @SerializedName("app.qrCode.deletionHours")
    val qrCodeDeletionHours: Float,
    @SerializedName("app.qrCode.expiredHours")
    val qrCodeExpiredHours: Float,
    @SerializedName("app.qrCode.formattedString")
    val qrCodeFormattedString: String,
    @SerializedName("app.qrCode.formattedStringDisplayed")
    val qrCodeFormattedStringDisplayed: String,
    @SerializedName("app.qrCode.footerString")
    val qrCodeFooterString: String,
    @SerializedName("app.venuesTimestampRoundingInterval")
    val venuesTimestampRoundingInterval: Int,
    @SerializedName("app.proximityReactivation.reminderHours")
    val proximityReactivationReminderHours: String,
    @SerializedName("app.venuesRetentionPeriod")
    val venuesRetentionPeriod: Int,
    @SerializedName("app.privateEventVenueType")
    val privateEventVenueType: String,
    @SerializedName("app.displayRecordVenues")
    val displayRecordVenues: Boolean,
    @SerializedName("app.displayPrivateEvent")
    val displayPrivateEvent: Boolean,
    @SerializedName("app.displayIsolation")
    val displayIsolation: Boolean,
    @SerializedName("app.isolationMinRiskLevel")
    val isolationMinRiskLevel: Float,
    @SerializedName("app.positiveSampleSpan")
    val positiveSampleSpan: Int,
    @SerializedName("app.isolation.duration")
    val isolationDuration: Long,
    @SerializedName("app.postIsolation.duration")
    val postIsolationDuration: Long,
    @SerializedName("app.isolation.durationCovid")
    val covidIsolationDuration: Long,
    @SerializedName("app.venuesSalt")
    val venuesSalt: Int,
    @SerializedName("app.allowNoAdvertisingDevice")
    val allowNoAdvertisingDevice: Boolean,
    @SerializedName("app.unsupportedDevices")
    val unsupportedDevices: String?,
    @SerializedName("app.vaccinationCentersCount")
    val vaccinationCentersCount: Int,
    @SerializedName("ble.scanReportDelay")
    val scanReportDelay: Int,
    @SerializedName("app.contagiousSpan")
    val contagiousSpan: Int,
    @SerializedName("app.ameliUrl")
    val ameliUrl: String?,
    @SerializedName("app.displaySanitaryCertificatesWallet")
    val displaySanitaryCertificatesWallet: Boolean,
    @SerializedName("app.wallet.oldCertificateThresholdInDays")
    val walletOldCertificateThresholdInDays: String,
    @SerializedName("app.walletPubKeys")
    val walletPublicKeys: String,
    @SerializedName("app.displaySanitaryCertificatesValidation")
    val displaySanitaryCertificatesValidation: Boolean,
    @SerializedName("app.isAnalyticsOn")
    val isAnalyticsOn: Boolean,
    @SerializedName("app.analyticsApiVersion")
    val analyticsApiVersion: String,
    @SerializedName("app.wallet.testCertificateValidityThresholds")
    val testCertificateValidityThresholds: String,
    @SerializedName("app.cleaUrls")
    val cleaUrls: String?,
    @SerializedName("app.covidPlusWarning")
    val covidPlusWarning: Int,
    @SerializedName("app.covidPlusNoTracing")
    val covidPlusNoTracing: Int,
    @SerializedName("app.displayCertificateConversion")
    val displayCertificateConversion: Boolean,
    @SerializedName("app.wallet.vaccin.daysAfterCompletion")
    val daysAfterCompletion: String,
    @SerializedName("app.wallet.certificateConversionSidepOnlyCode")
    val certificateConversionSidepOnlyCode: String,
    @SerializedName("app.wallet.conversionPublicKey")
    val conversionPublicKey: String,
    @SerializedName("app.wallet.conversionApiVersion")
    val conversionApiVersion: Int,
    @SerializedName("app.wallet.maxCertBeforeWarning")
    val maxCertBeforeWarning: Int,
    @SerializedName("app.wallet.vaccin.noWaitDoses")
    val noWaitDoses: String,
    @SerializedName("app.ratingsKeyFiguresOpeningThreshold")
    val ratingsKeyFiguresOpeningThreshold: Int,
    @SerializedName("app.displayUrgentDgs")
    val displayUrgentDgs: Boolean,
    @SerializedName("app.notif")
    val notif: String,
    @SerializedName("app.activityPass.generationServerPublicKey")
    val generationServerPublicKey: String,
    @SerializedName("app.activityPass.skipNegTestHours")
    val activityPassSkipNegTestHours: Int,
    @SerializedName("app.displayActivityPass")
    val displayActivityPass: Boolean,
    @SerializedName("app.activityPass.renewThreshold")
    val renewThreshold: Int,
    @SerializedName("app.smartwallet.ages")
    val smartWalletAges: String,
    @SerializedName("app.smartwallet.exp")
    val smartWalletExp: String,
    @SerializedName("app.smartwallet.elg")
    val smartWalletElg: String,
    @SerializedName("app.smartwallet.vacc")
    val smartWalletVacc: String,
    @SerializedName("app.smartwallet.notif")
    val smartWalletNotif: Boolean,
    @SerializedName("app.isSmartwalletOn")
    val isSmartWalletOn: Boolean,
    @SerializedName("app.keyfigures.compareColors")
    val compareColors: String,
    @SerializedName("app.keyfigures.compare")
    val keyFiguresCombination: String,
    @SerializedName("app.wallet.vaccin.noWaitDosesPivotDate")
    val noWaitDosesPivotDate: String,
)

internal fun ApiConfiguration.toDomain(gson: Gson) = Configuration(
    version = version,
    versionCalibrationBle = versionCalibrationBle,
    apiVersion = apiVersion,
    cleaReportApiVersion = cleaReportApiVersion,
    cleaStatusApiVersion = cleaStatusApiVersion,
    displayAttestation = displayAttestation,
    displayVaccination = displayVaccination,
    dataRetentionPeriod = dataRetentionPeriod,
    quarantinePeriod = quarantinePeriod,
    checkStatusFrequencyHour = checkStatusFrequencyHour,
    minStatusRetryDuration = minStatusRetryDuration,
    randomStatusHour = randomStatusHour,
    preSymptomsSpan = preSymptomsSpan,
    minHourContactNotif = minHourContactNotif,
    maxHourContactNotif = maxHourContactNotif,
    displayDepartmentLevel = displayDepartmentLevel,
    dontUseScannerHardwareBatching = gson.fromJson(
        dontUseScannerHardwareBatching,
        object : TypeToken<List<String>?>() {}.type
    ),
    filterConfig = filterConfig,
    filterMode = filterMode,
    serviceUUID = serviceUUID,
    characteristicUUID = characteristicUUID,
    backgroundServiceManufacturerData = backgroundServiceManufacturerData,
    qrCodeDeletionHours = qrCodeDeletionHours,
    qrCodeExpiredHours = qrCodeExpiredHours,
    qrCodeFormattedString = qrCodeFormattedString,
    qrCodeFormattedStringDisplayed = qrCodeFormattedStringDisplayed,
    qrCodeFooterString = qrCodeFooterString,
    venuesTimestampRoundingInterval = venuesTimestampRoundingInterval,
    proximityReactivationReminderHours = gson.fromJson(proximityReactivationReminderHours, object : TypeToken<List<Int>>() {}.type),
    venuesRetentionPeriod = venuesRetentionPeriod,
    privateEventVenueType = privateEventVenueType,
    displayRecordVenues = displayRecordVenues,
    displayPrivateEvent = displayPrivateEvent,
    displayIsolation = displayIsolation,
    isolationMinRiskLevel = isolationMinRiskLevel,
    positiveSampleSpan = positiveSampleSpan,
    isolationDuration = isolationDuration,
    postIsolationDuration = postIsolationDuration,
    covidIsolationDuration = covidIsolationDuration,
    venuesSalt = venuesSalt,
    allowNoAdvertisingDevice = allowNoAdvertisingDevice,
    unsupportedDevices = gson.fromJson(
        unsupportedDevices,
        object : TypeToken<List<String>?>() {}.type
    ),
    vaccinationCentersCount = vaccinationCentersCount,
    scanReportDelay = scanReportDelay,
    contagiousSpan = contagiousSpan,
    ameliUrl = ameliUrl,
    displaySanitaryCertificatesWallet = displaySanitaryCertificatesWallet,
    walletOldCertificateThresholdInDays = gson.fromJson(
        walletOldCertificateThresholdInDays,
        object : TypeToken<Map<String, Float>?>() {}.type
    ),
    walletPublicKeys = gson.fromJson(
        walletPublicKeys,
        object : TypeToken<List<WalletPublicKey>?>() {}.type
    ),
    isAnalyticsOn = isAnalyticsOn,
    analyticsApiVersion = analyticsApiVersion,
    testCertificateValidityThresholds = gson.fromJson(testCertificateValidityThresholds, object : TypeToken<List<Int>>() {}.type),
    cleaUrls = gson.fromJson(
        cleaUrls,
        object : TypeToken<List<String>?>() {}.type
    ),
    covidPlusWarning = covidPlusWarning,
    covidPlusNoTracing = covidPlusNoTracing,
    displayCertificateConversion = displayCertificateConversion,
    daysAfterCompletion = (
        gson.fromJson(
            daysAfterCompletion,
            object : TypeToken<List<ApiCodeValueEntry>>() {}.type
        ) as List<ApiCodeValueEntry>
        ).associate { Pair(it.code, it.value) },
    certificateConversionSidepOnlyCode = gson.fromJson(certificateConversionSidepOnlyCode, object : TypeToken<List<String>>() {}.type),
    conversionPublicKey = gson.fromJson(conversionPublicKey, object : TypeToken<Map<String, String>>() {}.type),
    conversionApiVersion = conversionApiVersion,
    maxCertBeforeWarning = maxCertBeforeWarning,
    noWaitDoses = (
        gson.fromJson(
            noWaitDoses,
            object : TypeToken<List<ApiCodeValueEntry>>() {}.type
        ) as List<ApiCodeValueEntry>
        ).associate { Pair(it.code, it.value) },
    ratingsKeyFiguresOpeningThreshold = ratingsKeyFiguresOpeningThreshold,
    displayUrgentDgs = displayUrgentDgs,
    notification = (
        gson.fromJson(
            notif,
            object : TypeToken<Map<String, Any>?>() {}.type
        ) as Map<String, Any>?
        )?.let {
        try {
            Configuration.Notification(
                title = (it["t"] as? String)!!,
                subtitle = (it["s"] as? String)!!,
                url = (it["u"] as? String)!!,
                version = (it["v"] as? Number)?.toInt()!!,
            )
        } catch (e: NullPointerException) {
            null
        }
    },
    generationServerPublicKey = generationServerPublicKey,
    activityPassSkipNegTestHours = activityPassSkipNegTestHours,
    displayActivityPass = displayActivityPass,
    renewThreshold = renewThreshold,
    smartWalletAges = gson.fromJson(smartWalletAges, SmartWalletAges::class.java),
    smartWalletExp = gson.fromJson(smartWalletExp, SmartWalletExp::class.java),
    smartWalletElg = gson.fromJson(smartWalletElg, SmartWalletElg::class.java),
    smartWalletVacc = gson.fromJson(smartWalletVacc, SmartWalletVacc::class.java),
    smartWalletNotif = smartWalletNotif,
    isSmartWalletOn = isSmartWalletOn,
    colorsCompareKeyFigures = (
        gson.fromJson(
            compareColors,
            object : TypeToken<List<Map<String, String>>>() {}.type
        ) as List<Map<String, String>>?
        )?.let {
        try {
            Configuration.ColorsCompareKeyFigures(
                Configuration.ColorCompareKeyFigures(
                    it[0]["d"],
                    it[0]["l"]
                ),
                Configuration.ColorCompareKeyFigures(
                    it[1]["d"],
                    it[1]["l"]
                ),
            )
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    },
    keyFiguresCombination = (
        gson.fromJson(
            this.keyFiguresCombination,
            object : TypeToken<List<Map<String, String>>>() {}.type
        ) as List<Map<String, String>>?
        )?.let { list ->
        list.map { map ->
            Configuration.KeyFigureCombination(
                map["t"],
                map["k1"],
                map["k2"]
            )
        }
    },
    noWaitDosesPivotDate = noWaitDosesPivotDate,
)
