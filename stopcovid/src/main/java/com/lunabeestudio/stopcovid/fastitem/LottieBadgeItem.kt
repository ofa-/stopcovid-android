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

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RawRes
import androidx.core.view.updateLayoutParams
import com.airbnb.lottie.LottieCompositionFactory
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemLottieBadgeBinding
import com.lunabeestudio.stopcovid.databinding.NestedItemLottieBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class LottieBadgeItem(@RawRes private val rawRes: Int) : AbstractBindingItem<ItemLottieBadgeBinding>() {
    override val type: Int = rawRes

    var chipText: String? = null
    var chipClick: (() -> Unit)? = null
    var forceBottomGravity: Boolean = false

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemLottieBadgeBinding {
        val itemLottieBinding = ItemLottieBadgeBinding.inflate(inflater, parent, false)
        val nestedItemLottieBadgeBinding = NestedItemLottieBinding.bind(itemLottieBinding.root)
        LottieCompositionFactory.fromRawRes(inflater.context, rawRes).addListener { composition ->
            nestedItemLottieBadgeBinding.lottieAnimationView.setComposition(composition)
        }

        if (forceBottomGravity) {
            nestedItemLottieBadgeBinding.lottieAnimationView.updateLayoutParams<FrameLayout.LayoutParams> {
                gravity = Gravity.BOTTOM
            }
        }
        return itemLottieBinding
    }

    override fun bindView(binding: ItemLottieBadgeBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        binding.timeChip.setTextOrHide(chipText)
        val clickListener = chipClick?.let {
            View.OnClickListener { it() }
        }
        binding.timeChip.setOnClickListener(clickListener)
    }
}

fun lottieBadgeItem(@RawRes rawRes: Int, block: (LottieBadgeItem.() -> Unit)): LottieBadgeItem = LottieBadgeItem(rawRes).apply(block)