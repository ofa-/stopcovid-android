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

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.hideSoftKeyBoard
import com.lunabeestudio.stopcovid.coreui.fastitem.ButtonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.fastitem.audioItem
import com.lunabeestudio.stopcovid.fastitem.editTextItem
import com.lunabeestudio.stopcovid.fastitem.imageItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.model.CaptchaNextFragment
import com.lunabeestudio.stopcovid.model.UnauthorizedException
import com.lunabeestudio.stopcovid.viewmodel.CaptchaViewModel
import com.lunabeestudio.stopcovid.viewmodel.CaptchaViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import java.io.File

class CaptchaFragment : MainFragment() {

    override fun getTitleKey(): String = "captchaController.title"

    private val args: CaptchaFragmentArgs by navArgs()

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val viewModel: CaptchaViewModel by viewModels { CaptchaViewModelFactory(robertManager) }

    private val imageFile: File
        get() = File(requireContext().cacheDir, "image.png")

    private val audioFile: File
        get() = File(requireContext().cacheDir, "audio.wav")

    private lateinit var buttonItem: ButtonItem

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        resetFiles()
        super.onViewCreated(view, savedInstanceState)
        initViewModelObserver()
        initViewModel()
    }

    private fun initViewModelObserver() {
        viewModel.loadingInProgress.observe(viewLifecycleOwner) { inProgress ->
            (activity as? MainActivity)?.showProgress(inProgress)
        }
        viewModel.imageSuccess.observe(viewLifecycleOwner) {
            refreshScreen()
        }
        viewModel.audioSuccess.observe(viewLifecycleOwner) {
            refreshScreen()
        }
        viewModel.codeSuccess.observe(viewLifecycleOwner) { captchaNextFragment ->
            val nextFragmentArgs = if (captchaNextFragment == CaptchaNextFragment.Venue) {
                args.nextFragmentArgs
            } else {
                null
            }
            captchaNextFragment.registerPostAction(
                findNavControllerOrNull(),
                sharedPreferences,
                nextFragmentArgs?.let { VenueQRCodeFragmentArgs.fromBundle(nextFragmentArgs) }
            )
        }
        viewModel.covidException.observe(viewLifecycleOwner) { error ->
            if (error is UnauthorizedException && viewModel.code.isNotBlank()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["captchaController.alert.invalidCode.title"])
                    .setMessage(strings["captchaController.alert.invalidCode.message"])
                    .setPositiveButton(strings["common.ok"], null)
                    .show()
                viewModel.code = ""
                resetFiles()
                viewModel.generateCaptcha()
            } else {
                findNavControllerOrNull()?.navigateUp()
                (activity as? MainActivity)?.showErrorSnackBar(error.getString(strings))
            }
        }
    }

    private fun initViewModel() {
        viewModel.imagePath = imageFile.absolutePath
        viewModel.audioPath = audioFile.absolutePath
        viewModel.isImage = (requireContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.isEnabled != true
        viewModel.generateCaptcha()
    }

    private fun resetFiles() {
        imageFile.delete()
        audioFile.delete()
    }

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings[
                if (viewModel.isImage) {
                    "captchaController.mainMessage.image.title"
                } else {
                    "captchaController.mainMessage.audio.title"
                }
            ]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = strings[
                if (viewModel.isImage) {
                    "captchaController.mainMessage.image.subtitle"
                } else {
                    "captchaController.mainMessage.audio.subtitle"
                }
            ]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        if (imageFile.exists()) {
            items += imageItem {
                imageFile = this@CaptchaFragment.imageFile
                identifier = 50L
            }
        }
        if (audioFile.exists()) {
            items += audioItem(requireContext(), audioFile) {
                playTalkbackText = strings["accessibility.hint.captcha.audio.button.play"]
                pauseTalkbackText = strings["accessibility.hint.captcha.audio.button.pause"]
                identifier = 51L
            }
        }
        items += linkItem {
            text = strings[
                if (viewModel.isImage) {
                    "captchaController.switchToAudio"
                } else {
                    "captchaController.switchToImage"
                }
            ]
            iconRes = if (viewModel.isImage) {
                R.drawable.ic_audio
            } else {
                R.drawable.ic_visual
            }
            onClickListener = View.OnClickListener {
                viewModel.isImage = !viewModel.isImage
                resetFiles()
                refreshScreen()
                viewModel.generateCaptcha()
            }
            identifier = 52L
        }
        items += editTextItem {
            hint = strings[
                if (viewModel.isImage) {
                    "captchaController.textField.image.placeholder"
                } else {
                    "captchaController.textField.audio.placeholder"
                }
            ]
            text = viewModel.code
            onTextChange = { text ->
                viewModel.code = text.toString()
                refreshButton()
            }
            onDone = {
                if (viewModel.code.isNotBlank()) {
                    activity?.hideSoftKeyBoard()
                    viewModel.register(requireContext().applicationContext as RobertApplication, args.nextFragment)
                }
            }
            identifier = 53L
        }
        buttonItem = buttonItem {
            text = strings[
                when {
                    viewModel.code.isBlank() && viewModel.isImage -> "captchaController.generate.image"
                    viewModel.code.isBlank() && !viewModel.isImage -> "captchaController.generate.sound"
                    else -> "captchaController.button.title"
                }
            ]
            onClickListener = View.OnClickListener {
                if (viewModel.code.isBlank()) {
                    resetFiles()
                    viewModel.generateCaptcha()
                } else {
                    activity?.hideSoftKeyBoard()
                    viewModel.register(requireContext().applicationContext as RobertApplication, args.nextFragment)
                }
            }
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += buttonItem

        return items
    }

    private fun refreshButton() {
        buttonItem.text = strings[
            when {
                viewModel.code.isBlank() && viewModel.isImage -> "captchaController.generate.image"
                viewModel.code.isBlank() && !viewModel.isImage -> "captchaController.generate.sound"
                else -> "captchaController.button.title"
            }
        ]
        if (binding?.recyclerView?.isComputingLayout == false) {
            binding?.recyclerView?.adapter?.notifyItemChanged(buttonItem.identifier.toInt())
        }
    }
}