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
import androidx.annotation.DrawableRes
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.setImageResourceOrHide
import com.lunabeestudio.stopcovid.databinding.ItemChangePostalCodeBinding
import com.lunabeestudio.stopcovid.extension.setTextOrHide
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class ChangePostalCodeItem : AbstractBindingItem<ItemChangePostalCodeBinding>() {
    var label: String? = null
    var endLabel: String? = null

    @DrawableRes
    var iconRes: Int? = null

    var onClickListener: View.OnClickListener? = null

    override val type: Int = R.id.item_change_postal_code

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemChangePostalCodeBinding {
        return ItemChangePostalCodeBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemChangePostalCodeBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.textView.setTextOrHide(label)
        binding.endTextView.setTextOrHide(endLabel)
        binding.leftIconImageView.setImageResourceOrHide(iconRes)
        binding.rootLayout.setOnClickListener(onClickListener)
    }
}

fun changePostalCodeItem(block: (ChangePostalCodeItem.() -> Unit)): ChangePostalCodeItem = ChangePostalCodeItem().apply(
    block
)