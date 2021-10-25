/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/08/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.widget

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.widget.VideoView

class TACVideoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : VideoView(context, attrs) {

    var tacVideoViewListener: TACVideoViewListener? = null

    val shCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            tacVideoViewListener?.onSurfaceCreated()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            // no-op
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            tacVideoViewListener?.onSurfaceDestroyed()
        }
    }

    init {
        holder.addCallback(shCallback)
    }

    override fun start() {
        super.start()
        tacVideoViewListener?.onPlay()
    }

    override fun pause() {
        super.pause()
        tacVideoViewListener?.onPause()
    }

    interface TACVideoViewListener {
        fun onPlay()
        fun onPause()
        fun onSurfaceCreated()
        fun onSurfaceDestroyed()
    }
}