/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/29 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.stopcovid.model.DepartmentKeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigure

private const val CORSICA_KEY: String = "20"
private const val CORSE_DU_SUD_KEY: String = "2A"
private const val HAUTE_CORSE_KEY: String = "2B"

private val CORSE_DU_SUD: Array<String> = arrayOf("200", "201")
private val OVERSEAS_FRANCE: Array<String> = arrayOf("97", "98")

val KeyFigure.labelStringKey: String
    get() = "$labelKey.label"

val KeyFigure.descriptionStringKey: String
    get() = "$labelKey.description"

fun KeyFigure.colorStringKey(dark: Boolean?): String = if (dark == true) {
    "$labelKey.colorCode.dark"
} else {
    "$labelKey.colorCode.light"
}

fun KeyFigure.getKeyFigureForPostalCode(postalCode: String?): DepartmentKeyFigure? {
    var key = postalCode?.take(2)

    if (key == CORSICA_KEY) { // Corsica case
        key = if (postalCode?.take(3) in CORSE_DU_SUD) CORSE_DU_SUD_KEY else HAUTE_CORSE_KEY
    } else if (key in OVERSEAS_FRANCE) { // Overseas France case
        key = postalCode?.take(3)
    }

    return valuesDepartments?.firstOrNull { it.dptNb == key }
}

fun List<KeyFigure>?.postalCodeExists(postalCode: String): Boolean {
    return this?.any {
        it.getKeyFigureForPostalCode(postalCode) != null
    } ?: false
}

fun List<KeyFigure>?.getDepartmentLabel(postalCode: String?): String? {
    val localization = this?.lastOrNull()?.getKeyFigureForPostalCode(
        postalCode
    ) ?: this?.firstOrNull()?.getKeyFigureForPostalCode(
        postalCode
    )

    return localization?.dptLabel
}