/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/07/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.fastitem.progressButtonItem
import com.lunabeestudio.stopcovid.model.BackendException
import com.lunabeestudio.stopcovid.model.UnauthorizedException
import com.lunabeestudio.stopcovid.viewmodel.CodeViewModel
import com.lunabeestudio.stopcovid.viewmodel.CodeViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import java.lang.ref.WeakReference

class SendHistoryFragment : MainFragment() {

    private val args: SendHistoryFragmentArgs by navArgs()

    private val robertManager by lazy {
        requireContext().robertManager()
    }
    private var progressButton: WeakReference<MaterialButton>? = null

    private val viewModel: CodeViewModel by viewModels { CodeViewModelFactory(robertManager) }

    override fun getTitleKey(): String = "sendHistoryController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModelObserver()
    }

    private fun initViewModelObserver() {
        viewModel.loadingInProgress.observe(viewLifecycleOwner) { inProgress ->
            if (inProgress) {
                progressButton?.get()?.showProgress {
                    progressColor = ContextCompat.getColor(requireContext(), R.color.color_on_primary)
                    gravity = DrawableButton.GRAVITY_CENTER
                }
            } else {
                progressButton?.get()?.hideProgress(strings["common.send"])
            }
        }
        viewModel.codeSuccess.observe(viewLifecycleOwner) {
            findNavController().navigate(SendHistoryFragmentDirections.actionSendHistoryFragmentToIsSickFragment())
        }
        viewModel.covidException.observe(viewLifecycleOwner) { error ->
            if (error is BackendException || error is UnauthorizedException) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["sendHistoryController.alert.invalidCode.title"])
                    .setMessage(strings["sendHistoryController.alert.invalidCode.message"])
                    .setPositiveButton(strings["common.ok"], null)
                    .show()
            } else {
                (activity as? MainActivity)?.showErrorSnackBar(error.getString(strings))
            }
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.protect
            identifier = items.count().toLong()
        }
        items += titleItem {
            text = strings["sendHistoryController.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["sendHistoryController.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
        items += progressButtonItem(viewLifecycleOwner) {
            text = strings["common.send"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                viewModel.verifyCode(args.code, args.firstSymptoms, requireContext().applicationContext as RobertApplication)
            }
            startInProgress = viewModel.loadingInProgress.value == true
            getProgressButton = { button ->
                progressButton = WeakReference(button)
            }
            identifier = items.count().toLong()
        }

        return items
    }
}