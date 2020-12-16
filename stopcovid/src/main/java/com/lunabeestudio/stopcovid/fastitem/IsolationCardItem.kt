/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/12/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemIsolationCardBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class IsolationCardItem : AbstractBindingItem<ItemIsolationCardBinding>() {

    var title: String? = null
    var content: String? = null
    var actions: List<IsolationCardActions> = emptyList()
    var onClickListener: View.OnClickListener? = null

    @DrawableRes
    var iconRes: Int? = null

    var contentDescription: String? = null

    override val type: Int = R.id.item_isolation_card

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemIsolationCardBinding {
        return ItemIsolationCardBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemIsolationCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.headerTextView.text = title
        binding.contentTextView.text = content
        onClickListener?.let {
            binding.mainLayout.setOnClickListener(it)
        }

        val actionBindings = listOf(binding.action1, binding.action2, binding.action3, binding.action4)
        for (i in actionBindings.indices) {
            if (actions.size > i) {
                actionBindings[i].textView.text = actions[i].label
                actionBindings[i].linkRootLayout.setOnClickListener(actions[i].actionListener)
                actionBindings[i].linkRootLayout.visibility = View.VISIBLE
            } else {
                actionBindings[i].linkRootLayout.visibility = View.GONE
            }
        }
    }
}

data class IsolationCardActions(
    val label: String?,
    val actionListener: View.OnClickListener,
)

fun isolationCardItem(block: (IsolationCardItem.() -> Unit)): IsolationCardItem = IsolationCardItem().apply(block)
