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

import android.graphics.Bitmap
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColor
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColorStateList
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemQrCodeCardBinding
import com.lunabeestudio.stopcovid.databinding.ItemTagBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class QrCodeCardItem : AbstractBindingItem<ItemQrCodeCardBinding>() {
    var generateBarcode: (() -> Bitmap)? = null
    var mainDescription: String? = null
    var footerDescription: Spannable? = null
    var tag1Text: String? = null
    var tag2Text: String? = null
    var formatText: String? = null
    var share: String? = null
    var delete: String? = null
    var convertText: String? = null
    var allowShare: Boolean = false
    var onShare: ((barcodeBitmap: Bitmap?) -> Unit)? = null
    var onDelete: (() -> Unit)? = null
    var onConvert: (() -> Unit)? = null
    var onClick: (() -> Unit)? = null
    var actionContentDescription: String? = null
    var onTag1Click: (() -> Unit)? = null
    var onTag2Click: (() -> Unit)? = null
    var favoriteContentDescription: String? = null
    var favoriteState: FavoriteState = FavoriteState.HIDDEN
    var onFavoriteClick: (() -> Unit)? = null
    var bottomText: String? = null

    @ColorRes
    var tag1ColorRes: Int? = null

    @ColorRes
    var tag2ColorRes: Int? = null

    @AttrRes
    private val tagDefaultColor = R.attr.colorPrimary

    override val type: Int = R.id.item_attestation_qr_code

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemQrCodeCardBinding {
        return ItemQrCodeCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemQrCodeCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.mainDescriptionTextView.setTextOrHide(mainDescription)
        binding.footerDescriptionTextView.setTextOrHide(footerDescription)
        footerDescription?.let { spannable ->
            if (spannable.nextSpanTransition(0, spannable.length, URLSpan::class.java) != spannable.length) {
                binding.footerDescriptionTextView.movementMethod = LinkMovementMethod.getInstance()
            } else {
                binding.footerDescriptionTextView.movementMethod = null
            }
        }

        val bitmap = generateBarcode?.invoke()
        binding.imageView.setImageBitmap(bitmap)

        binding.constraintLayout.setOnClickListener {
            onClick?.invoke()
        }
        binding.actionButton.setOnClickListener {
            showMenu(it, bitmap)
        }
        binding.formatTextView.setTextOrHide(formatText)

        setupTag(binding.tag1, tag1Text, tag1ColorRes, onTag1Click)
        setupTag(binding.tag2, tag2Text, tag2ColorRes, onTag2Click)

        binding.tagLayout.isVisible = !(tag1Text.isNullOrEmpty() && tag2Text.isNullOrEmpty())

        binding.actionButton.contentDescription = actionContentDescription

        binding.favoriteButton.isVisible = favoriteState != FavoriteState.HIDDEN
        binding.favoriteButton.contentDescription = favoriteContentDescription
        binding.favoriteButton.setOnClickListener {
            onFavoriteClick?.invoke()
        }
        when (favoriteState) {
            FavoriteState.HIDDEN -> {
                /* no-op */
            }
            FavoriteState.NOT_CHECKED -> binding.favoriteButton.setImageResource(R.drawable.ic_empty_heart)
            FavoriteState.CHECKED -> binding.favoriteButton.setImageResource(R.drawable.ic_filled_heart)
        }

        binding.bottomActionTextView.setTextOrHide(bottomText)
    }

    private fun setupTag(tagBinding: ItemTagBinding, tagText: String?, tagColorRes: Int?, onTagClick: (() -> Unit)?) {
        tagBinding.chip.text = tagText
        tagBinding.chip.setTextColor(R.attr.colorOnPrimary.fetchSystemColor(tagBinding.root.context))
        tagBinding.chip.isVisible = !tagText.isNullOrEmpty()

        if (tagColorRes != null) {
            tagBinding.chip.setChipBackgroundColorResource(tagColorRes)
        } else {
            tagBinding.chip.chipBackgroundColor = tagDefaultColor.fetchSystemColorStateList(tagBinding.root.context)
        }

        tagColorRes?.let { tagBinding.chip.setChipBackgroundColorResource(it) }
        onTagClick?.let { onClick ->
            tagBinding.chip.isEnabled = true
            tagBinding.chip.setOnClickListener {
                onClick()
            }
        }
    }

    override fun unbindView(binding: ItemQrCodeCardBinding) {
        super.unbindView(binding)
        binding.imageView.setImageBitmap(null)
        binding.tag1.chip.setOnClickListener(null)
        binding.tag1.chip.isEnabled = false
        binding.tag2.chip.setOnClickListener(null)
        binding.tag2.chip.isEnabled = false
    }

    private fun showMenu(v: View, bitmap: Bitmap?) {
        PopupMenu(v.context, v).apply {
            setOnMenuItemClickListener { onMenuItemClick(it, bitmap) }

            inflate(R.menu.qr_code_menu)

            menu.findItem(R.id.qr_code_menu_share).title = share
            menu.findItem(R.id.qr_code_menu_share).isVisible = allowShare

            menu.findItem(R.id.qr_code_menu_delete).title = delete

            menu.findItem(R.id.qr_code_menu_covert).title = convertText
            menu.findItem(R.id.qr_code_menu_covert).isVisible = onConvert != null

            show()
        }
    }

    private fun onMenuItemClick(item: MenuItem, bitmap: Bitmap?): Boolean {
        return when (item.itemId) {
            R.id.qr_code_menu_delete -> {
                onDelete?.invoke()
                true
            }
            R.id.qr_code_menu_share -> {
                onShare?.invoke(bitmap)
                true
            }
            R.id.qr_code_menu_covert -> {
                onConvert?.invoke()
                true
            }
            else -> false
        }
    }

    enum class FavoriteState {
        HIDDEN, NOT_CHECKED, CHECKED
    }
}

fun qrCodeCardItem(block: (QrCodeCardItem.() -> Unit)): QrCodeCardItem = QrCodeCardItem().apply(
    block
)
