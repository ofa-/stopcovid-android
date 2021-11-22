/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/22/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible

class SecuredBitmapView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private val isHuawei: Boolean = android.os.Build.MANUFACTURER.compareTo(HUAWEI_MANUFACTURER, true) == 0 ||
        android.os.Build.BRAND.compareTo(HUAWEI_MANUFACTURER, true) == 0
    private var securedView: SurfaceView? = null
    private var imageView: ImageView? = null

    var bitmap: Bitmap? = null
        set(value) {
            field = value
            value?.let {
                if (isHuawei) {
                    imageView?.setImageBitmap(bitmap)
                } else {
                    securedView?.holder?.let { holder -> drawBitmap(holder, it) }
                }
            }
        }

    init {
        if (isHuawei) {
            imageView = ImageView(context).apply {
                adjustViewBounds = true
                setImageBitmap(bitmap)
            }
            addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        } else {
            securedView = SurfaceView(context).apply {
                setSecure(true)
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        /* no-op */
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        bitmap?.let { drawBitmap(holder, it) }
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        /* no-op */
                    }
                })
            }
            addView(securedView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
    }

    suspend fun <T> runUnsecured(block: suspend () -> T): T {
        setUnsecure(true)
        return try {
            block()
        } finally {
            setUnsecure(false)
        }
    }

    private fun setUnsecure(isUnsecure: Boolean) {
        if (isHuawei) return

        if (isUnsecure) {
            imageView = ImageView(context).apply {
                adjustViewBounds = true
                setImageBitmap(bitmap)
            }
            addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            securedView?.isVisible = false
        } else {
            securedView?.isVisible = true
            imageView?.let {
                it.setImageDrawable(null)
                removeView(imageView)
            }
            imageView = null
        }
    }

    private fun drawBitmap(holder: SurfaceHolder, bitmap: Bitmap) {
        holder.lockCanvas()?.let { canvas ->
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            holder.unlockCanvasAndPost(canvas)
        }
    }

    companion object {
        private const val HUAWEI_MANUFACTURER = "HUAWEI"
    }
}