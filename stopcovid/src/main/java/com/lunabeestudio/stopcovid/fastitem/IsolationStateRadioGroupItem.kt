/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/12/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.databinding.ItemIsolationStateRadioGroupBinding
import com.lunabeestudio.stopcovid.manager.IsolationFormStateEnum
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class IsolationStateRadioGroupItem : AbstractBindingItem<ItemIsolationStateRadioGroupBinding>() {

    var groupTitle: String? = null
    var allGoodLabel: String? = null
    var symptomsLabel: String? = null
    var contactLabel: String? = null
    var positiveLabel: String? = null
    var onStateSymptomsClick: ((RadioGroup) -> Unit)? = null
    var selectedState: IsolationFormStateEnum? = null
    var onStateChangedListener: RadioGroup.OnCheckedChangeListener? = null

    override val type: Int = R.id.item_isolation_state_radio_group

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemIsolationStateRadioGroupBinding {
        return ItemIsolationStateRadioGroupBinding.inflate(inflater, parent, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun bindView(binding: ItemIsolationStateRadioGroupBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.groupTitle.text = groupTitle
        binding.stateAllGoodRadioButton.text = allGoodLabel.safeEmojiSpanify()
        binding.stateSymptomsRadioButton.text = symptomsLabel.safeEmojiSpanify()
        binding.stateContactRadioButton.text = contactLabel.safeEmojiSpanify()
        binding.statePositiveRadioButton.text = positiveLabel.safeEmojiSpanify()
        binding.stateSymptomsRadioButton.setOnTouchListener(onStateSymptomsClick?.let { onStateSymptomsClick ->
            View.OnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP && !binding.stateSymptomsRadioButton.isChecked) {
                    onStateSymptomsClick.invoke(binding.stateRadioGroup)
                    true
                } else {
                    false
                }
            }
        })

        selectedState?.let {
            when (it) {
                IsolationFormStateEnum.ALL_GOOD -> binding.stateAllGoodRadioButton.isChecked = true
                IsolationFormStateEnum.SYMPTOMS -> binding.stateSymptomsRadioButton.isChecked = true
                IsolationFormStateEnum.CONTACT -> binding.stateContactRadioButton.isChecked = true
                IsolationFormStateEnum.POSITIVE -> binding.statePositiveRadioButton.isChecked = true
            }
        }

        binding.stateRadioGroup.setOnCheckedChangeListener(onStateChangedListener)
    }
}

fun isolationStateRadioGroupItem(block: (IsolationStateRadioGroupItem.() -> Unit)): IsolationStateRadioGroupItem = IsolationStateRadioGroupItem().apply(
    block)
