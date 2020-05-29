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

internal object RobertConstant {
    const val STATUS_WORKER_NAME: String = "RobertManager.Status.Worker"
    const val EPOCH_DURATION_S: Int = 15 * 60
    const val KA_STRING_INPUT: String = "mac"
    const val KEA_STRING_INPUT: String = "tuples"
    const val DATA_RETENTION_PERIOD: Int = 14
    const val QUARANTINE_PERIOD: Int = 14
    const val CHECK_STATUS_FREQUENCY_HOURS: Int = 24
    const val RANDOM_STATUS_SEC: Long = 60 * 60 * 12
    const val PRE_SYMPTOMS_SPAN: Int = 2
    const val MIN_HOUR_CONTACT_NOTIF: Int = 7
    const val MAX_HOUR_CONTACT_NOTIF: Int = 19
    const val BLE_SERVICE_UUID: String = "0000fd64-0000-1000-8000-00805f9b34fb"
    const val BLE_CHARACTERISTIC_UUID: String = "a8f12d00-ee67-478b-b95f-65d599407756"
    const val BLE_BACKGROUND_SERVICE_MANUFACTURER_DATA_IOS: String = "1.0.0.0.0.0.0.0.0.0.0.8.0.0.0.0.0"

    object CONFIG {
        const val DATA_RETENTION_PERIOD: String = "app.dataRetentionPeriod"
        const val QUARANTINE_PERIOD: String = "app.quarantinePeriod"
        const val CHECK_STATUS_FREQUENCY: String = "app.checkStatusFrequency"
        const val RANDOM_STATUS_HOUR: String = "app.randomStatusHour"
        const val PRE_SYMPTOMS_SPAN: String = "app.preSymptomsSpan"
        const val APP_AVAILABILITY: String = "app.appAvailability"
        const val MIN_HOUR_CONTACT_NOTIF: String = "app.minHourContactNotif"
        const val MAX_HOUR_CONTACT_NOTIF: String = "app.maxHourContactNotif"
        const val CALIBRATION: String = "ble.calibration"
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
