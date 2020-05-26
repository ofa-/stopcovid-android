/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/07/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class IconTitleItem : BaseItem<IconTitleItem.ViewHolder>(
    R.layout.item_icon_title, ::ViewHolder, R.id.item_icon_title
) {
    var text: String? = null

    @DrawableRes
    var iconRes: Int = -1

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.textView.text = text.safeEmojiSpanify()
        if (iconRes != -1) {
            holder.imageView.setImageResource(iconRes)
        } else {
            holder.imageView.setImageBitmap(null)
        }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imageView: ImageView = v.findViewById(R.id.imageView)
        val textView: TextView = v.findViewById(R.id.textView)
    }
}

fun iconTitleItem(block: (IconTitleItem.() -> Unit)): IconTitleItem = IconTitleItem().apply(block)