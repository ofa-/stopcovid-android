/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.animation.Animator
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class LottieItem : BaseItem<LottieItem.ViewHolder>(
    R.layout.item_lottie, ::ViewHolder, R.id.item_lottie
) {
    var state: State = State.OFF

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.onView.removeAllAnimatorListeners()
        holder.offView.removeAllAnimatorListeners()
        holder.offView.setMinFrame(1)
        holder.offView.addLottieOnCompositionLoadedListener {
            holder.onView.addLottieOnCompositionLoadedListener {
                when (state) {
                    State.ON -> onCase(holder)
                    State.OFF -> offCase(holder)
                    State.OFF_TO_ON -> offToOnCase(holder)
                    State.ON_TO_OFF -> onToOffCase(holder)
                }
            }
        }
    }

    private fun onToOffCase(holder: ViewHolder) {
        holder.onView.speed = -5f
        holder.onView.repeatCount = 0
        holder.offView.progress = 1f
        holder.offView.repeatCount = 0
        holder.offView.speed = -1f
        holder.onView.addAnimatorListener(object : EndAnimatorListener {
            override fun onAnimationEnd(animation: Animator?) {
                holder.onView.removeAnimatorListener(this)
                holder.offView.addAnimatorListener(object : EndAnimatorListener {
                    override fun onAnimationEnd(animation: Animator?) {
                        holder.offView.removeAnimatorListener(this)
                        state = State.OFF
                        offCase(holder)
                    }
                })
                holder.offView.isVisible = true
                holder.onView.isInvisible = true
                holder.offView.post {
                    holder.offView.playAnimation()
                }
            }
        })
        holder.onView.isVisible = true
        holder.offView.isInvisible = true
    }

    private fun offToOnCase(holder: ViewHolder) {
        holder.offView.frame = 1
        holder.offView.speed = 1f
        holder.offView.repeatCount = 0
        holder.offView.addAnimatorListener(object : EndAnimatorListener {
            override fun onAnimationEnd(animation: Animator?) {
                holder.offView.removeAnimatorListener(this)
                state = State.ON
                onCase(holder)
            }
        })
        holder.offView.isVisible = true
        holder.onView.isInvisible = true
        holder.offView.post {
            holder.offView.playAnimation()
        }
    }

    private fun offCase(holder: ViewHolder) {
        holder.offView.frame = 1
        holder.offView.speed = 0f
        holder.offView.isVisible = true
        holder.onView.isInvisible = true
        holder.offView.post {
            holder.offView.playAnimation()
        }
    }

    private fun onCase(holder: ViewHolder) {
        holder.onView.repeatCount = LottieDrawable.INFINITE
        holder.onView.speed = 1f
        holder.onView.isVisible = true
        holder.offView.isInvisible = true
        holder.onView.post {
            holder.onView.playAnimation()
        }
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.onView.removeAllAnimatorListeners()
        holder.offView.removeAllAnimatorListeners()
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val offView: LottieAnimationView = v.findViewById(R.id.offLottieAnimationView)
        val onView: LottieAnimationView = v.findViewById(R.id.onLottieAnimationView)

        init {
            LottieCompositionFactory.fromRawRes(v.context, R.raw.off_to_on).addListener { offToOnComposition ->
                offView.setComposition(offToOnComposition)
                LottieCompositionFactory.fromRawRes(v.context, R.raw.on_waving).addListener { onWavingComposition ->
                    onView.setComposition(onWavingComposition)
                }
            }
        }
    }
}

private interface EndAnimatorListener : Animator.AnimatorListener {
    override fun onAnimationCancel(animation: Animator?) {}
    override fun onAnimationRepeat(animation: Animator?) {}
    override fun onAnimationStart(animation: Animator?) {}
}

enum class State {
    OFF, ON, OFF_TO_ON, ON_TO_OFF
}

fun lottieItem(block: (LottieItem.() -> Unit)): LottieItem = LottieItem().apply(block)