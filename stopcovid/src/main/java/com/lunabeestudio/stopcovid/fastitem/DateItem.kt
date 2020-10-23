/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class DateItem : BaseItem<DateItem.ViewHolder>(
    R.layout.item_date, DateItem::ViewHolder, R.id.item_date
) {
    var text: String? = null
    var gravity: Int = Gravity.NO_GRAVITY

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.textView.text = text.safeEmojiSpanify()
        holder.textView.gravity = gravity
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val textView: TextView = v.findViewById(R.id.textView)
    }
}

fun dateItem(block: (DateItem.() -> Unit)): DateItem = DateItem().apply(block)