/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemInfoCenterDetailCardBinding
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.model.InfoCenterTag
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.binding.BindingViewHolder

class InfoCenterDetailCardItem : AbstractBindingItem<ItemInfoCenterDetailCardBinding>() {
    var header: String? = null
    var tags: List<InfoCenterTag> = emptyList()
    var strings: Map<String, String> = emptyMap()
    var title: String? = null
    var body: String? = null
    var link: String? = null
    var url: String? = null
    var shareContentDescription: String? = null
    var onShareCard: (() -> Unit)? = null

    var tagRecyclerViewPool: RecyclerView.RecycledViewPool? = null

    override val type: Int = R.id.item_info_center_detail_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemInfoCenterDetailCardBinding {
        return ItemInfoCenterDetailCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(holder: BindingViewHolder<ItemInfoCenterDetailCardBinding>, payloads: List<Any>) {
        super.bindView(holder, payloads)

        val tagItems = tags
            .filter { strings[it.labelKey]?.isNotEmpty() ?: false }
            .map { (id, labelKey, colorCode) ->
                tagItem {
                    text = strings[labelKey]
                    color = colorCode
                    identifier = id.hashCode().toLong()
                }
            }

        (holder as InfoCenterDetailCardItemViewHolder).tagAdapter.setNewList(tagItems)
    }

    override fun bindView(binding: ItemInfoCenterDetailCardBinding, payloads: List<Any>) {
        binding.headerTextView.setTextOrHide(header)
        binding.titleTextView.setTextOrHide(title)
        val boldBody = body?.split("\n\n", limit = 2)?.let {
            if (it.size == 2) {
                it[0].length
            } else {
                null
            }
        }?.let { boldTextLength ->
            SpannableString(body).apply {
                setSpan(StyleSpan(Typeface.BOLD), 0, boldTextLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } ?: body
        binding.bodyTextView.setTextOrHide(boldBody)
        binding.includeLink.textView.setTextOrHide(link)
        binding.includeLink.leftIconImageView.isVisible = false
        binding.space.isVisible = link == null || url == null
        binding.includeLink.linkRootLayout.isVisible = link != null && url != null
        binding.includeLink.linkRootLayout.setOnClickListener {
            url?.openInExternalBrowser(it.context)
        }
        binding.tagRecyclerView.isVisible = tags.isNotEmpty()
        binding.shareButton.contentDescription = shareContentDescription
        binding.shareButton.setOnClickListener {
            onShareCard?.invoke()
        }
    }

    override fun getViewHolder(viewBinding: ItemInfoCenterDetailCardBinding): BindingViewHolder<ItemInfoCenterDetailCardBinding> {
        val viewHolder = InfoCenterDetailCardItemViewHolder(viewBinding)

        viewBinding.tagRecyclerView.layoutManager = LinearLayoutManager(
            viewBinding.root.context,
            RecyclerView.HORIZONTAL,
            false
        )
        viewBinding.tagRecyclerView.setHasFixedSize(true)
        viewBinding.tagRecyclerView.adapter = viewHolder.tagAdapter

        tagRecyclerViewPool?.let {
            viewBinding.tagRecyclerView.setRecycledViewPool(it)
        }

        return viewHolder
    }
}

class InfoCenterDetailCardItemViewHolder(binding: ItemInfoCenterDetailCardBinding) : BindingViewHolder<ItemInfoCenterDetailCardBinding>(
    binding
) {
    val tagAdapter: FastItemAdapter<TagItem> = FastItemAdapter()

    init {
        tagAdapter.attachDefaultListeners = false
        tagAdapter.setHasStableIds(true)
    }
}

fun infoCenterDetailCardItem(block: (InfoCenterDetailCardItem.() -> Unit)): InfoCenterDetailCardItem = InfoCenterDetailCardItem().apply(
    block
)
