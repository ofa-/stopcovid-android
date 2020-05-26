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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class ContactItem : BaseItem<ContactItem.ViewHolder>(
    R.layout.item_contact, ::ViewHolder, R.id.item_contact
) {
    var header: String? = null
    var title: String? = null
    var caption: String? = null
    var more: String? = null
    var moreClickListener: View.OnClickListener? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.headerTextView.text = header.safeEmojiSpanify()
        holder.titleTextView.text = title.safeEmojiSpanify()
        holder.captionTextView.text = caption.safeEmojiSpanify()
        holder.moreTextView.text = more.safeEmojiSpanify()
        holder.moreConstraintLayout.setOnClickListener(moreClickListener)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val headerTextView: TextView = v.findViewById(R.id.headerTextView)
        val titleTextView: TextView = v.findViewById(R.id.titleTextView)
        val captionTextView: TextView = v.findViewById(R.id.captionTextView)
        val moreConstraintLayout: ConstraintLayout = v.findViewById(R.id.linkLayout)
        val moreTextView: TextView = v.findViewById(R.id.textView)
    }
}

fun contactItem(block: (ContactItem.() -> Unit)): ContactItem = ContactItem().apply(block)