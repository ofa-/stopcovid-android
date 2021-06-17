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
import android.view.ViewGroup
import androidx.core.view.isInvisible
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.databinding.ItemSelectionBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class SelectionItem : AbstractBindingItem<ItemSelectionBinding>() {
    var title: String? = null
    var caption: String? = null
    var showSelection: Boolean = false
    var onClick: (() -> Unit)? = null

    override val type: Int = R.id.item_selection

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemSelectionBinding {
        return ItemSelectionBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemSelectionBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.titleTextView.text = title.safeEmojiSpanify()
        binding.captionTextView.text = caption.safeEmojiSpanify()
        binding.selectionRootLayout.setOnClickListener {
            onClick?.invoke()
        }
        binding.selectionImageView.isInvisible = !showSelection
    }
}

fun selectionItem(block: (SelectionItem.() -> Unit)): SelectionItem = SelectionItem().apply(block)