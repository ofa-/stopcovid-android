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
import android.view.View
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class BigTitleItem : BaseItem<BigTitleItem.ViewHolder>(
    R.layout.item_big_title, ::ViewHolder, R.id.item_big_title
) {
    var text: String? = null
    var gravity: Int = Gravity.NO_GRAVITY
    var importantForAccessibility: Int = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES
    var linkText: String? = null
    var onClickLink: View.OnClickListener? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.itemView.importantForAccessibility = importantForAccessibility
        holder.textView.text = text.safeEmojiSpanify()
        holder.textView.gravity = gravity
        holder.linkTextView.setTextOrHide(linkText)
        holder.linkTextView.setOnClickListener(onClickLink)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val textView: TextView = v.findViewById(R.id.textView)
        val linkTextView: TextView = v.findViewById(R.id.endTextView)
    }
}

fun bigTitleItem(block: (BigTitleItem.() -> Unit)): BigTitleItem = BigTitleItem().apply(block)