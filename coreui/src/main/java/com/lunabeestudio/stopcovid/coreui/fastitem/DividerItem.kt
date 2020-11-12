/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.fastitem

import android.view.View
import androidx.annotation.DimenRes
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.coreui.R
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize

class DividerItem : BaseItem<DividerItem.ViewHolder>(
    R.layout.item_divider, DividerItem::ViewHolder, R.id.item_divider
) {
    @DimenRes
    var marginStartRes: Int? = R.dimen.spacing_large

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.itemView.updateLayoutParams<RecyclerView.LayoutParams> {
            marginStart = marginStartRes?.toDimensSize(holder.itemView.context)?.toInt() ?: 0
        }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
}

fun dividerItem(block: (DividerItem.() -> Unit)): DividerItem = DividerItem()
    .apply(block)