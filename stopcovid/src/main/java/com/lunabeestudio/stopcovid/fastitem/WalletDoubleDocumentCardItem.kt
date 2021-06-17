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
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.extension.setImageFileIfValid
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemWalletDoubleDocumentCardBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import java.io.File

class WalletDoubleDocumentCardItem : AbstractBindingItem<ItemWalletDoubleDocumentCardBinding>() {
    override val type: Int = R.id.item_documents_card

    var mainTitle: String? = null
    var mainBody: String? = null

    var vaccinCertificateCaption: String? = null
    var onVaccinCertificateClick: (() -> Unit)? = null

    var testCertificateCaption: String? = null
    var onTestCertificateClick: (() -> Unit)? = null

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemWalletDoubleDocumentCardBinding {
        return ItemWalletDoubleDocumentCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemWalletDoubleDocumentCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.mainTitleTextView.text = mainTitle
        binding.mainBodyTextView.text = mainBody

        val parentFile = binding.root.context.filesDir

        val vaccinCertificateThumbnail = File(parentFile, ConfigConstant.Wallet.VACCIN_CERTIFICATE_THUMBNAIL_FILE)
        binding.vaccinCertificateImageView.setImageFileIfValid(vaccinCertificateThumbnail)
        binding.vaccinCertificateTextView.setTextOrHide(vaccinCertificateCaption)
        onVaccinCertificateClick?.let { onClick ->
            binding.vaccinCertificateClickableView.setOnClickListener { onClick() }
        }

        val testCertificateThumbnail = File(parentFile, ConfigConstant.Wallet.TEST_CERTIFICATE_THUMBNAIL_FILE)
        binding.testCertificateImageView.setImageFileIfValid(testCertificateThumbnail)
        binding.testCertificateTextView.setTextOrHide(testCertificateCaption)
        onTestCertificateClick?.let { onClick ->
            binding.testCertificateClickableView.setOnClickListener { onClick() }
        }
    }
}

fun walletDoubleDocumentCardItem(block: (WalletDoubleDocumentCardItem.() -> Unit)): WalletDoubleDocumentCardItem =
    WalletDoubleDocumentCardItem().apply(block)