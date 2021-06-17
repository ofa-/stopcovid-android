/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/29 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.databinding.ItemLinkCardBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class LinkCardItem : AbstractBindingItem<ItemLinkCardBinding>() {
    var label: String? = null

    @DrawableRes
    var iconRes: Int? = null

    var onClickListener: View.OnClickListener? = null

    override val type: Int = R.id.item_link_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemLinkCardBinding {
        return ItemLinkCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemLinkCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.link.textView.text = label.safeEmojiSpanify()
        iconRes?.let(binding.link.leftIconImageView::setImageResource)
        binding.link.linkRootLayout.setOnClickListener(onClickListener)
        binding.link.arrowImageView.isVisible = false
    }
}

fun linkCardItem(block: (LinkCardItem.() -> Unit)): LinkCardItem = LinkCardItem().apply(
    block
)