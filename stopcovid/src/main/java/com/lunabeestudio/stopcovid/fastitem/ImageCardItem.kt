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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemImageCardBinding
import com.lunabeestudio.stopcovid.extension.setImageResourceOrHide
import com.lunabeestudio.stopcovid.extension.setTextOrHide
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class ImageCardItem : AbstractBindingItem<ItemImageCardBinding>() {
    var title: String? = null
    var subtitle: String? = null
    var onClickListener: View.OnClickListener? = null

    var contentDescription: String? = null

    @DrawableRes
    var iconRes: Int? = null

    override val type: Int = R.id.item_image_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemImageCardBinding {
        return ItemImageCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemImageCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.titleTextView.setTextOrHide(title)
        binding.subtitleTextView.setTextOrHide(subtitle)
        binding.imageView.setImageResourceOrHide(iconRes)
        binding.root.setOnClickListener(onClickListener)
        binding.root.contentDescription = contentDescription
    }
}

fun imageCardItem(block: (ImageCardItem.() -> Unit)): ImageCardItem = ImageCardItem().apply(block)