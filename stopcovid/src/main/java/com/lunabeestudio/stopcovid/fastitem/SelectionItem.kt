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
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColor
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemSelectionBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class SelectionItem : AbstractBindingItem<ItemSelectionBinding>() {
    var title: String? = null
    var caption: String? = null
    var showSelection: Boolean = false
    var onClick: (() -> Unit)? = null
    var maxLineCaption: Int? = null

    @DrawableRes
    var iconSelectionOff: Int? = null

    @DrawableRes
    var iconSelectionOn: Int = R.drawable.ic_check_on

    @ColorInt
    var iconTint: Int? = null

    @ColorInt
    var textColor: Int? = null

    override val type: Int = R.id.item_selection

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemSelectionBinding {
        return ItemSelectionBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemSelectionBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.titleTextView.setTextOrHide(title)
        binding.captionTextView.setTextOrHide(caption)
        val clickListener = onClick?.let { onClick ->
            View.OnClickListener {
                onClick()
            }
        }
        binding.titleTextView.isEnabled = this.isEnabled
        binding.captionTextView.isEnabled = this.isEnabled
        binding.selectionImageView.isEnabled = this.isEnabled
        binding.selectionRootLayout.setOnClickListener(clickListener)
        binding.selectionRootLayout.isClickable = clickListener != null

        // max line on Caption
        maxLineCaption?.let {
            binding.captionTextView.maxLines = it
            binding.captionTextView.ellipsize = TextUtils.TruncateAt.END
        }

        setupIcon(binding)

        iconTint?.let { color ->
            binding.selectionImageView.imageTintList = ColorStateList.valueOf(color)
        }

        textColor?.let { color ->
            binding.titleTextView.setTextColor(color)
            binding.captionTextView.setTextColor(color)
        }
    }

    private fun setupIcon(binding: ItemSelectionBinding) {
        val iconSelectionOff = this.iconSelectionOff
        when {
            showSelection -> {
                binding.selectionImageView.isVisible = true
                binding.selectionImageView.setImageResource(iconSelectionOn)
            }
            iconSelectionOff != null -> {
                binding.selectionImageView.isVisible = true
                binding.selectionImageView.setImageResource(iconSelectionOff)
            }
            else -> {
                binding.selectionImageView.isInvisible = true
                binding.selectionImageView.setImageResource(iconSelectionOn)
            }
        }
    }

    override fun unbindView(binding: ItemSelectionBinding) {
        super.unbindView(binding)
        binding.apply {
            selectionImageView.setImageDrawable(null)
            captionTextView.maxLines = Integer.MAX_VALUE
            captionTextView.ellipsize = null
            selectionRootLayout.setOnClickListener(null)
            selectionImageView.imageTintList = ColorStateList.valueOf(
                com.lunabeestudio.stopcovid.coreui.R.attr.colorAccent.fetchSystemColor(
                    root.context
                )
            )
            val colorDefaultText = com.lunabeestudio.stopcovid.coreui.R.attr.colorOnSurface.fetchSystemColor(root.context)
            titleTextView.setTextColor(colorDefaultText)
            captionTextView.setTextColor(colorDefaultText)
        }
    }
}

fun selectionItem(block: (SelectionItem.() -> Unit)): SelectionItem = SelectionItem().apply(block)