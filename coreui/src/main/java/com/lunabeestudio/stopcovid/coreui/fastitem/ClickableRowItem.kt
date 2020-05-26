/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/15/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.fastitem

import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.coreui.R
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColorStateList

class ClickableRowItem : BaseItem<ClickableRowItem.ViewHolder>(
    R.layout.item_clickable_row, ClickableRowItem::ViewHolder, R.id.item_clickable_row
) {
    var title: String? = null
    var caption: String? = null
    var onClick: (() -> Unit)? = null

    @ColorInt
    var titleColorInt: Int? = null

    @DrawableRes
    var titleIconRes: Int? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.titleTextView.text = title
        titleColorInt?.let {
            holder.titleTextView.setTextColor(it)
        } ?: holder.titleTextView.setTextColor(android.R.attr.textColorPrimary.fetchSystemColorStateList(holder.itemView.context))
        holder.titleTextView.isVisible = title != null
        holder.titleTextView.setCompoundDrawablesWithIntrinsicBounds(titleIconRes ?: 0, 0, 0, 0)

        holder.captionTextView.text = caption
        holder.captionTextView.isVisible = caption != null

        holder.itemView.setOnClickListener(onClick?.let {
            View.OnClickListener {
                it()
            }
        })
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val titleTextView: TextView = v.findViewById(R.id.titleTextView)
        val captionTextView: TextView = v.findViewById(R.id.captionTextView)
    }
}

fun clickableRowItem(block: (ClickableRowItem.() -> Unit)): ClickableRowItem = ClickableRowItem()
    .apply(block)