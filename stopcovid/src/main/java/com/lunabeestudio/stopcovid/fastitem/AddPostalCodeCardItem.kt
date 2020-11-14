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
import android.view.View
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemAddPostalCodeCardBinding
import com.lunabeestudio.stopcovid.extension.setTextOrHide
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class AddPostalCodeCardItem : AbstractBindingItem<ItemAddPostalCodeCardBinding>() {
    var header: String? = null
    var content: String? = null
    var link: String? = null
    var onClickListener: View.OnClickListener? = null
    var contentDescription: String? = null

    override val type: Int = R.id.item_add_postal_code_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemAddPostalCodeCardBinding {
        return ItemAddPostalCodeCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemAddPostalCodeCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.headerTextView.setTextOrHide(header)
        binding.contentTextView.setTextOrHide(content)
        binding.linkTextView.setTextOrHide(link)
        binding.root.setOnClickListener(onClickListener)
        binding.root.contentDescription = contentDescription
    }
}

fun addPostalCodeCardItem(block: (AddPostalCodeCardItem.() -> Unit)): AddPostalCodeCardItem = AddPostalCodeCardItem().apply(block)