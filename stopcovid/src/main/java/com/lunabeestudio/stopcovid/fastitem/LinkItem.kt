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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser

class LinkItem : BaseItem<LinkItem.ViewHolder>(
    R.layout.item_link, ::ViewHolder, R.id.item_link
) {
    var text: String? = null
    var gravity: Int = Gravity.CENTER_VERTICAL
    var url: String? = null
    var onClickListener: View.OnClickListener? = null
    var forceShowArrow: Boolean = false

    @DrawableRes
    var iconRes: Int? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.textView.text = text.safeEmojiSpanify()
        holder.rootLayout.gravity = gravity
        holder.rootLayout.setOnClickListener {
            url?.openInExternalBrowser(it.context)
            onClickListener?.onClick(it)
        }

        iconRes?.let { holder.leftIconImageView.setImageResource(it) }
        holder.leftIconImageView.isVisible = iconRes != null
        holder.arrowImageView.isVisible = forceShowArrow || url != null
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val rootLayout: LinearLayout = v.findViewById(R.id.linkRootLayout)
        val textView: TextView = v.findViewById(R.id.textView)
        val leftIconImageView: ImageView = v.findViewById(R.id.leftIconImageView)
        val arrowImageView: ImageView = v.findViewById(R.id.arrowImageView)
    }
}

fun linkItem(block: (LinkItem.() -> Unit)): LinkItem = LinkItem().apply(block)