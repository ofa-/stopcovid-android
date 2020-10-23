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
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemInfoCenterDetailCardBinding
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.setTextOrHide
import com.lunabeestudio.stopcovid.model.InfoCenterTag
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class InfoCenterDetailCardItem : AbstractBindingItem<ItemInfoCenterDetailCardBinding>() {
    var header: String? = null
    var tags: List<InfoCenterTag> = emptyList()
    var strings: Map<String, String> = emptyMap()
    var title: String? = null
    var subtitle: String? = null
    var link: String? = null
    var url: String? = null

    private val fastItemAdapter: FastItemAdapter<GenericItem> = FastItemAdapter()

    override val type: Int = R.id.item_info_center_detail_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemInfoCenterDetailCardBinding {
        return ItemInfoCenterDetailCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemInfoCenterDetailCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.headerTextView.setTextOrHide(header)
        binding.titleTextView.setTextOrHide(title)
        binding.subtitleTextView.setTextOrHide(subtitle)
        binding.includeLink.textView.setTextOrHide(link)
        binding.includeLink.leftIconImageView.isVisible = false
        binding.space.isVisible = link == null || url == null
        binding.includeLink.constraintLayout.isVisible = link != null && url != null
        binding.includeLink.constraintLayout.setOnClickListener {
            url?.openInExternalBrowser(it.context)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(
            binding.root.context,
            RecyclerView.HORIZONTAL,
            false
        )
        binding.recyclerView.adapter = fastItemAdapter
        fastItemAdapter.set(
            tags.map { tag ->
                tagItem {
                    text = strings[tag.labelKey]
                    color = tag.colorCode
                    identifier = tag.id.hashCode().toLong()
                }
            }
        )
        binding.recyclerView.isVisible = tags.isNotEmpty()
    }

}

fun infoCenterDetailCardItem(block: (InfoCenterDetailCardItem.() -> Unit)): InfoCenterDetailCardItem = InfoCenterDetailCardItem().apply(
    block
)