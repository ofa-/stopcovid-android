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
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureCardBinding
import com.lunabeestudio.stopcovid.extension.setTextOrHide
import com.lunabeestudio.stopcovid.model.Trend
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class KeyFigureCardItem : AbstractBindingItem<ItemKeyFigureCardBinding>() {
    var updatedAt: String? = null
    var leftLocation: String? = null
    var rightLocation: String? = null
    var leftValue: String? = null
    var rightValue: String? = null
    var leftTrend: Trend? = null
    var rightTrend: Trend? = null
    var label: String? = null
    var description: String? = null
    var color: Int? = null
    var shareContentDescription: String? = null
    var onShareCard: ((binding: ItemKeyFigureCardBinding) -> Unit)? = null

    override val type: Int = R.id.item_key_figure_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemKeyFigureCardBinding {
        return ItemKeyFigureCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemKeyFigureCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.updatedAtTextView.setTextOrHide(updatedAt)

        binding.leftLocationTextView.setTextOrHide(leftLocation)
        binding.rightLocationTextView.setTextOrHide(rightLocation)

        leftTrend?.let {
            binding.leftTrend.setImageResource(it.imageRes)
            binding.leftTrend.contentDescription = it.hint
        } ?: binding.leftTrend.setImageDrawable(null)

        rightTrend?.let {
            binding.rightTrend.setImageResource(it.imageRes)
            binding.rightTrend.contentDescription = it.hint
        } ?: binding.rightTrend.setImageDrawable(null)

        binding.leftValueTextView.setTextOrHide(leftValue)
        binding.rightValueTextView.setTextOrHide(rightValue)
        binding.labelTextView.setTextOrHide(label)

        binding.descriptionTextView.setTextOrHide(description)
        color?.let {
            binding.labelTextView.setTextColor(it)
            binding.leftValueTextView.setTextColor(it)
            binding.rightValueTextView.setTextColor(it)
        }

        binding.shareButton.contentDescription = shareContentDescription
        binding.shareButton.setOnClickListener {
            onShareCard?.invoke(binding)
        }
    }

    override fun unbindView(binding: ItemKeyFigureCardBinding) {
        super.unbindView(binding)

        binding.labelTextView.setTextColor(Color.BLACK)
        binding.leftValueTextView.setTextColor(Color.BLACK)
        binding.rightValueTextView.setTextColor(Color.BLACK)
        binding.leftTrend.setImageDrawable(null)
        binding.rightTrend.setImageDrawable(null)
    }
}

fun keyFigureCardItem(block: (KeyFigureCardItem.() -> Unit)): KeyFigureCardItem = KeyFigureCardItem().apply(
    block
)
