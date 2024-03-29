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

import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.coreui.R
import com.lunabeestudio.stopcovid.coreui.extension.addRipple
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify

class CaptionItem : BaseItem<CaptionItem.ViewHolder>(
    R.layout.item_caption, CaptionItem::ViewHolder, R.id.item_caption
) {
    var text: CharSequence? = null
    var spannedText: CharSequence? = null
    var gravity: Int = Gravity.NO_GRAVITY
    var onClick: (() -> Unit) = {}
    var onLongClick: (() -> Unit) = {}
    var ripple = false

    @StyleRes
    var textAppearance: Int = R.style.TextAppearance_StopCovid_Caption

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.textView.text = spannedText ?: text.safeEmojiSpanify()
        holder.textView.gravity = gravity
        holder.itemView.run {
            setOnClickListener { onClick() }
            setOnLongClickListener { onLongClick(); true }
            if (ripple) addRipple()
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            holder.textView.setTextAppearance(textAppearance)
        } else {
            @Suppress("DEPRECATION")
            holder.textView.setTextAppearance(holder.textView.context, textAppearance)
        }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val textView: TextView = v.findViewById(R.id.textView)
    }
}

fun captionItem(block: (CaptionItem.() -> Unit)): CaptionItem = CaptionItem()
    .apply(block)
