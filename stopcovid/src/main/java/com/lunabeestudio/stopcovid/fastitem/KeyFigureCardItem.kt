/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureCardBinding
import com.lunabeestudio.stopcovid.extension.setTextOrHide
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class KeyFigureCardItem : AbstractBindingItem<ItemKeyFigureCardBinding>() {
    var updatedAt: String? = null
    var value: String? = null
    var label: String? = null
    var description: String? = null
    var color: Int? = null

    override val type: Int = R.id.item_key_figure_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemKeyFigureCardBinding {
        return ItemKeyFigureCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemKeyFigureCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.updatedAtTextView.setTextOrHide(updatedAt)
        binding.valueTextView.setTextOrHide(value)
        binding.labelTextView.setTextOrHide(label)
        binding.descriptionTextView.setTextOrHide(description)
        color?.let {
            binding.labelTextView.setTextColor(it)
            binding.valueTextView.setTextColor(it)
        }
    }

    override fun unbindView(binding: ItemKeyFigureCardBinding) {
        super.unbindView(binding)

        binding.labelTextView.setTextColor(Color.BLACK)
        binding.valueTextView.setTextColor(Color.BLACK)
    }
}

fun keyFigureCardItem(block: (KeyFigureCardItem.() -> Unit)): KeyFigureCardItem = KeyFigureCardItem().apply(
    block
)
