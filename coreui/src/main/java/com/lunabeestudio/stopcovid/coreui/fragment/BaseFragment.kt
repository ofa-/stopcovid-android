/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.lunabeestudio.stopcovid.coreui.LocalizedApplication
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings

import android.view.WindowManager
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import kotlin.math.max
import kotlin.math.min


abstract class BaseFragment : Fragment() {

    abstract fun refreshScreen()

    val strings: LocalizedStrings
        get() = (activity?.application as? LocalizedApplication)?.localizedStrings ?: emptyMap()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity?.application as? LocalizedApplication)?.liveLocalizedStrings?.observe(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    protected fun stringsFormat(key: String, vararg args: Any?): String? {
        return strings.stringsFormat(key, *args)
    }

    protected fun setFullBrightness(on: Boolean = true) {
        val window = activity?.window ?: return
        val params = window.attributes
        params.screenBrightness = if (on)
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                else
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = params
    }

    protected fun toggleFullBrightness() {
        val window = activity?.window ?: return
        setFullBrightness(
            window.attributes.screenBrightness ==
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
    }

    protected fun setZoomable(element: View) {
        scaleGestureDetector = ScaleGestureDetector(requireActivity(), ScaleListener())
        element.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            scaleGestureDetector.onTouchEvent(motionEvent)
        })
    }

    open fun onZoom(scaleFactor: Float) {
    }

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
       override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
          scaleFactor *= scaleGestureDetector.scaleFactor
          scaleFactor = max(0.1f, min(scaleFactor, 10.0f))
          onZoom(scaleFactor)
          return true
       }
    }
}
