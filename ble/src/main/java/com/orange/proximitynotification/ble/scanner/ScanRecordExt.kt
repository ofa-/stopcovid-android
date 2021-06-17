/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. 
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2021/02/19 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble.scanner

import android.os.ParcelUuid
import no.nordicsemi.android.support.v18.scanner.ScanRecord
import kotlin.experimental.and

internal fun ScanRecord.hasServiceUuid(serviceUuid: ParcelUuid): Boolean =
    serviceUuids?.contains(serviceUuid) == true || serviceData?.get(serviceUuid) != null

internal fun ScanRecord.matchesManufacturerDataMask(manufacturerId: Int, mask: ByteArray): Boolean {
    val manufacturerData = getManufacturerSpecificData(manufacturerId)

    if (manufacturerData == null || manufacturerData.size != mask.size) {
        return false
    }

    mask.forEachIndexed { index, byte ->
        if ((manufacturerData[index] and byte) != byte) {
            return false
        }
    }

    return true
}
