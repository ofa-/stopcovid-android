/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.preference.PreferenceManager
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.userLanguage
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem
import java.util.Locale

class OnBoardingWelcomeFragment : OnBoardingFragment() {

    override fun getTitleKey(): String = "onboarding.welcomeController.title"
    override fun getButtonTitleKey(): String = "onboarding.welcomeController.howDoesItWork"
    override fun getOnButtonClick(): () -> Unit = {
        val hasUnsupportedLanguage = Locale.getDefault().language !in UiConstants.SUPPORTED_LOCALES.map { it.language }
        val hasAlreadySetLanguage = PreferenceManager.getDefaultSharedPreferences(requireContext()).userLanguage != null
        if (hasUnsupportedLanguage && !hasAlreadySetLanguage) {
            setFragmentResultListener(UserLanguageBottomSheetFragment.USER_LANGUAGE_RESULT_KEY) { _, bundle ->
                clearFragmentResult(UserLanguageBottomSheetFragment.USER_LANGUAGE_RESULT_KEY)
                val hasSetLanguage = bundle.getBoolean(UserLanguageBottomSheetFragment.USER_LANGUAGE_SET_BUNDLE_KEY)
                if (hasSetLanguage) {
                    // Result listener is called before the dialog is dismiss, wait for the navigation back before navigate to next fragment
                    findNavControllerOrNull()?.addOnDestinationChangedListener(object : NavController.OnDestinationChangedListener {
                        override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
                            if (controller.currentDestination?.id == R.id.onBoardingWelcomeFragment) {
                                controller.safeNavigate(
                                    OnBoardingWelcomeFragmentDirections.actionOnBoardingWelcomeFragmentToOnBoardingExplanationFragment()
                                )
                                controller.removeOnDestinationChangedListener(this)
                            }
                        }
                    })
                }
            }

            findNavControllerOrNull()
                ?.safeNavigate(OnBoardingWelcomeFragmentDirections.actionOnBoardingWelcomeFragmentToUserLanguageBottomSheetFragment())
        } else {
            findNavControllerOrNull()
                ?.safeNavigate(OnBoardingWelcomeFragmentDirections.actionOnBoardingWelcomeFragmentToOnBoardingExplanationFragment())
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }
        items += logoItem {
            imageRes = R.drawable.home
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["onboarding.welcomeController.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = strings["onboarding.welcomeController.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }

        return items
    }
}
