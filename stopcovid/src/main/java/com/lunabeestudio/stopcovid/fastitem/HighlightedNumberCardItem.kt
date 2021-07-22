package com.lunabeestudio.stopcovid.fastitem

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemHighlightedNumberCardBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class HighlightedNumberCardItem : AbstractBindingItem<ItemHighlightedNumberCardBinding>() {
    var updatedAt: String? = null
    var value: String? = null
    var label: String? = null
    var color: Int? = null
    var onClickListener: View.OnClickListener? = null

    override val type: Int = R.id.item_highlighted_number_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemHighlightedNumberCardBinding {
        return ItemHighlightedNumberCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemHighlightedNumberCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.headerTextView.setTextOrHide(label)
        binding.subheaderTextView.setTextOrHide(updatedAt)
        binding.figureTextView.setTextOrHide(value)

        color?.let { color ->
            binding.headerTextView.setTextColor(color)
            binding.headerTextView.compoundDrawablesRelative.forEach {
                it?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        }

        binding.root.setOnClickListener(onClickListener)
    }

    override fun unbindView(binding: ItemHighlightedNumberCardBinding) {
        super.unbindView(binding)
        binding.headerTextView.setTextColor(Color.BLACK)
    }
}

fun highlightedNumberCardItem(block: (HighlightedNumberCardItem.() -> Unit)): HighlightedNumberCardItem = HighlightedNumberCardItem().apply(
    block
)
