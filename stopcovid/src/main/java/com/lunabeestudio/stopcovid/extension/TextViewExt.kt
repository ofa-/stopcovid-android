/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/09/13 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.model.ChartData

fun TextView.setLegend1FromChartData(chartData: Array<ChartData>) {
    if (chartData.isNotEmpty()) {
        setTextOrHide(chartData[0].description) {
            setTextColor(chartData[0].color)
            TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(chartData[0].color))
        }
    }
    isVisible = chartData.isNotEmpty()
}

fun TextView.setLegend2FromChartData(chartData: Array<ChartData>) {
    if (chartData.size > 1) {
        setTextOrHide(chartData[1].description) {
            setTextColor(chartData[1].color)
            TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(chartData[1].color))
        }
    }
    isVisible = chartData.size > 1
}