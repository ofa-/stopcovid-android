/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/05/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class DoubleButtonItem : BaseItem<DoubleButtonItem.ViewHolder>(
    R.layout.item_double_button, ::ViewHolder, R.id.item_double_button
) {
    var text1: String? = null
    var text2: String? = null
    var onClickListener1: View.OnClickListener? = null
    var onClickListener2: View.OnClickListener? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.button1.text = text1.safeEmojiSpanify()
        holder.button1.setOnClickListener(onClickListener1)
        holder.button2.text = text2.safeEmojiSpanify()
        holder.button2.setOnClickListener(onClickListener2)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val button1: MaterialButton = v.findViewById(R.id.button1)
        val button2: MaterialButton = v.findViewById(R.id.button2)
    }
}

fun doubleButtonItem(block: (DoubleButtonItem.() -> Unit)): DoubleButtonItem = DoubleButtonItem().apply(block)