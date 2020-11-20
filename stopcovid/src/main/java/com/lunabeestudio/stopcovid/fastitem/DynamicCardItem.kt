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
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.databinding.ItemDynamicCardBinding
import com.lunabeestudio.stopcovid.databinding.ItemLinkBinding
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.model.Link
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class DynamicCardItem : AbstractBindingItem<ItemDynamicCardBinding>() {
    var title: String? = null
    var caption: String? = null
    var links: List<Link>? = null

    override val type: Int = R.id.item_dynamic_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemDynamicCardBinding {
        return ItemDynamicCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemDynamicCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.titleTextView.isVisible = title != null
        binding.titleTextView.text = title.safeEmojiSpanify()
        binding.captionTextView.isVisible = caption != null
        binding.captionTextView.text = caption.safeEmojiSpanify()
        binding.space.isVisible = links.isNullOrEmpty()
        binding.linksLinearLayout.removeAllViews()
        links?.forEach { link ->
            val linkBinding = ItemLinkBinding.inflate(LayoutInflater.from(binding.linksLinearLayout.context),
                binding.linksLinearLayout,
                true)
            linkBinding.textView.text = link.label.safeEmojiSpanify()
            linkBinding.leftIconImageView.isVisible = false
            linkBinding.linkRootLayout.setOnClickListener {
                link.url.openInExternalBrowser(it.context)
            }
        }
    }
}

fun dynamicCardItem(block: (DynamicCardItem.() -> Unit)): DynamicCardItem = DynamicCardItem().apply(
    block)