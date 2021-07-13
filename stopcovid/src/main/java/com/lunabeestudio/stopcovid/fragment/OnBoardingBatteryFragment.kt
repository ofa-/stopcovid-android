/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/08/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.mikepenz.fastadapter.GenericItem

class OnBoardingBatteryFragment : OnBoardingFragment() {

    override fun getTitleKey(): String = "onboarding.batteryController.title"
    override fun getButtonTitleKey(): String = "onboarding.batteryController.accept"

    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null

    @SuppressLint("BatteryLife")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun getOnButtonClick(): () -> Unit = {
        if (!ProximityManager.isBatteryOptimizationOff(requireContext())) {
            ProximityManager.requestIgnoreBatteryOptimization(this, activityResultLauncher)
            setHasOptionsMenu(true)
        } else {
            findNavControllerOrNull()
                ?.safeNavigate(OnBoardingBatteryFragmentDirections.actionOnBoardingBatteryFragmentToOnBoardingNotificationFragment())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                findNavControllerOrNull()
                    ?.safeNavigate(OnBoardingBatteryFragmentDirections.actionOnBoardingBatteryFragmentToOnBoardingNotificationFragment())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.text_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.item_text).title = strings["common.skip"]
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.item_text) {
            findNavControllerOrNull()
                ?.safeNavigate(OnBoardingBatteryFragmentDirections.actionOnBoardingBatteryFragmentToOnBoardingNotificationFragment())
            true
        } else {
            super.onOptionsItemSelected(item)
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
}
