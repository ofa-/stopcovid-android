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

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColor
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem
import timber.log.Timber

class TagItem : BaseItem<TagItem.ViewHolder>(
    R.layout.item_tag, ::ViewHolder, R.id.item_tag
) {
    var text: String? = null
    var color: String? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.chip.text = text
        holder.chip.chipBackgroundColor = ColorStateList.valueOf(
            try {
                Timber.d(color)
                Color.parseColor(color)
            } catch (e: Exception) {
                Timber.e(e)
                R.attr.colorPrimary.fetchSystemColor(holder.itemView.context)
            }
        )
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val chip: Chip = v.findViewById(R.id.chip)
    }
}

fun tagItem(block: (TagItem.() -> Unit)): TagItem = TagItem().apply(block)
