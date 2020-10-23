package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
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
        val infos = InfoCenterManager.infos.value?.peekContent()
        val tags = InfoCenterManager.tags.value?.peekContent()

        if (infoCenterStrings != null && infos != null && tags != null) {
            infos.forEach { info ->
                info.tagIds?.mapNotNull { tagId ->
                    tags.firstOrNull { tag ->
                        tag.id == tagId
                    }
                }?.filter {
                    infoCenterStrings[it.labelKey] != null
                }
                items += infoCenterDetailCardItem {
                    header = info.timestamp.seconds.getRelativeDateTimeString(requireContext())
                    title = infoCenterStrings[info.titleKey]
                    subtitle = infoCenterStrings[info.descriptionKey]
                    link = infoCenterStrings[info.buttonLabelKey]
                    this.tags = tags ?: emptyList()
                    strings = infoCenterStrings
                    url = infoCenterStrings[info.urlKey]
                    identifier = info.timestamp
                }
                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
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