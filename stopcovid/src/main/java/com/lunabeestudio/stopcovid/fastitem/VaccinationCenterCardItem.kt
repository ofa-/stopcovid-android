package com.lunabeestudio.stopcovid.fastitem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemVaccinationCenterCardBinding
import com.lunabeestudio.stopcovid.extension.setTextOrHide
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class VaccinationCenterCardItem : AbstractBindingItem<ItemVaccinationCenterCardBinding>() {
    var title: String? = null
    var modality: String? = null
    var address: String? = null
    var openingDateHeader: String? = null
    var openingDate: String? = null
    var openingTime: String? = null
    var onClickListener: View.OnClickListener? = null

    override val type: Int = R.id.item_vaccination_center_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemVaccinationCenterCardBinding {
        return ItemVaccinationCenterCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemVaccinationCenterCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.titleTextView.setTextOrHide(title)
        binding.modalityTextView.setTextOrHide(modality)
        binding.addressTextView.setTextOrHide(address)
        binding.openingDateHeaderTextView.setTextOrHide(openingDateHeader)
        binding.openingDateTextView.setTextOrHide(openingDate)
        binding.openingTimeTextView.setTextOrHide(openingTime)
        binding.root.setOnClickListener(onClickListener)
    }
}

fun vaccinationCenterCardItem(block: (VaccinationCenterCardItem.() -> Unit)): VaccinationCenterCardItem = VaccinationCenterCardItem().apply(
    block
)
