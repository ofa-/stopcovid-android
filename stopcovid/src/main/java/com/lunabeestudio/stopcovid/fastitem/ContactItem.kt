/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/07/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem
import com.lunabeestudio.stopcovid.extension.setTextOrHide

class ContactItem(layoutRes: Int) : BaseItem<ContactItem.ViewHolder>(
    layoutRes, ::ViewHolder, R.id.item_contact + layoutRes
) {
    var header: String? = null
    var title: String? = null
    var caption: String? = null
    var more: String? = null
    var atRisk: Boolean? = null
    var moreClickListener: View.OnClickListener? = null
    var actionClickListener: View.OnClickListener? = null
    var actionContentDescription: String? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.headerTextView.setTextOrHide(header.safeEmojiSpanify())
        holder.titleTextView.setTextOrHide(title.safeEmojiSpanify())
        holder.captionTextView.setTextOrHide(caption.safeEmojiSpanify())
        holder.moreButton.setTextOrHide(more.safeEmojiSpanify())
        holder.moreButton.setOnClickListener(moreClickListener)
        holder.actionButton.setOnClickListener(actionClickListener)
        holder.actionButton.contentDescription = actionContentDescription
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val headerTextView: TextView = v.findViewById(R.id.headerTextView)
        val titleTextView: TextView = v.findViewById(R.id.titleTextView)
        val captionTextView: TextView = v.findViewById(R.id.captionTextView)
        val moreButton: MaterialButton = v.findViewById(R.id.learnMoreButton)
        val actionButton: ImageButton = v.findViewById(R.id.actionButton)
    }
}

fun contactItem(layoutRes: Int, block: (ContactItem.() -> Unit)): ContactItem = ContactItem(layoutRes).apply(block)