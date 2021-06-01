/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble

import java.util.UUID

/**
 * @param serviceUuid Service UUID used by BLE advertiser / scanner
 * @param servicePayloadCharacteristicUuid Service Characteristic UUID used for reading payload
 * @param backgroundServiceManufacturerDataIOS Byte array of manufacturer data advertised by iOS in background
 * @param txCompensationGain Tx Compensation Gain (in dBm)
 * @param rxCompensationGain Tx Compensation Gain (in dBm)
 * @param scanCacheTimeout Timeout (in ms) for scan cache expiration
 * @param identityCacheTimeout Timeout (in ms) for identity cache expiration
 * @param maxCacheSize Maximum cache capacity
 * @param scanReportDelay Delay (in ms) between each ble scan report
 * @param connectionTimeout Timeout (in ms) for establishing a GATT connection
 * @param readRemoteRssiTimeout Timeout (in ms) for reading remote RSSI
 * @param discoverServicesTimeout Timeout (in ms) for discovering remote GATT services
 * @param readRemotePayloadTimeout Timeout (in ms) for reading remote payload on connected GATT
 * @param writeRemotePayloadTimeout Timeout (in ms) for writing remote payload on connected GATT
 * @param useScannerHardwareBatching True to use scanner hardware batching if supported
 * @param maxDelayBetweenExchange Maximum delay (in s) between successful exchanges for a same device
 * @param maxSuccessiveFailure Maximum successive failure for a same device
 * */
@Suppress("ArrayInDataClass")
data class BleSettings(
    val serviceUuid: UUID,
    val servicePayloadCharacteristicUuid: UUID,
    val backgroundServiceManufacturerDataIOS: ByteArray,
    val txCompensationGain: Int,
    val rxCompensationGain: Int,
    val scanCacheTimeout: Long = 30 * 1000,
    val identityCacheTimeout: Long = 150 * 1000,
    val maxCacheSize: Int = 1000,
    val scanReportDelay: Long = 1 * 1000,
    val connectionTimeout: Long = 4 * 1000,
    val readRemoteRssiTimeout: Long = 1 * 1000,
    val discoverServicesTimeout: Long = 4 * 1000,
    val readRemotePayloadTimeout: Long = 1 * 1000,
    val writeRemotePayloadTimeout: Long = 1 * 1000,
    val useScannerHardwareBatching: Boolean = true,
    val maxDelayBetweenExchange: Long = 45 * 1000,
    val maxSuccessiveFailure: Int = 10,

    val _devDebugForceNoAdvertiser: Boolean = false,
)