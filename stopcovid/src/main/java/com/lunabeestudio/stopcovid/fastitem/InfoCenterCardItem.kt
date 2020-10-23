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
import androidx.core.view.isVisible
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemInfoCenterCardBinding
import com.lunabeestudio.stopcovid.extension.setTextOrHide
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class InfoCenterCardItem : AbstractBindingItem<ItemInfoCenterCardBinding>() {
    var header: String? = null
    var subheader: String? = null
    var title: String? = null
    var subtitle: String? = null
    var link: String? = null
    var showBadge: Boolean = false
    var onClickListener: View.OnClickListener? = null
    var contentDescription: String? = null

    override val type: Int = R.id.item_info_center_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemInfoCenterCardBinding {
        return ItemInfoCenterCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemInfoCenterCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.headerTextView.setTextOrHide(header)
        binding.subheaderTextView.setTextOrHide(subheader)
        binding.titleTextView.setTextOrHide(title)
        binding.subtitleTextView.setTextOrHide(subtitle)
        binding.linkTextView.setTextOrHide(link)
        binding.badgeView.isVisible = showBadge
        binding.root.setOnClickListener(onClickListener)
        binding.root.contentDescription = contentDescription
    }
}

fun infoCenterCardItem(block: (InfoCenterCardItem.() -> Unit)): InfoCenterCardItem = InfoCenterCardItem().apply(block)