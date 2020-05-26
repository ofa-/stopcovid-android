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
import android.widget.ImageSwitcher
import androidx.annotation.DrawableRes
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class LogoItem : BaseItem<LogoItem.ViewHolder>(
    R.layout.item_logo, ::ViewHolder, R.id.item_logo
) {
    @DrawableRes
    var imageRes: Int = -1
    var isInvisible: Boolean = false

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.imageSwitcher.setImageResource(imageRes)
        holder.imageSwitcher.isInvisible = isInvisible
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imageSwitcher: ImageSwitcher = v.findViewById(R.id.imageSwitcher)

        init {
            imageSwitcher.setInAnimation(v.context, R.anim.fade_in)
            imageSwitcher.setOutAnimation(v.context, R.anim.fade_out)
        }
    }
}

fun logoItem(block: (LogoItem.() -> Unit)): LogoItem = LogoItem().apply(block)