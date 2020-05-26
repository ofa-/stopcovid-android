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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class DoubleTextItem : BaseItem<DoubleTextItem.ViewHolder>(
    R.layout.item_double_text, ::ViewHolder, R.id.item_double_text
) {
    var title: String? = null
    var caption: String? = null
    var onClickListener: View.OnClickListener? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.titleTextView.text = title.safeEmojiSpanify()
        holder.captionTextView.text = caption.safeEmojiSpanify()
        holder.itemView.setOnClickListener(onClickListener)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val titleTextView: TextView = v.findViewById(R.id.titleTextView)
        val captionTextView: TextView = v.findViewById(R.id.captionTextView)
    }
}

fun doubleTextItem(block: (DoubleTextItem.() -> Unit)): DoubleTextItem = DoubleTextItem().apply(block)