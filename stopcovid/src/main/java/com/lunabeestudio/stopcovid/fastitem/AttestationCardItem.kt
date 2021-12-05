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
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemAttestationCardBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class AttestationCardItem : AbstractBindingItem<ItemAttestationCardBinding>() {
    var generateBarcode: (() -> Bitmap)? = null
    var mainDescription: String? = null
    var share: String? = null
    var delete: String? = null
    var allowShare: Boolean = false
    var onShare: ((barcodeBitmap: Bitmap?) -> Unit)? = null
    var onDelete: (() -> Unit)? = null
    var onClick: (() -> Unit)? = null
    var actionContentDescription: String? = null
    var bottomText: String? = null

    override val type: Int = R.id.item_attestation_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemAttestationCardBinding {
        return ItemAttestationCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemAttestationCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.mainDescriptionTextView.setTextOrHide(mainDescription)

        val bitmap = generateBarcode?.invoke()
        binding.imageView.setImageBitmap(bitmap)

        binding.constraintLayout.setOnClickListener {
            onClick?.invoke()
        }
        binding.actionButton.setOnClickListener {
            showMenu(it, bitmap)
        }

        binding.actionButton.contentDescription = actionContentDescription

        binding.bottomActionTextView.setTextOrHide(bottomText)
    }

    override fun unbindView(binding: ItemAttestationCardBinding) {
        super.unbindView(binding)
        binding.imageView.setImageBitmap(null)
    }

    private fun showMenu(v: View, bitmap: Bitmap?) {
        PopupMenu(v.context, v).apply {
            setOnMenuItemClickListener { onMenuItemClick(it, bitmap) }

            inflate(R.menu.attestation_menu)

            menu.findItem(R.id.attestation_menu_share).title = share
            menu.findItem(R.id.attestation_menu_share).isVisible = allowShare

            menu.findItem(R.id.attestation_menu_delete).title = delete

            show()
        }
    }

    private fun onMenuItemClick(item: MenuItem, bitmap: Bitmap?): Boolean {
        return when (item.itemId) {
            R.id.attestation_menu_delete -> {
                onDelete?.invoke()
                true
            }
            R.id.attestation_menu_share -> {
                onShare?.invoke(bitmap)
                true
            }
            else -> false
        }
    }
}

fun attestationCardItem(block: (AttestationCardItem.() -> Unit)): AttestationCardItem = AttestationCardItem().apply(
    block
)
