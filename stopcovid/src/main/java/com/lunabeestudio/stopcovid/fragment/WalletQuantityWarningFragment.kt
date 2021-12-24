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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentWalletQuantityWarningBinding

class WalletQuantityWarningFragment : BaseFragment() {

    val args: WalletQuantityWarningFragmentArgs by navArgs()

    lateinit var binding: FragmentWalletQuantityWarningBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = FragmentWalletQuantityWarningBinding.inflate(inflater, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { v, insets ->
            val insetsMask = WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            v.updatePadding(
                top = insets.getInsets(insetsMask).top
            )
            insets
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.continueButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(strings["walletQuantityWarningController.continueAlert.title"])
                .setMessage(strings["walletQuantityWarningController.continueAlert.message"])
                .setPositiveButton(strings["walletQuantityWarningController.continueAlert.confirm"]) { _, _ ->
                    val result = bundleOf(
                        WALLET_QUANTITY_WARNING_BUNDLE_CONFIRM_KEY to true,
                        WALLET_QUANTITY_WARNING_BUNDLE_PASSTHROUGH_KEY to args.passthrough,
                    )
                    setFragmentResult(WALLET_QUANTITY_WARNING_RESULT_KEY, result)
                    findNavControllerOrNull()?.navigateUp()
                }
                .setNegativeButton(strings["common.cancel"]) { _, _ ->
                    val result = bundleOf(
                        WALLET_QUANTITY_WARNING_BUNDLE_CONFIRM_KEY to false,
                        WALLET_QUANTITY_WARNING_BUNDLE_PASSTHROUGH_KEY to args.passthrough,
                    )
                    setFragmentResult(WALLET_QUANTITY_WARNING_RESULT_KEY, result)
                    findNavControllerOrNull()?.navigateUp()
                }
                .setCancelable(false)
                .show()
        }

        binding.cancelButton.setOnClickListener {
            findNavControllerOrNull()?.navigateUp()
        }
    }

    override fun refreshScreen() {
        binding.titleTextView.text = strings["walletQuantityWarningController.title"]
        binding.explanationTextView.text = strings["walletQuantityWarningController.explanation"]
        binding.continueButton.text = strings["walletQuantityWarningController.continue"]
        binding.cancelButton.text = strings["common.cancel"]
    }

    companion object {
        const val WALLET_QUANTITY_WARNING_RESULT_KEY: String = "WALLET_QUANTITY_WARNING_RESULT_KEY"
        const val WALLET_QUANTITY_WARNING_BUNDLE_CONFIRM_KEY: String = "WALLET_QUANTITY_WARNING_BUNDLE_CONFIRM_KEY"
        const val WALLET_QUANTITY_WARNING_BUNDLE_PASSTHROUGH_KEY: String = "WALLET_QUANTITY_WARNING_BUNDLE_PASSTHROUGH_KEY"
    }
}