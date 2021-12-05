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
import android.view.LayoutInflater
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemPrivateVenueQrCodeBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class PrivateVenueQrCodeItem : AbstractBindingItem<ItemPrivateVenueQrCodeBinding>() {
    var qrCodeBitmap: Bitmap? = null
    var text: String? = null

    override val type: Int = R.id.item_attestation_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemPrivateVenueQrCodeBinding {
        return ItemPrivateVenueQrCodeBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemPrivateVenueQrCodeBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.textView.text = text
        binding.imageView.setImageBitmap(qrCodeBitmap)
    }
}

fun privateVenueQrCodeItem(block: (PrivateVenueQrCodeItem.() -> Unit)): PrivateVenueQrCodeItem = PrivateVenueQrCodeItem().apply(
    block
)