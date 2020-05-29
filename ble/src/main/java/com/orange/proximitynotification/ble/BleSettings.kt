/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble

import java.util.UUID

/**
 * @param serviceUuid Service UUID used by BLE advertiser / scanner
 * @param servicePayloadCharacteristicUuid Service Characteristic UUID used for reading payload
 * @param backgroundServiceManufacturerDataIOS Byte array of manufacturer data advertised by iOS in background
 * @param txCompensationGain Tx Compensation Gain (in dBm)
 * @param rxCompensationGain Tx Compensation Gain (in dBm)
 * @param cacheTimeout Timeout (in ms) for internal cache expiration
 * @param scanReportDelay Delay (in ms) between each ble scan report
 * @param connectionTimeout Timeout (in ms) for establishing a GATT connection
 * */
@Suppress("ArrayInDataClass")
data class BleSettings(
    val serviceUuid: UUID,
    val servicePayloadCharacteristicUuid: UUID,
    val backgroundServiceManufacturerDataIOS: ByteArray,
    val txCompensationGain: Int,
    val rxCompensationGain: Int,
    val cacheTimeout: Long = 30 * 1000,
    val scanReportDelay: Long = 1000,
    val connectionTimeout: Long = 5 * 1000
)