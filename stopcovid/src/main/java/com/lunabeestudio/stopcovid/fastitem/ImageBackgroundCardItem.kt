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

import android.util.LayoutDirection
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColor
import com.lunabeestudio.stopcovid.databinding.ItemImageBackgroundCardBinding
import com.lunabeestudio.stopcovid.extension.setImageResourceOrHide
import com.lunabeestudio.stopcovid.extension.setTextOrHide
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class ImageBackgroundCardItem : AbstractBindingItem<ItemImageBackgroundCardBinding>() {
    var header: String? = null
    var title: String? = null
    var subtitle: String? = null
    var layoutDirection: Int = LayoutDirection.INHERIT
    var onClickListener: View.OnClickListener? = null

    @ColorInt
    var textColor: Int? = null

    @DrawableRes
    var backgroundDrawable: Int? = null

    @DrawableRes
    var iconRes: Int? = null

    var contentDescription: String? = null

    override val type: Int = R.id.item_image_background_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemImageBackgroundCardBinding {
        return ItemImageBackgroundCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemImageBackgroundCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        val textColor = textColor ?: if (backgroundDrawable == null) {
            R.attr.colorOnSurface.fetchSystemColor(binding.root.context)
        } else {
            ContextCompat.getColor(binding.root.context, R.color.color_on_gradient)
        }

        binding.headerTextView.setTextOrHide(header)
        binding.headerTextView.setTextColor(textColor)
        binding.titleTextView.setTextOrHide(title)
        binding.titleTextView.setTextColor(textColor)
        binding.subtitleTextView.setTextOrHide(subtitle)
        binding.subtitleTextView.setTextColor(textColor)
        binding.imageView.setImageResourceOrHide(iconRes)
        backgroundDrawable?.let { binding.mainLayout.setBackgroundResource(it) }
        binding.mainLayout.layoutDirection = layoutDirection
        binding.root.setOnClickListener(onClickListener)
        binding.root.contentDescription = contentDescription
    }

    override fun unbindView(binding: ItemImageBackgroundCardBinding) {
        super.unbindView(binding)
        binding.mainLayout.background = null
        binding.mainLayout.layoutDirection = LayoutDirection.INHERIT
    }
}

fun imageBackgroundCardItem(block: (ImageBackgroundCardItem.() -> Unit)): ImageBackgroundCardItem = ImageBackgroundCardItem().apply(block)