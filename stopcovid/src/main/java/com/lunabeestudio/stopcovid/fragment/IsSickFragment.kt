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
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.callPhone
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.lightButtonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.openInChromeTab
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.viewmodel.IsSickViewModel
import com.lunabeestudio.stopcovid.viewmodel.IsSickViewModelFactory
import com.mikepenz.fastadapter.GenericItem

class IsSickFragment : AboutMainFragment() {

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel: IsSickViewModel by viewModels { IsSickViewModelFactory(robertManager) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModelObserver()
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun initViewModelObserver() {
        viewModel.loadingInProgress.observe(viewLifecycleOwner) { loadingInProgress ->
            (activity as? MainActivity)?.showProgress(loadingInProgress)
        }
        viewModel.covidException.observe(viewLifecycleOwner) { covidException ->
            showErrorSnackBar(covidException.getString(strings))
        }
        viewModel.quitStopCovidSuccess.observe(viewLifecycleOwner) {
            showSnackBar(strings["manageDataController.quitStopCovid.success"] ?: "")
            sharedPreferences.edit {
                remove(Constants.SharedPrefs.ON_BOARDING_DONE)
            }
            try {
                findNavController()
                    .navigate(ManageDataFragmentDirections.actionGlobalOnBoardingActivity())
                activity?.finishAndRemoveTask()
            } catch (e: IllegalArgumentException) {
                // If user leave the screen before logout is done
            }
        }
    }

    override fun getTitleKey(): String = "sickController.sick.title"

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.sick
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["sickController.sick.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["sickController.sick.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += buttonItem {
            text = strings["sickController.button.recommendations"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                strings["sickController.button.recommendations.url"]?.openInChromeTab(requireContext())
            }
            identifier = items.count().toLong()
        }
        items += buttonItem {
            text = strings["informationController.step.appointment.buttonTitle"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                strings["callCenter.phoneNumber"]?.callPhone(requireContext())
            }
            identifier = items.count().toLong()
        }
        items += lightButtonItem {
            text = strings["manageDataController.quitStopCovid.button"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                viewModel.quitStopCovid(requireContext().applicationContext as RobertApplication)
            }
            identifier = items.count().toLong()
        }

        return items
    }
}