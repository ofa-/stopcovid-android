/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/19/5 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.view.LayoutInflater
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.setImageFileIfValid
import com.lunabeestudio.stopcovid.databinding.ItemWalletSingleDocumentCardBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import java.io.File

class WalletSingleDocumentCardItem : AbstractBindingItem<ItemWalletSingleDocumentCardBinding>() {
    override val type: Int = R.id.item_documents_card

    var mainTitle: String? = null
    var mainBody: String? = null

    var onClick: (() -> Unit)? = null
    var certificateFile: File? = null

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemWalletSingleDocumentCardBinding {
        return ItemWalletSingleDocumentCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemWalletSingleDocumentCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.mainTitleTextView.text = mainTitle
        binding.mainBodyTextView.text = mainBody
        onClick?.let { onClick -> binding.rootLayout.setOnClickListener { onClick() } }
        certificateFile?.let { binding.certificateImageView.setImageFileIfValid(it) }
    }
}

fun walletSingleDocumentCardItem(block: (WalletSingleDocumentCardItem.() -> Unit)): WalletSingleDocumentCardItem = WalletSingleDocumentCardItem().apply(
    block
)