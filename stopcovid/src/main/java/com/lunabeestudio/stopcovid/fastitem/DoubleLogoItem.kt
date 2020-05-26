/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class DoubleLogoItem : BaseItem<DoubleLogoItem.ViewHolder>(
    R.layout.item_double_logo, ::ViewHolder, R.id.item_double_logo
) {
    @DrawableRes
    var leftImageRes: Int = -1

    @DrawableRes
    var rightImageRes: Int = -1

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.leftImageView.setImageResource(leftImageRes)
        holder.rightImageView.setImageResource(rightImageRes)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val leftImageView: ImageView = v.findViewById(R.id.leftImageView)
        val rightImageView: ImageView = v.findViewById(R.id.rightImageView)
    }
}

fun doubleLogoItem(block: (DoubleLogoItem.() -> Unit)): DoubleLogoItem = DoubleLogoItem().apply(block)