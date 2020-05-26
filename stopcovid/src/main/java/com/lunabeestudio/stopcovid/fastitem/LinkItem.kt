/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.boldSubstringForSearch
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.extension.transformForSearch
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem
import com.lunabeestudio.stopcovid.extension.openInChromeTab

class LinkItem : BaseItem<LinkItem.ViewHolder>(
    R.layout.item_link, ::ViewHolder, R.id.item_link
) {
    var text: String? = null
    var searchString: String? = null
    var url: String? = null
    var onClickListener: View.OnClickListener? = null

    @DrawableRes
    var iconRes: Int? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.textView.text = if (searchString.isNullOrBlank()) {
            text.safeEmojiSpanify()
        } else {
            text?.boldSubstringForSearch(
                text?.transformForSearch() ?: "",
                searchString ?: ""
            )
        }
        holder.constraintLayout.setOnClickListener {
            url?.openInChromeTab(it.context)
            onClickListener?.onClick(it)
        }

        iconRes?.let { holder.leftIconImageView.setImageResource(it) }
        holder.leftIconImageView.isVisible = iconRes != null
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val constraintLayout: ConstraintLayout = v.findViewById(R.id.constraintLayout)
        val textView: TextView = v.findViewById(R.id.textView)
        val leftIconImageView: ImageView = v.findViewById(R.id.leftIconImageView)
    }
}

fun linkItem(block: (LinkItem.() -> Unit)): LinkItem = LinkItem().apply(block)