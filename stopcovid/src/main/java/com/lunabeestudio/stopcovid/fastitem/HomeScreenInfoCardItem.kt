package com.lunabeestudio.stopcovid.fastitem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemHomeScreenInfoCardBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class HomeScreenInfoCardItem : AbstractBindingItem<ItemHomeScreenInfoCardBinding>() {
    override val type: Int = R.id.item_home_screen_info_card

    var captionText : String? = null
    var titleText : String? = null
    var subtitleText : String? = null
    var onClick: View.OnClickListener? = null

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemHomeScreenInfoCardBinding {
        return ItemHomeScreenInfoCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemHomeScreenInfoCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.apply {
            captionTextView.setTextOrHide(captionText)
            titleTextView.setTextOrHide(titleText)
            subtitleTextView.setTextOrHide(subtitleText)
            constraintLayout.setOnClickListener(onClick)
        }
    }

    override fun unbindView(binding: ItemHomeScreenInfoCardBinding) {
        super.unbindView(binding)
        binding.apply {
            constraintLayout.setOnClickListener(null)
            captionTextView.text = null
            titleTextView.text = null
            subtitleTextView.text = null
        }
    }
}

fun homeScreeInfoCardItem(block: (HomeScreenInfoCardItem.() -> Unit)): HomeScreenInfoCardItem = HomeScreenInfoCardItem().apply(block)