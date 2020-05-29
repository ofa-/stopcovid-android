/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/08/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.view.Gravity
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.mikepenz.fastadapter.GenericItem

class OnBoardingBatteryFragment : OnBoardingFragment() {

    override fun getTitleKey(): String = "onboarding.batteryController.title"
    override fun getButtonTitleKey(): String? = "onboarding.batteryController.accept"

    @SuppressLint("BatteryLife")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun getOnButtonClick(): () -> Unit = {
        if (!ProximityManager.isBatteryOptimizationOn(requireContext())) {
            ProximityManager.requestIgnoreBatteryOptimization(this)
        } else {
            findNavController()
                .navigate(OnBoardingBatteryFragmentDirections.actionOnBoardingBatteryFragmentToOnBoardingNotificationFragment())
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.battery
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["onboarding.batteryController.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = strings["onboarding.batteryController.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }

        return items
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.Activity.BATTERY.ordinal) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    findNavController()
                        .navigate(OnBoardingBatteryFragmentDirections.actionOnBoardingBatteryFragmentToOnBoardingNotificationFragment())
                } catch (e: IllegalArgumentException) {
                    // back and button pressed quickly can trigger this exception.
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}