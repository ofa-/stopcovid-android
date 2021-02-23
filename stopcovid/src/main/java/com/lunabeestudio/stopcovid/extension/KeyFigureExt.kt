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

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.fastitem.KeyFigureCardItem
import com.lunabeestudio.stopcovid.fastitem.keyFigureCardItem
import com.lunabeestudio.stopcovid.model.DepartmentKeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigure
import java.text.NumberFormat
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

private const val CORSICA_KEY: String = "20"
private const val CORSE_DU_SUD_KEY: String = "2A"
private const val HAUTE_CORSE_KEY: String = "2B"

private val CORSE_DU_SUD: Array<String> = arrayOf("200", "201")
private val OVERSEAS_FRANCE: Array<String> = arrayOf("97", "98")

val KeyFigure.labelStringKey: String
    get() = "$labelKey.label"

val KeyFigure.labelShortStringKey: String
    get() = "$labelKey.shortLabel"

val KeyFigure.descriptionStringKey: String
    get() = "$labelKey.description"

val KeyFigure.learnMoreStringKey: String
    get() = "$labelKey.learnMore"

val KeyFigure.limitLineStringKey: String
    get() = "$labelKey.limitLine"

fun KeyFigure.colorStringKey(dark: Boolean?): String = if (dark == true) {
    "$labelKey.colorCode.dark"
} else {
    "$labelKey.colorCode.light"
}

fun KeyFigure.hasAverageChart(): Boolean = !avgSeries.isNullOrEmpty()

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

@OptIn(ExperimentalTime::class)
fun KeyFigure.itemForFigure(context: Context,
    sharedPrefs: SharedPreferences,
    numberFormat: NumberFormat,
    strings: HashMap<String, String>,
    useDateTime: Boolean,
    block: (KeyFigureCardItem.() -> Unit)): KeyFigureCardItem {
    return keyFigureCardItem {
        val extractDate: Long
        if (sharedPrefs.hasChosenPostalCode) {
            val departmentKeyFigure = getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)

            if (departmentKeyFigure != null) {
                rightLocation = strings["common.country.france"]
                leftLocation = departmentKeyFigure.dptLabel
                leftValue = departmentKeyFigure.valueToDisplay?.formatNumberIfNeeded(numberFormat)
                rightValue = valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
                rightTrend = trend?.getTrend()
                leftTrend = departmentKeyFigure.trend?.getTrend()
                extractDate = departmentKeyFigure.extractDate
            } else {
                leftLocation = strings["common.country.france"]
                leftValue = valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
                leftTrend = trend?.getTrend()
                extractDate = this@itemForFigure.extractDate
            }
        } else {
            leftValue = valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
            leftTrend = trend?.getTrend()
            extractDate = this@itemForFigure.extractDate
        }
        updatedAt = strings.stringsFormat(
            "keyFigures.update",
            if (useDateTime) {
                extractDate.seconds.getRelativeDateTimeString(context, strings["common.justNow"])
            } else {
                extractDate.seconds.getRelativeDateString()
            }
        )

        label = strings[labelStringKey]
        description = strings[descriptionStringKey]
        identifier = labelKey.hashCode().toLong()

        strings[colorStringKey(context.isNightMode())]?.let {
            color = Color.parseColor(it)
        }
    }.apply(block)
}