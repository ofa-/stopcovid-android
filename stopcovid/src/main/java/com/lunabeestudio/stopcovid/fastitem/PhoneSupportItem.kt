/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/20/5 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.view.LayoutInflater
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemPhoneSupportBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class PhoneSupportItem() : AbstractBindingItem<ItemPhoneSupportBinding>() {
    override val type: Int = R.id.item_phone_support

    var title: String? = null
    var subtitle: String? = null

    var onClick: (() -> Unit)? = null

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemPhoneSupportBinding {
        return ItemPhoneSupportBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemPhoneSupportBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.titleTextView.setTextOrHide(title)
        binding.subtitleTextView.setTextOrHide(subtitle)

        onClick?.let { onClick ->
            binding.rootLayout.setOnClickListener { onClick() }
        }
    }
}

fun phoneSupportItem(block: (PhoneSupportItem.() -> Unit)): PhoneSupportItem = PhoneSupportItem().apply(block)