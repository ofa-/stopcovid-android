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

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.coreui.databinding.FragmentRecyclerViewBinding
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter

class ReminderBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val strings: HashMap<String, String>
        get() = StringsManager.strings

    private var binding: FragmentRecyclerViewBinding? = null
    private var adapter: FastItemAdapter<GenericItem> = GenericFastItemAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRecyclerViewBinding.inflate(inflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter.attachDefaultListeners = false
        binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        binding?.recyclerView?.adapter = adapter

        StringsManager.liveStrings.observe(viewLifecycleOwner) {
            setItems()
            (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun setItems() {
        val items = mutableListOf<GenericItem>()
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
        }
        items += titleItem {
            text = strings["home.deactivate.actionSheet.title"]
            gravity = Gravity.CENTER
            identifier = text.hashCode().toLong()
        }
        items += captionItem {
            text = strings["home.deactivate.actionSheet.subtitle"]
            gravity = Gravity.CENTER
            identifier = text.hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        robertManager.proximityReactivationReminderHours.forEach { inHour ->
            items += dividerItem {
                marginStartRes = null
            }
            items += linkItem {
                text = if (inHour > 1) {
                    strings.stringsFormat("home.deactivate.actionSheet.hours.plural", inHour)
                } else {
                    strings.stringsFormat("home.deactivate.actionSheet.hours.singular", inHour)
                }
                gravity = Gravity.CENTER
                onClickListener = View.OnClickListener {
                    (requireContext().applicationContext as StopCovid).setActivateReminder(inHour)
                    dismiss()
                }
                identifier = text.hashCode().toLong()
            }
        }
        items += dividerItem {
            marginStartRes = null
        }
        items += linkItem {
            text = strings["home.deactivate.actionSheet.noReminder"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                dismiss()
            }
            identifier = text.hashCode().toLong()
        }
        adapter.setNewList(items)
    }
}