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
import androidx.core.view.isVisible
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.databinding.ItemMoreCardBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class MoreCardItem : AbstractBindingItem<ItemMoreCardBinding>() {
    var manageDataText: String? = null
    var manageDataOnClickListener: View.OnClickListener? = null
    var privacyText: String? = null
    var privacyOnClickListener: View.OnClickListener? = null
    var aboutText: String? = null
    var aboutOnClickListener: View.OnClickListener? = null
    var linksText: String? = null
    var linksOnClickListener: View.OnClickListener? = null

    @DrawableRes
    var linksIconRes: Int? = null

    @DrawableRes
    var manageDataIconRes: Int? = null

    @DrawableRes
    var privacyIconRes: Int? = null

    @DrawableRes
    var aboutIconRes: Int? = null

    override val type: Int = R.id.item_more_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemMoreCardBinding {
        return ItemMoreCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemMoreCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.links.textView.text = linksText.safeEmojiSpanify()
        linksIconRes?.let(binding.links.leftIconImageView::setImageResource)
        binding.links.linkRootLayout.setOnClickListener(linksOnClickListener)
        binding.links.linkRootLayout.isVisible = !linksText.isNullOrEmpty()
        binding.links.arrowImageView.isVisible = false

        binding.managerData.textView.text = manageDataText.safeEmojiSpanify()
        manageDataIconRes?.let(binding.managerData.leftIconImageView::setImageResource)
        binding.managerData.linkRootLayout.setOnClickListener(manageDataOnClickListener)
        binding.managerData.linkRootLayout.isVisible = !manageDataText.isNullOrEmpty()
        binding.managerData.arrowImageView.isVisible = false

        binding.privacy.textView.text = privacyText.safeEmojiSpanify()
        privacyIconRes?.let(binding.privacy.leftIconImageView::setImageResource)
        binding.privacy.linkRootLayout.setOnClickListener(privacyOnClickListener)
        binding.privacy.linkRootLayout.isVisible = !privacyText.isNullOrEmpty()
        binding.privacy.arrowImageView.isVisible = false

        binding.about.textView.text = aboutText.safeEmojiSpanify()
        aboutIconRes?.let(binding.about.leftIconImageView::setImageResource)
        binding.about.linkRootLayout.setOnClickListener(aboutOnClickListener)
        binding.about.linkRootLayout.isVisible = !aboutText.isNullOrEmpty()
        binding.about.arrowImageView.isVisible = false
    }
}

fun moreCardItem(block: (MoreCardItem.() -> Unit)): MoreCardItem = MoreCardItem().apply(block)
