/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/15/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.view.View
import android.view.ViewGroup
import android.widget.ViewSwitcher
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class ProximityButtonItem : BaseItem<ProximityButtonItem.ViewHolder>(
    R.layout.item_proximity_button,
    ::ViewHolder,
    R.id.item_proximity_button) {
    var mainText: String? = null
    var lightText: String? = null
    var showMainButton: Boolean = true
    var isButtonEnabled: Boolean = true
    var onClickListener: View.OnClickListener? = null
    var width: Int = ViewGroup.LayoutParams.WRAP_CONTENT

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.mainButton.text = mainText.safeEmojiSpanify()
        holder.lightButton.text = lightText.safeEmojiSpanify()
        holder.mainButton.setOnClickListener(onClickListener)
        holder.lightButton.setOnClickListener(onClickListener)
        holder.mainButton.isEnabled = isButtonEnabled
        holder.lightButton.isEnabled = isButtonEnabled
        if (showMainButton) {
            if (holder.viewSwitcher.currentView != holder.mainButton) {
                holder.viewSwitcher.showNext()
            }
        } else {
            if (holder.viewSwitcher.currentView != holder.lightButton) {
                holder.viewSwitcher.showNext()
            }
        }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val mainButton: MaterialButton = v.findViewById(R.id.mainButton)
        val lightButton: MaterialButton = v.findViewById(R.id.lightButton)
        val viewSwitcher: ViewSwitcher = v.findViewById(R.id.viewSwitcher)

        init {
            viewSwitcher.setInAnimation(v.context, R.anim.fade_in)
            viewSwitcher.setOutAnimation(v.context, R.anim.fade_out)
        }
    }
}

fun proximityButtonItem(block: (ProximityButtonItem.() -> Unit)): ProximityButtonItem = ProximityButtonItem()
    .apply(block)