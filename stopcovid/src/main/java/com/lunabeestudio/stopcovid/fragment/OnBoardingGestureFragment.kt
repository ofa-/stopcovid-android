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

import android.content.SharedPreferences
import android.view.View
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.manager.GestureManager
import com.mikepenz.fastadapter.GenericItem

class OnBoardingGestureFragment : OnBoardingFragment() {

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun getTitleKey(): String = "onboarding.gesturesController.title"
    override fun getButtonTitleKey(): String? = "onboarding.gesturesController.noted"
    override fun getOnButtonClick(): () -> Unit = {
        sharedPreferences.edit {
            putBoolean(Constants.SharedPrefs.ON_BOARDING_DONE, true)
        }
        findNavController()
            .navigate(OnBoardingGestureFragmentDirections.actionOnBoardingGestureFragmentToMainActivity())
        activity?.finishAndRemoveTask()
    }

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        GestureManager.fillItems(items, strings)

        return items
    }
}
