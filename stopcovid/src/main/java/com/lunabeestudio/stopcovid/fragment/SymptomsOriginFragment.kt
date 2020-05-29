/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/07/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.view.Gravity
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.doubleTextItem
import com.mikepenz.fastadapter.GenericItem
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar

class SymptomsOriginFragment : MainFragment() {

    val args: SymptomsOriginFragmentArgs by navArgs()

    override fun getTitleKey(): String = "symptomsOriginController.title"

    private val dateFormat: DateFormat = SimpleDateFormat.getDateInstance(DateFormat.FULL)

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += bigTitleItem {
            text = strings["symptomsOriginController.title2"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += titleItem {
            text = strings["symptomsOriginController.explanation.title"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["symptomsOriginController.explanation.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
        items += doubleTextItem {
            title = strings["symptomsOriginController.noSymptoms"]
            onClickListener = View.OnClickListener {
                findNavController()
                    .navigate(SymptomsOriginFragmentDirections.actionSymptomsOriginFragmentToSendHistoryFragment(args.code, -1))
            }
        }
        items += dividerItem { }
        val calendar = Calendar.getInstance()
        repeat(15) { index ->
            val title = when (index) {
                0 -> strings["common.today"]
                1 -> strings["common.yesterday"]
                else -> String.format(strings["common.daysAgo"] ?: "", index)
            }
            val currentDate = calendar.time
            items += doubleTextItem {
                this.title = title
                caption = dateFormat.format(currentDate)
                onClickListener = View.OnClickListener {
                    findNavController()
                        .navigate(SymptomsOriginFragmentDirections.actionSymptomsOriginFragmentToSendHistoryFragment(args.code, index))
                }
            }
            items += dividerItem { }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        items += doubleTextItem {
            title = strings["common.iDontKnow"]
            onClickListener = View.OnClickListener {
                findNavController()
                    .navigate(SymptomsOriginFragmentDirections.actionSymptomsOriginFragmentToSendHistoryFragment(args.code, -1))
            }
        }
        return items
    }
}