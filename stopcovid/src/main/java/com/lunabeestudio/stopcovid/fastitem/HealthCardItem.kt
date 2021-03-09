/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/07/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem
import com.lunabeestudio.stopcovid.extension.setTextOrHide

class HealthCardItem(layoutRes: Int) : BaseItem<HealthCardItem.ViewHolder>(
    layoutRes, ::ViewHolder, R.id.item_contact + layoutRes
) {
    var header: String? = null
    var title: String? = null
    var caption: String? = null
    var dateLabel: String? = null
    var dateValue: String? = null

    // Gradient background, override theme
    var gradientBackground: GradientDrawable? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.headerTextView.setTextOrHide(header.safeEmojiSpanify())
        holder.titleTextView.setTextOrHide(title.safeEmojiSpanify())
        holder.captionTextView.setTextOrHide(caption.safeEmojiSpanify())
        holder.dateLabelTextView.setTextOrHide(dateLabel.safeEmojiSpanify())
        holder.dateValueTextView.setTextOrHide(dateValue.safeEmojiSpanify())
        gradientBackground?.let { holder.rootLayout.background = it }
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.rootLayout.background = null
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val headerTextView: TextView = v.findViewById(R.id.headerTextView)
        val titleTextView: TextView = v.findViewById(R.id.titleTextView)
        val captionTextView: TextView = v.findViewById(R.id.captionTextView)
        val dateLabelTextView: TextView = v.findViewById(R.id.dateLabelTextView)
        val dateValueTextView: TextView = v.findViewById(R.id.dateValueTextView)
        val rootLayout: ConstraintLayout = v.findViewById(R.id.rootLayout)
    }
}

fun healthCardItem(layoutRes: Int, block: (HealthCardItem.() -> Unit)): HealthCardItem = HealthCardItem(layoutRes).apply(block)