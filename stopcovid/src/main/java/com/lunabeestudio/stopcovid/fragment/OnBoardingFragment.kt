/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import com.google.android.material.appbar.AppBarLayout
import com.lunabeestudio.stopcovid.activity.OnBoardingActivity
import com.lunabeestudio.stopcovid.coreui.extension.registerToAppBarLayoutForLiftOnScroll
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.fragment.FastAdapterFragment

abstract class OnBoardingFragment : FastAdapterFragment() {

    abstract fun getTitleKey(): String
    abstract fun getButtonTitleKey(): String?
    abstract fun getOnButtonClick(): () -> Unit

    private fun getActivityBinding() = (activity as OnBoardingActivity).binding

    override fun getAppBarLayout(): AppBarLayout? = getActivityBinding().appBarLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.recyclerView?.registerToAppBarLayoutForLiftOnScroll(getActivityBinding().appBarLayout)
        initBottomButton()
    }

    override fun refreshScreen() {
        super.refreshScreen()
        (activity as AppCompatActivity).supportActionBar?.title = strings[getTitleKey()]
        getActivityBinding().bottomSheetLayout.bottomSheetButton.text = strings[getButtonTitleKey()].safeEmojiSpanify()
    }

    private fun initBottomButton() {
        getActivityBinding().bottomSheetLayout.bottomSheetButton.setOnClickListener {
            try {
                getOnButtonClick().invoke()
            } catch (e: IllegalArgumentException) {
                // back and button pressed quickly can trigger this exception.
            }
        }
        getActivityBinding().bottomSheetLayout.bottomSheetFrameLayout.post {
            binding?.recyclerView?.updatePadding(bottom = getActivityBinding().bottomSheetLayout.bottomSheetFrameLayout.height)
        }
    }
}
