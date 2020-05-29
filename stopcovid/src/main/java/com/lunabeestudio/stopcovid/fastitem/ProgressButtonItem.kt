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

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.cleanUpDrawable
import com.github.razir.progressbutton.showProgress
import com.google.android.material.button.MaterialButton
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class ProgressButtonItem(lifecycleOwner: LifecycleOwner) : BaseItem<ProgressButtonItem.ViewHolder>(
    R.layout.item_button, { v -> ViewHolder(lifecycleOwner, v) }, R.id.item_progress_button
) {
    var text: String? = null
    var gravity: Int = Gravity.NO_GRAVITY
    var startInProgress: Boolean = false
    var getProgressButton: ((MaterialButton) -> Unit)? = null
    var onClickListener: View.OnClickListener? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.button.apply {
            text = this@ProgressButtonItem.text
            cleanUpDrawable()
            if (startInProgress) {
                showProgress {
                    progressColor = ContextCompat.getColor(context, R.color.color_on_primary)
                    gravity = DrawableButton.GRAVITY_CENTER
                }
            }
            getProgressButton?.invoke(this)
            setOnClickListener(onClickListener)
            holder.button.updateLayoutParams<FrameLayout.LayoutParams> {
                this.gravity = this@ProgressButtonItem.gravity
            }
        }
    }

    class ViewHolder(lifecycleOwner: LifecycleOwner, v: View) : RecyclerView.ViewHolder(v) {
        val button: MaterialButton = v.findViewById(R.id.button)

        init {
            lifecycleOwner.bindProgressButton(button)
        }
    }
}

fun progressButtonItem(lifecycleOwner: LifecycleOwner,
    block: (ProgressButtonItem.() -> Unit)): ProgressButtonItem = ProgressButtonItem(lifecycleOwner).apply(block)