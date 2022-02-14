/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/7 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager.model

import com.lunabeestudio.domain.model.smartwallet.SmartWalletVaccineValidityPivot

class ApiVaccineValidityRule(
    val doses: ApiDoses?,
    val products: List<String>?,
    val valid: ApiValidityRange,
) {
    fun toRule(): SmartWalletVaccineValidityPivot.Rule {
        return SmartWalletVaccineValidityPivot.Rule(
            dose = doses?.toDose(),
            products = products,
            validityRange = valid.toSmartWalletValidityRange(),
        )
    }
}