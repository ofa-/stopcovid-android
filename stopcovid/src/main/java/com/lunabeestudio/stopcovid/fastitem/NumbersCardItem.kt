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
import android.view.View
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemNumbersCardBinding
import com.lunabeestudio.stopcovid.extension.setTextOrHide
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class NumbersCardItem : AbstractBindingItem<ItemNumbersCardBinding>() {
    var header: String? = null
    var subheader: String? = null
    var label1: String? = null
    var value1: String? = null
    var color1: Int? = null
    var label2: String? = null
    var value2: String? = null
    var color2: Int? = null
    var label3: String? = null
    var value3: String? = null
    var color3: Int? = null
    var link: String? = null
    var onClickListener: View.OnClickListener? = null
    var contentDescription: String? = null

    override val type: Int = R.id.item_numbers_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemNumbersCardBinding {
        return ItemNumbersCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemNumbersCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.headerTextView.setTextOrHide(header)
        binding.subheaderTextView.setTextOrHide(subheader)

        binding.label1TextView.text = label1
        binding.value1TextView.text = value1
        color1?.let { binding.label1TextView.setTextColor(it) }

        binding.label2TextView.text = label2
        binding.value2TextView.text = value2
        color2?.let { binding.label2TextView.setTextColor(it) }

        binding.label3TextView.text = label3
        binding.value3TextView.text = value3
        color3?.let { binding.label3TextView.setTextColor(it) }

        binding.linkTextView.setTextOrHide(link)
        binding.root.setOnClickListener(onClickListener)

        binding.root.contentDescription = contentDescription
    }

    override fun unbindView(binding: ItemNumbersCardBinding) {
        super.unbindView(binding)
        binding.label1TextView.setTextColor(Color.BLACK)
        binding.label2TextView.setTextColor(Color.BLACK)
        binding.label3TextView.setTextColor(Color.BLACK)
    }
}

fun numbersCardItem(block: (NumbersCardItem.() -> Unit)): NumbersCardItem = NumbersCardItem().apply(block)