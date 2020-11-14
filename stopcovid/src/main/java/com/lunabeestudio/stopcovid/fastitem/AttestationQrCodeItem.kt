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
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemAttestationQrCodeBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class AttestationQrCodeItem : AbstractBindingItem<ItemAttestationQrCodeBinding>() {
    var qrCodeBitmap: Bitmap? = null
    var text: String? = null
    var share: String? = null
    var delete: String? = null
    var allowShare: Boolean = false
    var onShare: (() -> Unit)? = null
    var onDelete: (() -> Unit)? = null
    var onClick: (() -> Unit)? = null
    var actionContentDescription: String? = null

    override val type: Int = R.id.item_attestation_qr_code

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemAttestationQrCodeBinding {
        return ItemAttestationQrCodeBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemAttestationQrCodeBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.textView.text = text
        binding.imageView.setImageBitmap(qrCodeBitmap)
        binding.constraintLayout.setOnClickListener {
            onClick?.invoke()
        }
        binding.actionButton.setOnClickListener {
            showMenu(it)
        }
        binding.actionButton.contentDescription = actionContentDescription
    }

    private fun showMenu(v: View) {
        PopupMenu(v.context, v).apply {
            setOnMenuItemClickListener(::onMenuItemClick)

            inflate(R.menu.qr_code_menu)

            menu.findItem(R.id.qr_code_menu_share).title = share
            menu.findItem(R.id.qr_code_menu_delete).title = delete
            menu.findItem(R.id.qr_code_menu_share).isVisible = allowShare

            show()
        }
    }

    private fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.qr_code_menu_delete -> {
                onDelete?.invoke()
                true
            }
            R.id.qr_code_menu_share -> {
                onShare?.invoke()
                true
            }
            else -> false
        }
    }
}

fun attestationQrCodeItem(block: (AttestationQrCodeItem.() -> Unit)): AttestationQrCodeItem = AttestationQrCodeItem().apply(
    block)