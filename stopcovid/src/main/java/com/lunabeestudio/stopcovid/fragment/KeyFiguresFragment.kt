/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.graphics.Color.parseColor
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.lunabeestudio.robert.utils.EventObserver
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.fastitem.KeyFigureCardItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.keyFigureCardItem
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigureCategory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

class KeyFiguresFragment : MainFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        KeyFiguresManager.figures.observe(viewLifecycleOwner, EventObserver(this.javaClass.name.hashCode()) {
            refreshScreen()
        })

        binding?.emptyButton?.setOnClickListener {
            showLoading()
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                KeyFiguresManager.appForeground(requireContext())
                withContext(Dispatchers.Main) {
                    refreshScreen()
                }
            }
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        KeyFiguresManager.figures.value?.peekContent()?.let { keyFigures ->
            if (keyFigures.isNotEmpty()) {
                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.count().toLong()
                }
                items += bigTitleItem {
                    text = strings["keyFiguresController.section.health"]
                    identifier = items.count().toLong()
                }
                keyFigures.filter { it.category == KeyFigureCategory.HEALTH }.forEach { figure ->
                    items += itemForFigure(figure)
                }

                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.count().toLong()
                }

                items += bigTitleItem {
                    text = strings["keyFiguresController.section.app"]
                    identifier = items.count().toLong()
                }
                keyFigures.filter { it.category == KeyFigureCategory.APP }.forEach { figure ->
                    items += itemForFigure(figure)
                }
            }
        }

        return items
    }

    override fun refreshScreen() {
        super.refreshScreen()

        binding?.emptyTitleTextView?.text = strings["infoCenterController.noInternet.title"]
        binding?.emptyDescriptionTextView?.text = strings["infoCenterController.noInternet.subtitle"]
        binding?.emptyButton?.text = strings["common.retry"]
    }

    @OptIn(ExperimentalTime::class)
    private fun itemForFigure(figure: KeyFigure): KeyFigureCardItem {
        return keyFigureCardItem {
            updatedAt = stringsFormat(
                "myHealthController.notification.update",
                figure.lastUpdate.seconds.getRelativeDateTimeString(requireContext())
            )
            value = figure.valueGlobalToDisplay
            label = strings["${figure.labelKey}.label"]
            description = strings["${figure.labelKey}.description"]
            identifier = figure.labelKey.hashCode().toLong()

            if (requireContext().isNightMode()) {
                strings["${figure.labelKey}.colorCode.dark"]
            } else {
                strings["${figure.labelKey}.colorCode.light"]
            }?.let {
                color = parseColor(it)
            }
        }
    }

    override fun getTitleKey(): String = "keyFiguresController.title"

}
