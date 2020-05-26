/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/12/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.fastitem

import android.annotation.SuppressLint
import android.view.View
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.items.AbstractItem

/**
 * Kotlin implementation of the [AbstractItem] to make things shorter
 *
 * If only one iitem type extends the given [layoutRes], you may use it as the type and not worry about another id.
 *
 * You extend it like so
 *
 * ```Kotlin
 * class ExampleItem : BaseItem<ExampleItem.ViewHolder>(
 *    R.layout.ex_iitem_card, ::ViewHolder, R.id.ex_item_card
 * ){
 *    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
 *       super.bindView(holder, payloads)
 *    }
 *
 *    override fun unbindView(holder: ViewHolder) {
 *       super.unbindView(holder)
 *    }
 *
 *    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)
 * }
 * ```
 *
 * @param layoutRes The layout ResId of this item.
 * @param viewHolder The ViewHolder lambda is typically of the
 * form ::ViewHolder Where you will have a nested class ViewHolder(v: View) : RecyclerView.override fun getViewHolder(v: View)
 * @param type Optional -> If a layout is only used for one item, it may also be used as the id, which you may leave blank in this case.
 */
open class BaseItem<VH : RecyclerView.ViewHolder>(
    @param:LayoutRes final override val layoutRes: Int,
    private val viewHolder: (v: View) -> VH,
    final override val type: Int = layoutRes
) : AbstractItem<VH>() {
    @SuppressLint("ResourceType")
    final override fun getViewHolder(v: View): VH = viewHolder(v)
}