/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/06/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.ble

import java.util.UUID

interface RobertBleSettings {
    val serviceUuid: UUID
    val servicePayloadCharacteristicUuid: UUID
    val backgroundServiceManufacturerDataIOS: ByteArray
}