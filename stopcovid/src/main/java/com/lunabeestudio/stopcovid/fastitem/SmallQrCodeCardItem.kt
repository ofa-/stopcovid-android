package com.lunabeestudio.stopcovid.fastitem

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemSmallQrCodeCardBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class SmallQrCodeCardItem : AbstractBindingItem<ItemSmallQrCodeCardBinding>() {

    override val type: Int = R.id.item_small_qr_code_card_item

    var generateBarcode: (() -> Bitmap)? = null
    var title: String? = null
    var body: String? = null
    var onClick: (() -> Unit)? = null

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemSmallQrCodeCardBinding {
        return ItemSmallQrCodeCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemSmallQrCodeCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.titleTextView.setTextOrHide(title)
        binding.bodyTextView.setTextOrHide(body)

        val bitmap = generateBarcode?.invoke()
        binding.qrCodeImageView.setImageBitmap(bitmap)

        binding.container.setOnClickListener {
            onClick?.invoke()
        }
    }
}

fun smallQrCodeCardItem(block: (SmallQrCodeCardItem.() -> Unit)): SmallQrCodeCardItem = SmallQrCodeCardItem().apply(block)