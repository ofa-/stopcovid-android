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
import android.widget.Space
import androidx.annotation.DimenRes
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.coreui.R

class SpaceItem : BaseItem<SpaceItem.ViewHolder>(
    R.layout.item_space, SpaceItem::ViewHolder, R.id.item_space
) {
    @DimenRes
    var spaceRes: Int = DEFAULT_HEIGHT
    var orientation: Orientation = Orientation.VERTICAL

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.space.apply {
            if (spaceRes == DEFAULT_HEIGHT) {
                updateLayoutParams {
                    height = 0
                    width = 0
                }
            } else {
                updateLayoutParams {
                    if (orientation == Orientation.VERTICAL) {
                        height = context.resources.getDimensionPixelSize(spaceRes)
                    } else {
                        width = context.resources.getDimensionPixelSize(spaceRes)
                    }
                }
            }
        }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val space: Space = v.findViewById(R.id.space)
    }

    companion object {
        const val DEFAULT_HEIGHT: Int = -1
    }

    enum class Orientation {
        VERTICAL, HORIZONTAL
    }
}

fun spaceItem(block: (SpaceItem.() -> Unit)): SpaceItem = SpaceItem()
    .apply(block)