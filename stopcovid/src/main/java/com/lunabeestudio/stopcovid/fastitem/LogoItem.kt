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

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageSwitcher
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.databinding.ItemLogoBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import kotlin.math.roundToInt

class LogoItem : AbstractBindingItem<ItemLogoBinding>() {
    override val type: Int = R.id.item_logo

    @DrawableRes
    var imageRes: Int? = null
    var isInvisible: Boolean = false

    @ColorInt
    var imageTint: Int? = null

    var onClick: (() -> Unit)? = null

    @DimenRes
    var minLogoHeightRes: Int = R.dimen.min_logo_height

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemLogoBinding {
        return ItemLogoBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemLogoBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        imageRes?.let(binding.imageSwitcher::setImageResource)
        binding.imageSwitcher.isInvisible = isInvisible
        imageTint?.let { tint ->
            binding.imageView1.imageTintList = ColorStateList.valueOf(tint)
            binding.imageView2.imageTintList = ColorStateList.valueOf(tint)
        }
        binding.imageSwitcher.setOnClickListener {
            onClick?.let { it1 -> it1() }
        }
        binding.imageSwitcher.minimumHeight = if (minLogoHeightRes == NO_MINIMUM_HEIGHT) {
            0
        } else {
            minLogoHeightRes.toDimensSize(binding.imageSwitcher.context).roundToInt()
        }
    }

    override fun unbindView(binding: ItemLogoBinding) {
        super.unbindView(binding)
        binding.imageView1.imageTintList = null
        binding.imageView2.imageTintList = null
    }

    companion object {
        const val NO_MINIMUM_HEIGHT: Int = -1
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imageSwitcher: ImageSwitcher = v.findViewById(R.id.imageSwitcher)

        init {
            imageSwitcher.setInAnimation(v.context, R.anim.fade_in)
            imageSwitcher.setOutAnimation(v.context, R.anim.fade_out)
        }
    }
}

fun logoItem(block: (LogoItem.() -> Unit)): LogoItem = LogoItem().apply(block)