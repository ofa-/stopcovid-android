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

internal class ApiConfiguration(
    @SerializedName("version")
    val version: Int,
    @SerializedName("app.apiVersion")
    val apiVersion: String,
    @SerializedName("app.warningApiVersion")
    val warningApiVersion: String,
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
    @SerializedName("ble.calibration")
    val calibration: String,
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
    @SerializedName("app.positiveSampleSpan")
    val positiveSampleSpan: Int,
    @SerializedName("app.isolation.duration")
    val isolationDuration: Long,
    @SerializedName("app.postIsolation.duration")
    val postIsolationDuration: Long,
    @SerializedName("app.venuesSalt")
    val venuesSalt: Int,
    @SerializedName("app.allowNoAdvertisingDevice")
    val allowNoAdvertisingDevice: Boolean,
    @SerializedName("app.unsupportedDevices")
    val unsupportedDevices: String?,
)

internal fun ApiConfiguration.toDomain(gson: Gson) = Configuration(
    version = version,
    apiVersion = apiVersion,
    warningApiVersion = warningApiVersion,
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
    calibration = gson.fromJson<List<ApiDeviceParameterCorrection>>(
        calibration,
        object : TypeToken<List<ApiDeviceParameterCorrection>>() {}.type
    ).map { it.toDomain() },
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
    positiveSampleSpan = positiveSampleSpan,
    isolationDuration = isolationDuration,
    postIsolationDuration = postIsolationDuration,
    venuesSalt = venuesSalt,
    allowNoAdvertisingDevice = allowNoAdvertisingDevice,
    unsupportedDevices = gson.fromJson(
        unsupportedDevices,
        object : TypeToken<List<String>?>() {}.type
    )
)