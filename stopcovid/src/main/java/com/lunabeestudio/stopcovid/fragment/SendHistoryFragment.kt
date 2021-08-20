/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/07/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.databinding.DialogProgressBarBinding
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.model.UnauthorizedException
import com.lunabeestudio.stopcovid.viewmodel.CodeViewModel
import com.lunabeestudio.stopcovid.viewmodel.CodeViewModelFactory
import com.mikepenz.fastadapter.GenericItem

class SendHistoryFragment : MainFragment() {

    private val args: SendHistoryFragmentArgs by navArgs()
    private var dialogProgressBarBinding: DialogProgressBarBinding? = null
    private var progressDialog: AlertDialog? = null

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel: CodeViewModel by viewModels { CodeViewModelFactory(robertManager) }

    private val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // prevent back navigation
        }
    }

    override fun getTitleKey(): String = "sendHistoryController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModelObserver()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    private fun initViewModelObserver() {
        viewModel.loadingInProgress.observe(viewLifecycleOwner) { inProgress ->
            if (inProgress == null) {
                progressDialog?.dismiss()
                progressDialog = null
                onBackPressedCallback.isEnabled = false
                activity?.let { activity ->
                    activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    // Fix loosing status bar light status
                    WindowInsetsControllerCompat(
                        activity.window,
                        activity.window.decorView
                    ).isAppearanceLightStatusBars = !activity.isNightMode()
                }
            } else {
                onBackPressedCallback.isEnabled = true
                if (progressDialog == null) {
                    context?.let { context ->
                        dialogProgressBarBinding = DialogProgressBarBinding.inflate(LayoutInflater.from(context))
                        progressDialog = MaterialAlertDialogBuilder(context).apply {
                            setTitle(strings["sendHistoryController.progress.title"])
                            setMessage(strings["sendHistoryController.progress.message"])
                            setCancelable(false)
                            setView(dialogProgressBarBinding?.root)
                        }.show()
                    }
                }
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                dialogProgressBarBinding?.dialogProgressBar?.progress = (inProgress * 100f).toInt()
                dialogProgressBarBinding?.dialogPercentTextView?.text = String.format("%d%%", (inProgress * 100f).toInt())
            }
        }
        viewModel.codeSuccess.observe(viewLifecycleOwner) {
            context?.let { context ->
                (context.applicationContext as StopCovid).cancelActivateReminder()
                MaterialAlertDialogBuilder(context)
                    .setTitle(strings["sendHistoryController.successAlert.title"])
                    .setMessage(strings["sendHistoryController.successAlert.message"])
                    .setPositiveButton(strings["sendHistoryController.successAlert.button.learnMore"]) { _, _ ->
                        findNavControllerOrNull()?.safeNavigate(SendHistoryFragmentDirections.actionSendHistoryFragmentToIsSickFragment())
                    }
                    .setCancelable(false)
                    .show()
            }
        }
        viewModel.covidException.observe(viewLifecycleOwner) { error ->
            context?.let { context ->
                if (error is UnauthorizedException) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(strings["sendHistoryController.alert.invalidCode.title"])
                        .setMessage(strings["sendHistoryController.alert.invalidCode.message"])
                        .setPositiveButton(strings["common.ok"], null)
                        .show()
                } else {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(strings["common.error.unknown"])
                        .setMessage(error.getString(strings))
                        .setPositiveButton(strings["common.ok"], null)
                        .show()
                }
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
        items += buttonItem {
            text = strings["common.send"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                viewModel.verifyCode(
                    args.code,
                    args.firstSymptoms?.toInt(),
                    args.positiveTest?.toInt(),
                    requireContext().applicationContext as RobertApplication
                )
            }
            identifier = "common.send".hashCode().toLong()
        }

        return items
    }
}
