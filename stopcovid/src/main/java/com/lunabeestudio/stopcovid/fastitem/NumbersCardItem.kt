/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemNumbersCardBinding
import com.lunabeestudio.stopcovid.extension.setTextOrHide
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class NumbersCardItem : AbstractBindingItem<ItemNumbersCardBinding>() {
    var header: String? = null
    var subheader: String? = null
    var localData: Data? = null
    var franceData: Data? = null
    var link: String? = null
    var onClickListener: View.OnClickListener? = null
    var contentDescription: String? = null

    override val type: Int = R.id.item_numbers_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemNumbersCardBinding {
        return ItemNumbersCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemNumbersCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.headerTextView.setTextOrHide(header)
        binding.subheaderTextView.setTextOrHide(subheader)

        binding.localInclude.root.isVisible = localData != null
        localData?.let { (localization, dataFigure1, dataFigure2, dataFigure3) ->
            binding.localInclude.locationTextView.setTextOrHide(localization)

            binding.localInclude.label1TextView.setTextOrHide(dataFigure1?.label)
            binding.localInclude.value1TextView.text = dataFigure1?.value ?: "-"
            dataFigure1?.color?.let { binding.localInclude.label1TextView.setTextColor(it) }

            binding.localInclude.label2TextView.setTextOrHide(dataFigure2?.label)
            binding.localInclude.value2TextView.text = dataFigure2?.value ?: "-"
            dataFigure2?.color?.let { binding.localInclude.label2TextView.setTextColor(it) }

            binding.localInclude.label3TextView.setTextOrHide(dataFigure3?.label)
            binding.localInclude.value3TextView.text = dataFigure3?.value ?: "-"
            dataFigure3?.color?.let { binding.localInclude.label3TextView.setTextColor(it) }
        }

        franceData?.let { (localization, dataFigure1, dataFigure2, dataFigure3) ->
            if (localData == null) {
                binding.franceInclude.locationTextView.isVisible = false
            } else {
                binding.franceInclude.locationTextView.setTextOrHide(localization)
            }

            binding.franceInclude.label1TextView.setTextOrHide(dataFigure1?.label)
            binding.franceInclude.value1TextView.text = dataFigure1?.value ?: "-"
            dataFigure1?.color?.let { binding.franceInclude.label1TextView.setTextColor(it) }

            binding.franceInclude.label2TextView.setTextOrHide(dataFigure2?.label)
            binding.franceInclude.value2TextView.text = dataFigure2?.value ?: "-"
            dataFigure2?.color?.let { binding.franceInclude.label2TextView.setTextColor(it) }

            binding.franceInclude.label3TextView.setTextOrHide(dataFigure3?.label)
            binding.franceInclude.value3TextView.text = dataFigure3?.value ?: "-"
            dataFigure3?.color?.let { binding.franceInclude.label3TextView.setTextColor(it) }
        }

        binding.linkTextView.setTextOrHide(link)
        binding.root.setOnClickListener(onClickListener)

        binding.root.contentDescription = contentDescription
    }

    override fun unbindView(binding: ItemNumbersCardBinding) {
        super.unbindView(binding)
        binding.localInclude.root.isVisible = false
        binding.localInclude.label1TextView.setTextColor(Color.BLACK)
        binding.localInclude.label2TextView.setTextColor(Color.BLACK)
        binding.localInclude.label3TextView.setTextColor(Color.BLACK)

        binding.franceInclude.label1TextView.setTextColor(Color.BLACK)
        binding.franceInclude.label2TextView.setTextColor(Color.BLACK)
        binding.franceInclude.label3TextView.setTextColor(Color.BLACK)
    }

    data class Data(
        var localization: String?,
        var dataFigure1: DataFigure?,
        var dataFigure2: DataFigure?,
        var dataFigure3: DataFigure?,
    )

    data class DataFigure(
        var label: String? = null,
        var value: String? = null,
        var color: Int? = null
    )
}

fun numbersCardItem(block: (NumbersCardItem.() -> Unit)): NumbersCardItem = NumbersCardItem().apply(block)