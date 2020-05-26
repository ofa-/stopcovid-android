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

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class DangerButtonItem : BaseItem<DangerButtonItem.ViewHolder>(
    R.layout.item_danger_button, ::ViewHolder, R.id.item_danger_button
) {
    var text: String? = null
    var gravity: Int = Gravity.NO_GRAVITY
    var onClickListener: View.OnClickListener? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.button.text = text.safeEmojiSpanify()
        holder.button.setOnClickListener(onClickListener)
        holder.button.updateLayoutParams<FrameLayout.LayoutParams> {
            this.gravity = this@DangerButtonItem.gravity
        }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val button: MaterialButton = v.findViewById(R.id.button)
    }
}

fun dangerButtonItem(block: (DangerButtonItem.() -> Unit)): DangerButtonItem = DangerButtonItem().apply(block)