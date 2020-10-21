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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemButtonCardBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class ButtonCardItem : AbstractBindingItem<ItemButtonCardBinding>() {
    var text: String? = null
    var buttonText: String? = null
    var onClickListener: View.OnClickListener? = null

    override val type: Int = R.id.item_button_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemButtonCardBinding {
        return ItemButtonCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemButtonCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.textView.text = text
        binding.button.text = buttonText
        binding.button.setOnClickListener(onClickListener)
    }
}

fun buttonCardItem(block: (ButtonCardItem.() -> Unit)): ButtonCardItem = ButtonCardItem().apply(block)