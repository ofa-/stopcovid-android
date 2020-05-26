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
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class LogoBodyItem : BaseItem<LogoBodyItem.ViewHolder>(
    R.layout.item_logo_body, ::ViewHolder, R.id.item_logo_body
) {
    @DrawableRes
    var imageRes: Int = -1
    var text: String? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.textView.text = text
        holder.imageView.setImageResource(imageRes)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val textView: TextView = v.findViewById(R.id.textView)
        val imageView: ImageView = v.findViewById(R.id.imageView)
    }
}

fun logoBodyItem(block: (LogoBodyItem.() -> Unit)): LogoBodyItem = LogoBodyItem().apply(block)