/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.itemForFigure
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.KeyFigureCardItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigureCategory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime

class KeyFiguresFragment : KeyFigureGenericFragment() {

    override fun showPostalCodeBottomSheet() {
        findNavControllerOrNull()?.safeNavigate(KeyFiguresFragmentDirections.actionKeyFiguresFragmentToPostalCodeBottomSheetFragment())
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        keyFiguresManager.figures.value?.peekContent()?.let { keyFigures ->
            if (keyFigures.isNotEmpty()) {
                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.count().toLong()
                }
                items += bigTitleItem {
                    text = strings["keyFiguresController.section.health"]
                    identifier = items.count().toLong()
                }
                items += captionItem {
                    text = strings["keyFiguresController.section.health.subtitle"]
                    identifier = text.hashCode().toLong()
                }
                items += linkItem {
                    text = strings["keyFiguresController.section.health.button"]
                    onClickListener = View.OnClickListener {
                        findNavControllerOrNull()
                            ?.safeNavigate(KeyFiguresFragmentDirections.actionKeyFiguresFragmentToMoreKeyFigureFragment())
                    }
                    identifier = text.hashCode().toLong()
                }
                keyFigures
                    .filter { it.category == KeyFigureCategory.HEALTH }
                    .mapNotNullTo(items) { itemForFigure(it, true) }

                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.count().toLong()
                }

                items += bigTitleItem {
                    text = strings["keyFiguresController.section.app"]
                    identifier = items.count().toLong()
                }
                keyFigures
                    .filter { it.category == KeyFigureCategory.APP }
                    .mapNotNullTo(items) { itemForFigure(it, true) }
            }
        }

        return items
    }

    @OptIn(ExperimentalTime::class)
    private fun itemForFigure(figure: KeyFigure, useDateTime: Boolean): KeyFigureCardItem? {
        return figure.itemForFigure(
            requireContext(),
            sharedPrefs,
            numberFormat,
            strings,
            useDateTime
        ) {
            shareContentDescription = strings["accessibility.hint.keyFigure.share"]
            onShareCard = { binding ->
                viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
                    val uri = getShareCaptureUri(binding, "$label")
                    withContext(Dispatchers.Main) {
                        val shareString = if (rightLocation == null) {
                            stringsFormat("keyFigure.sharing.national", label, leftValue)
                        } else {
                            stringsFormat("keyFigure.sharing.department", label, leftLocation, leftValue, label, rightValue)
                        }
                        ShareManager.shareImageAndText(requireContext(), uri, shareString) {
                            strings["common.error.unknown"]?.let { showErrorSnackBar(it) }
                        }
                    }
                }
            }
            onClickListener = View.OnClickListener {
                analyticsManager.reportAppEvent(requireContext(), AppEventName.e9, null)
                findNavControllerOrNull()?.safeNavigate(
                    KeyFiguresFragmentDirections.actionKeyFiguresFragmentToKeyFigureDetailsFragment(
                        figure.labelKey
                    )
                )
            }
            descriptionMaxLines = 2
        }
    }

    override fun getTitleKey(): String = "keyFiguresController.title"
}
