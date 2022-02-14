/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/9 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

data class SmartWalletTestingCombo(
    val config: Config,
    val sections: List<Section>,
) {
    data class Config(
        val vaccineProduct: VaccineProduct,
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
            val today: String,
            val dob: String,
            val inputs: List<Input>,
            val output: Output,
        ) {
            data class Input(
                val id: Int,
                val type: String,
                val doi: String,
                val products: List<String>,
                val doses: List<Dose>,
                val prefixes: List<String>,
            ) {
                data class Dose(
                    val c: Int,
                    val t: Int,
                )
            }

            data class Output(
                val id: Int?,
                val elg: String?,
            )
        }
    }
}