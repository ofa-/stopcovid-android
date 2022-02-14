/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/4 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager.model

import com.lunabeestudio.domain.model.smartwallet.SmartWalletDose

class ApiDoses(
    val c: Int,
    val t: Int
) {
    fun toDose(): SmartWalletDose {
        return SmartWalletDose(
            current = c,
            target = t
        )
    }
}