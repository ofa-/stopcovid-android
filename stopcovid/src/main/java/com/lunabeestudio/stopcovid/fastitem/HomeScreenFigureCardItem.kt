package com.lunabeestudio.stopcovid.fastitem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColor
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemHomeScreenFigureCardBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class HomeScreenFigureCardItem : AbstractBindingItem<ItemHomeScreenFigureCardBinding>() {
    override val type: Int = R.id.item_home_screen_figure_card

    var regionText: String? = null
    var valueText: String? = null
    var figureText: String? = null

    var onClick: View.OnClickListener? = null

    @ColorRes
    var colorBackground: Int? = null

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemHomeScreenFigureCardBinding {
        return ItemHomeScreenFigureCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemHomeScreenFigureCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.apply {
            root.setOnClickListener(onClick)

            colorBackground?.let {
                binding.constraintLayout.setBackgroundColor(it)
            }
            regionTextView.setTextOrHide(regionText)
            figureValueTextView.setTextOrHide(valueText)
            bottomActionTextView.setTextOrHide(figureText)
        }
    }

    override fun unbindView(binding: ItemHomeScreenFigureCardBinding) {
        super.unbindView(binding)
        binding.apply {
            root.setOnClickListener(null)
            regionTextView.text = null
            figureValueTextView.text = null
            bottomActionTextView.text = null

            val defaultColor = R.attr.colorPrimary.fetchSystemColor(root.context)
            binding.constraintLayout.setBackgroundColor(defaultColor)
        }
    }
}

fun homeScreeFigureCardItem(
    block: (HomeScreenFigureCardItem.() -> Unit)
): HomeScreenFigureCardItem = HomeScreenFigureCardItem().apply(block)