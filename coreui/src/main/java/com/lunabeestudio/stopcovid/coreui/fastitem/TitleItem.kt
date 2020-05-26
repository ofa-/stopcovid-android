/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.fastitem

import android.view.Gravity
import android.view.View
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.coreui.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify

class TitleItem : BaseItem<TitleItem.ViewHolder>(
    R.layout.item_title, TitleItem::ViewHolder, R.id.item_title
) {
    var text: String? = null
    var gravity: Int = Gravity.NO_GRAVITY

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.textSwitcher.setText(text.safeEmojiSpanify())
        holder.textView1.gravity = gravity
        holder.textView2.gravity = gravity
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.textSwitcher.setText(null)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val textSwitcher: TextSwitcher = v.findViewById(R.id.textSwitcher)
        val textView1: TextView = v.findViewById(R.id.textView1)
        val textView2: TextView = v.findViewById(R.id.textView2)

        init {
            textSwitcher.setInAnimation(v.context, R.anim.fade_in)
            textSwitcher.setOutAnimation(v.context, R.anim.fade_out)
        }
    }
}

fun titleItem(block: (TitleItem.() -> Unit)): TitleItem = TitleItem()
    .apply(block)