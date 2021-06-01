/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/25/5 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.setImageFileIfValid
import com.lunabeestudio.stopcovid.databinding.ItemAddCertificateCardBinding
import com.lunabeestudio.stopcovid.extension.setTextOrHide
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import java.io.File

class AddCertificateCardItem : AbstractBindingItem<ItemAddCertificateCardBinding>() {
    override val type: Int = R.id.item_add_certificate_card

    var title: String? = null
    var subtitle: String? = null
    var imageFile: File? = null

    @DrawableRes
    var fallbackRes: Int? = null

    var mainAction: (() -> Unit)? = null
    var thumbnailAction: (() -> Unit)? = null

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemAddCertificateCardBinding {
        return ItemAddCertificateCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemAddCertificateCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.titleTextView.setTextOrHide(title)
        binding.subtitleTextView.setTextOrHide(subtitle)

        if (imageFile?.let { binding.certificatedImageView.setImageFileIfValid(it) } != true) {
            fallbackRes?.let { binding.certificatedImageView.setImageResource(it) }
        }
        binding.certificatedImageView.setOnClickListener {
            thumbnailAction?.invoke()
        }

        binding.constraintLayout.setOnClickListener {
            mainAction?.invoke()
        }
    }
}

fun addCertificateCardItem(block: (AddCertificateCardItem.() -> Unit)): AddCertificateCardItem = AddCertificateCardItem().apply(block)
