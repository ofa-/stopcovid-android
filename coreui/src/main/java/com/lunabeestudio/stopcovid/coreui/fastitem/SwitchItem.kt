/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.fastitem

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.lunabeestudio.stopcovid.coreui.R

class SwitchItem : BaseItem<SwitchItem.ViewHolder>(
    R.layout.item_switch, ::ViewHolder, R.id.item_switch
) {
    var title: String? = null
    var isChecked: Boolean = false
    var onCheckChange: ((Boolean) -> Unit)? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.titleTextView.text = title
        holder.leftSwitch.isChecked = isChecked
        holder.leftSwitch.contentDescription = title
        onCheckChange?.let { onChange ->
            holder.leftSwitch.setOnCheckedChangeListener { _, isChecked ->
                onChange(isChecked)
            }
        }
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.leftSwitch.setOnCheckedChangeListener(null)
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val titleTextView: TextView = v.findViewById(R.id.titleTextView)
        val leftSwitch: SwitchMaterial = v.findViewById(R.id.leftSwitch)
    }
}

fun switchItem(block: (SwitchItem.() -> Unit)): SwitchItem = SwitchItem().apply(block)