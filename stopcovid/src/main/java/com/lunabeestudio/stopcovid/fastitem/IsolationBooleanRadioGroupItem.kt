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

import android.view.LayoutInflater
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemIsolationBooleanRadioGroupBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class IsolationBooleanRadioGroupItem : AbstractBindingItem<ItemIsolationBooleanRadioGroupBinding>() {

    var groupTitle: String? = null
    var yesLabel: String? = null
    var noLabel: String? = null
    var selectedState: Boolean? = null
    var onStateChangedListener: ((Boolean?) -> Unit)? = null

    override val type: Int = R.id.item_isolation_boolean_radio_group

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemIsolationBooleanRadioGroupBinding {
        return ItemIsolationBooleanRadioGroupBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemIsolationBooleanRadioGroupBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.groupTitle.text = groupTitle
        binding.yesRadioButton.text = yesLabel
        binding.noRadioButton.text = noLabel

        if (selectedState == true) {
            binding.yesRadioButton.isChecked = true
        } else if (selectedState == false) {
            binding.noRadioButton.isChecked = true
        }

        onStateChangedListener?.let {
            binding.stateRadioGroup.setOnCheckedChangeListener { _, id ->
                onStateChangedListener?.invoke(
                    when (id) {
                        R.id.yesRadioButton -> true
                        R.id.noRadioButton -> false
                        else -> null
                    }
                )
            }
        }
    }

    override fun unbindView(binding: ItemIsolationBooleanRadioGroupBinding) {
        super.unbindView(binding)
        binding.stateRadioGroup.setOnCheckedChangeListener(null)
        binding.stateRadioGroup.clearCheck()
    }
}

fun isolationBooleanRadioGroupItem(block: (IsolationBooleanRadioGroupItem.() -> Unit)): IsolationBooleanRadioGroupItem =
    IsolationBooleanRadioGroupItem().apply(block)
