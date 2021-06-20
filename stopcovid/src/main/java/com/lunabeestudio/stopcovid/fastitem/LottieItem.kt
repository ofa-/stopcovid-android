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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RawRes
import com.airbnb.lottie.LottieCompositionFactory
import com.lunabeestudio.stopcovid.databinding.ItemLottieBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class LottieItem(@RawRes private val rawRes: Int) : AbstractBindingItem<ItemLottieBinding>() {
    override val type: Int = rawRes

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemLottieBinding {
        val itemLottieBinding = ItemLottieBinding.inflate(inflater, parent, false)
        LottieCompositionFactory.fromRawRes(inflater.context, rawRes).addListener { composition ->
            itemLottieBinding.lottieAnimationView.setComposition(composition)
        }
        return itemLottieBinding
    }
}

fun lottieItem(@RawRes rawRes: Int, block: (LottieItem.() -> Unit)): LottieItem = LottieItem(rawRes).apply(block)