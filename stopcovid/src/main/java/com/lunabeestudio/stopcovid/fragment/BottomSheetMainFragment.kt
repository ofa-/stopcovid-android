/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/01/12 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import com.lunabeestudio.stopcovid.coreui.databinding.LayoutButtonBottomSheetBinding

abstract class BottomSheetMainFragment : MainFragment() {

    protected var bottomSheetBinding: LayoutButtonBottomSheetBinding? = null
    abstract fun onBottomSheetButtonClicked()
    abstract fun getBottomSheetButtonKey(): String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        binding?.root?.let { frameLayout ->
            bottomSheetBinding = LayoutButtonBottomSheetBinding.inflate(inflater, frameLayout)
            bottomSheetBinding?.bottomSheetButton?.setOnClickListener {
                onBottomSheetButtonClicked()
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomSheetBinding?.bottomSheetFrameLayout?.post {
            binding?.recyclerView?.updatePadding(bottom = bottomSheetBinding?.bottomSheetFrameLayout?.height ?: 0)
        }
    }

    override fun refreshScreen() {
        super.refreshScreen()
        bottomSheetBinding?.bottomSheetButton?.text = strings[getBottomSheetButtonKey()]
    }
}