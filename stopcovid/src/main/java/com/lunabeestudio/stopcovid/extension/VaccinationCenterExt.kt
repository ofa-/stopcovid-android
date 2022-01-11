/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/04/02 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.location.Location
import android.view.View
import com.lunabeestudio.stopcovid.fastitem.vaccinationCenterCardItem
import com.lunabeestudio.stopcovid.model.VaccinationCenter
import com.mikepenz.fastadapter.GenericItem
import java.text.DateFormat
import java.util.Date

val VaccinationCenter.location: Location?
    get() = if (latitude != null && longitude != null) {
        Location("").apply {
            latitude = this@location.latitude
            longitude = this@location.longitude
        }
    } else {
        null
    }

private fun VaccinationCenter.getDisplayAddress(): String {
    val firstLine = listOfNotNull(streetNumber, streetName).filter { it.isNotBlank() }.joinToString()
    val secondLine = listOfNotNull(postalCode, locality).filter { it.isNotBlank() }.joinToString()
    return listOf(firstLine, secondLine).filter { it.isNotBlank() }.joinToString("\n")
}

private fun VaccinationCenter.availabilityTimestamp(): Long? {
    return if (openingTimestamp == null || System.currentTimeMillis() / 1000 > openingTimestamp) {
        null
    } else {
        openingTimestamp
    }
}

fun VaccinationCenter.toItem(strings: Map<String, String>, dateFormat: DateFormat, onClickListener: View.OnClickListener?): GenericItem {
    return vaccinationCenterCardItem {
        title = name
        modality = modalities
        address = getDisplayAddress()
        openingDate = availabilityTimestamp()?.let {
            "${strings["vaccinationCenterCell.openingDate.from"]} ${dateFormat.format(Date(it))}"
        }
        openingTime = planning
        openingDateHeader = strings["vaccinationCenterCell.openingDate.title"].takeUnless {
            openingDate.isNullOrBlank() && openingTime.isNullOrBlank()
        }
        this.onClickListener = onClickListener
        identifier = postalCode.hashCode().toLong()
    }
}