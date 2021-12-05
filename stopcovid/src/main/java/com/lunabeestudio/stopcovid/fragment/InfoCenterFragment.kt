/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/29 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.getRelativeDateTimeString
import com.lunabeestudio.stopcovid.extension.startTextIntent
import com.lunabeestudio.stopcovid.fastitem.infoCenterDetailCardItem
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

import java.util.Date
import java.text.SimpleDateFormat

class InfoCenterFragment : TimeMainFragment() {

    private val tagRecyclerPool = RecyclerView.RecycledViewPool()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        infoCenterManager.infos.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        infoCenterManager.tags.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        infoCenterManager.strings.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }

        binding?.emptyLayout?.emptyButton?.setOnClickListener {
            showLoading()
            viewLifecycleOwnerOrNull()?.lifecycleScope?.launch(Dispatchers.IO) {
                infoCenterManager.refreshIfNeeded(requireContext())
                withContext(Dispatchers.Main) {
                    refreshScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
            putBoolean(Constants.SharedPrefs.HAS_NEWS, false)
        }
        viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
            infoCenterManager.refreshIfNeeded(requireContext())
        }
    }

    override fun getTitleKey(): String = "infoCenterController.title"

    @OptIn(kotlin.time.ExperimentalTime::class)
    override suspend fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        val infoCenterStrings = infoCenterManager.strings.value?.peekContent()
        val infos = infoCenterManager.infos.value?.peekContent() ?: emptyList()
        val tags = infoCenterManager.tags.value?.peekContent() ?: emptyList()

        if (infoCenterStrings != null) {
            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.size.toLong()
            }

            infos.forEach { info ->
                val infoTitle = infoCenterStrings[info.titleKey]
                val infoDescription = infoCenterStrings[info.descriptionKey]

                // Make sure we have at least the title or description
                if (infoTitle != null || infoDescription != null) {
                    val filteredTags = info.tagIds?.mapNotNull { tagIds ->
                        tags.firstOrNull { it.id == tagIds }
                    }

                    items += infoCenterDetailCardItem {
                        header = info.timestamp.seconds.getRelativeDateTimeString(requireContext(), strings["common.justNow"])
                        title = infoTitle
                        body = infoDescription
                        link = infoCenterStrings[info.buttonLabelKey]
                        this.tags = filteredTags ?: emptyList()
                        strings = infoCenterStrings
                        url = infoCenterStrings[info.urlKey]
                        tagRecyclerViewPool = this@InfoCenterFragment.tagRecyclerPool
                        shareContentDescription = strings["accessibility.hint.info.share"]
                        onShareCard = {
                            val date = Date(seconds(info.timestamp).inWholeMilliseconds)
                            val text = infoShareText(infoTitle, infoDescription, date)
                            requireContext().startTextIntent(text)
                        }
                        identifier = info.titleKey.hashCode().toLong()
                    }

                    items += spaceItem {
                        spaceRes = R.dimen.spacing_large
                        identifier = items.size.toLong()
                    }
                }
            }
        }

        return items
    }

    private fun infoShareText(infoTitle: String?, infoBody: String?, infoDate: Date): String {
        val dateStr = SimpleDateFormat("EEE dd LLL yyyy | HH:mm").format(infoDate)

        return "${infoTitle}\n[TAC | ${dateStr}]\n\n${infoBody}"
    }

    override fun refreshScreen() {
        super.refreshScreen()

        binding?.emptyLayout?.emptyTitleTextView?.text = strings["infoCenterController.noInternet.title"]
        binding?.emptyLayout?.emptyDescriptionTextView?.text = strings["infoCenterController.noInternet.subtitle"]
        binding?.emptyLayout?.emptyButton?.text = strings["common.retry"]
    }

    override fun timeRefresh() {
        refreshScreen()
    }
}
