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
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.lunabeestudio.stopcovid.coreui.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify

open class ButtonItem(@LayoutRes layout: Int = R.layout.item_button,
    vh: (v: View) -> ViewHolder = ButtonItem::ViewHolder,
    @IdRes id: Int = R.id.item_button)
    : BaseItem<ButtonItem.ViewHolder>(layout, vh, id) {
    var text: String? = null
    var gravity: Int = Gravity.NO_GRAVITY
    var isButtonEnabled: Boolean = true
    var onClickListener: View.OnClickListener? = null
    var width: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    var getMaterialButton: ((MaterialButton) -> Unit)? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.button.text = text.safeEmojiSpanify()
        holder.button.setOnClickListener(onClickListener)
        holder.button.isEnabled = isButtonEnabled
        holder.button.updateLayoutParams<FrameLayout.LayoutParams> {
            this.gravity = this@ButtonItem.gravity
            this.width = this@ButtonItem.width
        }
        getMaterialButton?.invoke(holder.button)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val button: MaterialButton = v.findViewById(R.id.button)
    }
}

fun buttonItem(block: (ButtonItem.() -> Unit)): ButtonItem = ButtonItem()
    .apply(block)