/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/7 - for the TOUS-ANTI-COVID project
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/7 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

class SmartWalletTesting(
    val config: Config,
    val sections: List<Section>,
) {
    data class Config(
        val vaccineProduct: VaccineProduct
    ) {
        data class VaccineProduct(
            val ar: List<String>,
            val ja: List<String>,
            val az: List<String>,
        )
    }

    data class Section(
        val section: String,
        val tests: List<Test>,
    ) {

        data class Test(
            val desc: String,
            val input: Input,
            val output: Output
        ) {
            data class Input(
                val today: String,
                val dob: String,
                val type: String,
                val doi: String,
                val products: List<String>,
                val doses: List<Dose>,
                val prefixes: List<String>
            ) {
                data class Dose(
                    val c: Int,
                    val t: Int
                )
            }

            data class Output(
                val elg: String?,
                val exp: String?,
                val start: String?,
            )
        }
    }
}