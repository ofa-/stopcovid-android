/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/07/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.doubleTextItem
import com.mikepenz.fastadapter.GenericItem
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar

class PositiveTestFragment : MainFragment() {

    val args: PositiveTestFragmentArgs by navArgs()

    override fun getTitleKey(): String = "positiveTestController.title"

    private val dateFormat: DateFormat = SimpleDateFormat.getDateInstance(DateFormat.FULL)

    @SuppressLint("UseValueOf")
    override suspend fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += titleItem {
            text = strings["positiveTestController.explanation.title"]
            gravity = Gravity.CENTER
            identifier = "positiveTestController.explanation.title".hashCode().toLong()
        }
        items += captionItem {
            text = strings["positiveTestController.explanation.subtitle"]
            gravity = Gravity.CENTER
            identifier = "positiveTestController.explanation.subtitle".hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
        items += doubleTextItem {
            title = strings["positiveTestController.noDate"]
            onClickListener = View.OnClickListener {
                findNavControllerOrNull()
                    ?.safeNavigate(
                        PositiveTestFragmentDirections
                            .actionPositiveTestFragmentToSendHistoryFragment(args.code, args.firstSymptoms)
                    )
            }
            identifier = "positiveTestController.noDate".hashCode().toLong()
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
                    findNavControllerOrNull()
                        ?.safeNavigate(
                            PositiveTestFragmentDirections
                                .actionPositiveTestFragmentToSendHistoryFragment(args.code, args.firstSymptoms, Integer(index))
                        )
                }
                identifier = title.hashCode().toLong()
            }
            if (index != 14) {
                items += dividerItem { }
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return items
    }
}