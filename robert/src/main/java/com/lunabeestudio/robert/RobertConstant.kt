/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert

import com.lunabeestudio.robert.manager.LocalProximityFilter

object RobertConstant {
    const val API_VERSION: String = "v1"
    const val STATUS_WORKER_NAME: String = "RobertManager.Status.Worker"
    const val EPOCH_DURATION_S: Int = 15 * 60
    const val KA_STRING_INPUT: String = "mac"
    const val KEA_STRING_INPUT: String = "tuples"
    const val DATA_RETENTION_PERIOD: Int = 14
    const val QUARANTINE_PERIOD: Int = 14
    const val CHECK_STATUS_FREQUENCY_HOURS: Int = 24
    const val RANDOM_STATUS_HOUR: Int = 12
    const val PRE_SYMPTOMS_SPAN: Int = 2
    const val MIN_HOUR_CONTACT_NOTIF: Int = 7
    const val MAX_HOUR_CONTACT_NOTIF: Int = 19
    const val BLE_SERVICE_UUID: String = "0000fd64-0000-1000-8000-00805f9b34fb"
    const val BLE_CHARACTERISTIC_UUID: String = "a8f12d00-ee67-478b-b95f-65d599407756"
    const val BLE_BACKGROUND_SERVICE_MANUFACTURER_DATA_IOS: String = "1.0.0.0.0.0.0.0.0.0.0.8.0.0.0.0.0"
    const val BLE_FILTER_CONFIG: String = "{\"a\":4.3429448190325175,\"b\":0.1,\"deltas\":[39.0,27.0,23.0,21.0,20.0,19.0,18.0,17.0,16.0,15.0],\"durationThreshold\":120,\"p0\":-66.0,\"rssiThreshold\":-25,\"timeOverlap\":60,\"timeWindow\":120,\"riskThreshold\":0.1}"
    val BLE_FILTER_MODE: LocalProximityFilter.Mode = LocalProximityFilter.Mode.RISKS
    const val MIN_GAP_SUCCESS_STATUS: Long = 30L * 60L * 1000L

    object CONFIG {
        const val API_VERSION: String = "app.apiVersion"
        const val DATA_RETENTION_PERIOD: String = "app.dataRetentionPeriod"
        const val QUARANTINE_PERIOD: String = "app.quarantinePeriod"
        const val CHECK_STATUS_FREQUENCY: String = "app.checkStatusFrequency"
        const val RANDOM_STATUS_HOUR: String = "app.randomStatusHour"
        const val PRE_SYMPTOMS_SPAN: String = "app.preSymptomsSpan"
        const val APP_AVAILABILITY: String = "app.appAvailability"
        const val MIN_HOUR_CONTACT_NOTIF: String = "app.minHourContactNotif"
        const val MAX_HOUR_CONTACT_NOTIF: String = "app.maxHourContactNotif"
        const val CALIBRATION: String = "ble.calibration"
        const val FILTER_CONFIG: String = "ble.filterConfig"
        const val FILTER_MODE: String = "ble.filterMode"
        const val SERVICE_UUID: String = "ble.serviceUUID"
        const val CHARACTERISTIC_UUID: String = "ble.characteristicUUID"
        const val BACKGROUND_SERVICE_MANUFACTURER_DATA: String = "ble.backgroundServiceManufacturerData"
    }

    object PREFIX {
        const val C1: Byte = 0b00000001
        const val C2: Byte = 0b00000010
        const val C3: Byte = 0b00000011
        const val C4: Byte = 0b00000100
    }
}
