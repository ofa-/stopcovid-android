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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.databinding.ItemDeleteCardBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class DeleteCardItem : AbstractBindingItem<ItemDeleteCardBinding>() {
    var title: String? = null
    var caption: String? = null
    var onDelete: (() -> Unit)? = null

    override val type: Int = R.id.item_delete_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemDeleteCardBinding {
        return ItemDeleteCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemDeleteCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.titleTextView.isVisible = title != null
        binding.titleTextView.text = title.safeEmojiSpanify()
        binding.captionTextView.isVisible = caption != null
        binding.captionTextView.text = caption.safeEmojiSpanify()
        binding.deleteImageView.isVisible = onDelete != null
        binding.deleteImageView.setOnClickListener {
            onDelete?.invoke()
        }
    }
}

fun deleteCardItem(block: (DeleteCardItem.() -> Unit)): DeleteCardItem = DeleteCardItem().apply(
    block)