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
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColor
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColorStateList
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemCertificateCardBinding
import com.lunabeestudio.stopcovid.databinding.ItemTagBinding
import com.lunabeestudio.stopcovid.model.CertificateHeaderState
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class CertificateCardItem : AbstractBindingItem<ItemCertificateCardBinding>() {
    var generateBarcode: (() -> Bitmap)? = null
    var headerText: String? = null
    var headerState: CertificateHeaderState? = null
    var titleText: String? = null
    var nameText: String? = null
    var descriptionText: String? = null
    var tag1Text: String? = null
    var tag2Text: String? = null
    var share: String? = null
    var delete: String? = null
    var convertText: String? = null
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
    var readMoreText: String? = null
    var onReadMoreClick: (() -> Unit)? = null

    @ColorRes
    var tag1ColorRes: Int? = null

    @ColorRes
    var tag2ColorRes: Int? = null

    @AttrRes
    private val tagDefaultColor = R.attr.colorPrimary

    override val type: Int = R.id.item_certificate_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemCertificateCardBinding {
        return ItemCertificateCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemCertificateCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        val bitmap = generateBarcode?.invoke()

        setupHeader(binding)
        setupTitle(binding)
        setupActions(binding, bitmap)
        setupQRCode(binding, bitmap)
        setupName(binding)
        setupInfos(binding)
        setupTags(binding)
        setupBottomAction(binding)

        binding.certificateContentLayout.setOnClickListener {
            onClick?.invoke()
        }
    }

    private fun setupHeader(binding: ItemCertificateCardBinding) {
        val headerTextSpan = headerText?.toSpannable()?.also {
            Linkify.addLinks(it, Linkify.WEB_URLS)
        }

        binding.headerMessageTextView.setTextOrHide(headerTextSpan)
        headerState?.let { headerState ->
            binding.headerMessageLayout.setBackgroundColor(
                ContextCompat.getColor(
                    binding.root.context,
                    headerState.backgroundColor,
                )
            )
            val textColor = ContextCompat.getColor(binding.root.context, headerState.textColor)
            binding.headerMessageTextView.setTextColor(textColor)
            binding.readMoreTextView.setTextColor(textColor)
            binding.topMessageImageView.setImageResource(headerState.icon)
        }
        binding.headerMessageLayout.isGone = headerTextSpan.isNullOrBlank()

        binding.headerMessageTextView.post {
            val isEllipsized = (binding.headerMessageTextView.layout?.getEllipsisCount(1) ?: 0) > 0
            if (isEllipsized && onReadMoreClick != null) {
                binding.readMoreTextView.setTextOrHide(readMoreText)
                binding.headerMessageLayout.requestLayout()
                binding.headerMessageLayout.setOnClickListener {
                    onReadMoreClick?.invoke()
                }
            } else {
                if (!headerTextSpan?.getSpans(0, headerTextSpan.length, URLSpan::class.java).isNullOrEmpty()) {
                    binding.headerMessageTextView.movementMethod = LinkMovementMethod.getInstance()
                } else {
                    binding.headerMessageTextView.movementMethod = null
                }

                binding.readMoreTextView.isVisible = false
                binding.headerMessageLayout.setOnClickListener(null)
                binding.headerMessageLayout.isClickable = false
            }
        }
    }

    private fun setupTitle(binding: ItemCertificateCardBinding) {
        binding.titleTextView.setTextOrHide(titleText)
    }

    private fun setupActions(binding: ItemCertificateCardBinding, bitmap: Bitmap?) {
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

        binding.actionButton.setOnClickListener {
            showMenu(it, bitmap)
        }
    }

    private fun setupQRCode(binding: ItemCertificateCardBinding, bitmap: Bitmap?) {
        binding.imageView.setImageBitmap(bitmap)
    }

    private fun setupName(binding: ItemCertificateCardBinding) {
        binding.nameTextView.setTextOrHide(nameText)
    }

    private fun setupInfos(binding: ItemCertificateCardBinding) {
        binding.descriptionTextView.setTextOrHide(descriptionText)
    }

    private fun setupTags(binding: ItemCertificateCardBinding) {
        setupTag(binding.tag1, tag1Text, tag1ColorRes, onTag1Click)
        setupTag(binding.tag2, tag2Text, tag2ColorRes, onTag2Click)
        binding.tagLayout.isVisible = !(tag1Text.isNullOrEmpty() && tag2Text.isNullOrEmpty())
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

    private fun setupBottomAction(binding: ItemCertificateCardBinding) {
        binding.bottomActionTextView.setTextOrHide(bottomText)
    }

    override fun unbindView(binding: ItemCertificateCardBinding) {
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

            menu.findItem(R.id.qr_code_menu_delete).title = delete

            menu.findItem(R.id.qr_code_menu_convert).title = convertText
            menu.findItem(R.id.qr_code_menu_convert).isVisible = onConvert != null

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
            R.id.qr_code_menu_convert -> {
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

fun certificateCardItem(block: (CertificateCardItem.() -> Unit)): CertificateCardItem = CertificateCardItem().apply(
    block
)
