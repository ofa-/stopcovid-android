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
    val apiVersion: String,
    val warningApiVersion: String,
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
    val calibration: List<DeviceParameterCorrection>,
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
    val positiveSampleSpan: Int,
    val isolationDuration: Long,
    val postIsolationDuration: Long,
    val venuesSalt: Int,
    val allowNoAdvertisingDevice: Boolean,
    val unsupportedDevices: List<String>?,
    val vaccinationCentersCount: Int,
)