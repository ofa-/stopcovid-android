package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.robert.utils.EventObserver
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.fastitem.infoCenterDetailCardItem
import com.lunabeestudio.stopcovid.manager.InfoCenterManager
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

class InfoCenterFragment : TimeMainFragment() {

    private val tagRecyclerPool = RecyclerView.RecycledViewPool()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        InfoCenterManager.infos.observe(viewLifecycleOwner, EventObserver(this.javaClass.name.hashCode()) {
            refreshScreen()
        })
        InfoCenterManager.tags.observe(viewLifecycleOwner, EventObserver(this.javaClass.name.hashCode()) {
            refreshScreen()
        })
        InfoCenterManager.strings.observe(viewLifecycleOwner, EventObserver(this.javaClass.name.hashCode()) {
            refreshScreen()
        })

        binding?.emptyButton?.setOnClickListener {
            showLoading()
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                InfoCenterManager.appForeground(requireContext())
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
    }

    override fun getTitleKey(): String = "infoCenterController.title"

    @OptIn(ExperimentalTime::class)
    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        val infoCenterStrings = InfoCenterManager.strings.value?.peekContent()
        val infos = InfoCenterManager.infos.value?.peekContent() ?: emptyList()
        val tags = InfoCenterManager.tags.value?.peekContent() ?: emptyList()

        if (infoCenterStrings != null) {
            infos.forEach { info ->
                val filteredTags = info.tagIds?.map { tagIds ->
                    tags.first { it.id == tagIds }
                }

                items += infoCenterDetailCardItem {
                    header = info.timestamp.seconds.getRelativeDateTimeString(requireContext())
                    title = infoCenterStrings[info.titleKey]
                    body = infoCenterStrings[info.descriptionKey]
                    link = infoCenterStrings[info.buttonLabelKey]
                    this.tags = filteredTags ?: emptyList()
                    strings = infoCenterStrings
                    url = infoCenterStrings[info.urlKey]
                    tagRecyclerViewPool = this@InfoCenterFragment.tagRecyclerPool
                    identifier = info.titleKey.hashCode().toLong()
                }

                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.size.toLong()
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

    override fun timeRefresh() {
        refreshScreen()
    }
}