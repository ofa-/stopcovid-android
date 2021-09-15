/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/2/9 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.view.LayoutInflater
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemExplanationActionCardBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class ExplanationActionCardItem : AbstractBindingItem<ItemExplanationActionCardBinding>() {
    override val type: Int = R.id.item_caption_action_card

    var explanation: String? = null
    var onClick: (() -> Unit)? = null
    var bottomText: String? = null

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemExplanationActionCardBinding {
        return ItemExplanationActionCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemExplanationActionCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.explanationTextView.setTextOrHide(explanation)
        binding.root.setOnClickListener {
            onClick?.invoke()
        }
        binding.bottomActionTextView.setTextOrHide(bottomText)
    }

    override fun unbindView(binding: ItemExplanationActionCardBinding) {
        super.unbindView(binding)
        binding.root.setOnClickListener(null)
    }
}

fun explanationActionCardItem(block: ExplanationActionCardItem.() -> Unit): ExplanationActionCardItem = ExplanationActionCardItem().apply(
    block
)