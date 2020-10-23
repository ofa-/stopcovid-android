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
import androidx.annotation.DrawableRes
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemImageBackgroundCardBinding
import com.lunabeestudio.stopcovid.extension.setImageResourceOrHide
import com.lunabeestudio.stopcovid.extension.setTextOrHide
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class ImageBackgroundCardItem : AbstractBindingItem<ItemImageBackgroundCardBinding>() {
    var header: String? = null
    var title: String? = null
    var subtitle: String? = null
    var onClickListener: View.OnClickListener? = null

    @DrawableRes
    var backgroundDrawable: Int? = null

    @DrawableRes
    var iconRes: Int? = null

    override val type: Int = R.id.item_image_background_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemImageBackgroundCardBinding {
        return ItemImageBackgroundCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemImageBackgroundCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.headerTextView.setTextOrHide(header)
        binding.titleTextView.setTextOrHide(title)
        binding.subtitleTextView.setTextOrHide(subtitle)
        binding.imageView.setImageResourceOrHide(iconRes)
        backgroundDrawable?.let { binding.constraintLayout.setBackgroundResource(it) }
        binding.root.setOnClickListener(onClickListener)
    }

    override fun unbindView(binding: ItemImageBackgroundCardBinding) {
        super.unbindView(binding)
        binding.constraintLayout.background = null
    }
}

fun imageBackgroundCardItem(block: (ImageBackgroundCardItem.() -> Unit)): ImageBackgroundCardItem = ImageBackgroundCardItem().apply(block)