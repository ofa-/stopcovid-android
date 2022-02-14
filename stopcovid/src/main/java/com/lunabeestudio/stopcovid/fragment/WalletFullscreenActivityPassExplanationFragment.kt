/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/1/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.databinding.LayoutButtonBottomSheetBinding
import com.lunabeestudio.stopcovid.coreui.extension.findParentFragmentByType
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.extension.activityPassValidFuture
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.isEligibleForActivityPass
import com.lunabeestudio.stopcovid.extension.navGraphWalletViewModels
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.NoInternetException
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.usecase.GenerateActivityPassState
import com.lunabeestudio.stopcovid.usecase.GenerateActivityPassStateName
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import com.lunabeestudio.stopcovid.worker.ActivityPassAvailableNotificationWorker
import com.lunabeestudio.stopcovid.worker.DccLightGenerationWorker
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

class WalletFullscreenActivityPassExplanationFragment : MainFragment() {

    private var bottomBinding: LayoutButtonBottomSheetBinding? = null

    private val robertManager by lazy { requireContext().robertManager() }

    private val viewModel by navGraphWalletViewModels<WalletFullscreenPagerFragment> {
        WalletViewModelFactory(
            robertManager,
            injectionContainer.blacklistDCCManager,
            injectionContainer.blacklist2DDOCManager,
            injectionContainer.walletRepository,
            injectionContainer.generateActivityPassUseCase,
            injectionContainer.getSmartWalletMapUseCase,
            injectionContainer.getSmartWalletStateUseCase,
        )
    }

    private var generationErrorDialog: AlertDialog? = null

    private val certificate: WalletCertificate? by lazy {
        val certificateId = arguments?.getString(CERTIFICATE_ID_ARG_KEY)
        viewModel.certificates.value.data?.firstOrNull { it.id == certificateId }
    }

    private var activityPassState: ActivityPassState? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.binding?.tabLayout?.isVisible = true

        lifecycleScope.launch {
            val activityPass = certificate?.id?.let { viewModel.getNotExpiredActivityPass(it) }
            refreshViewState(activityPass)
        }
    }

    private suspend fun refreshViewState(activityPass: EuropeanCertificate?) {
        activityPassState = when {
            activityPass?.activityPassValidFuture() == true -> {
                ActivityPassState.Future(activityPass)
            }
            certificate?.isEligibleForActivityPass(
                injectionContainer.blacklistDCCManager,
                robertManager.configuration,
                injectionContainer.getSmartWalletStateUseCase,
            ) == true -> if ((certificate as? EuropeanCertificate)?.canRenewActivityPass == true) {
                ActivityPassState.EligibleRenewFail
            } else {
                ActivityPassState.Eligible
            }
            else -> ActivityPassState.NonEligible
        }.also { activityPassState ->
            when (activityPassState) {
                is ActivityPassState.Future -> {
                    binding?.root?.removeView(bottomBinding?.coordinatorLayout)
                    bottomBinding = null
                    lifecycleScope.launch {
                        delay(activityPassState.activityPass.timestamp + 1_000L - System.currentTimeMillis())
                        findParentFragmentByType<WalletFullscreenPagerFragment>()?.refreshPager()
                    }
                }
                ActivityPassState.Eligible,
                ActivityPassState.EligibleRenewFail -> {
                    if (activityPassState is ActivityPassState.EligibleRenewFail) {
                        certificate?.id?.let { id -> DccLightGenerationWorker.startDccLightGenerationWorker(requireContext(), id) }
                    }

                    setupBottomSheet()
                    observeGenerationWorker()
                }
                ActivityPassState.NonEligible -> {
                    /* no-op */
                }
            }
        }

        refreshScreen()
    }

    override fun onResume() {
        super.onResume()

        certificate?.id?.let { certificateId ->
            lifecycleScope.launch {
                if (viewModel.getNotExpiredActivityPass(certificateId) != null) {
                    generationErrorDialog?.dismiss()
                    findParentFragmentByType<WalletFullscreenPagerFragment>()?.refreshPager()
                }
            }
        }
    }

    private fun observeGenerationWorker() {
        val certificateId = certificate?.id ?: return

        DccLightGenerationWorker.getInfo(requireContext(), certificateId).observe(viewLifecycleOwner) { workInfo ->
            if (workInfo?.state == WorkInfo.State.RUNNING) {
                when (DccLightGenerationWorker.getProgressState(workInfo)) {
                    GenerateActivityPassStateName.DOWNLOADED, null -> (activity as? MainActivity)?.showProgress(true)
                    GenerateActivityPassStateName.FIRST_ACTIVITY_PASS_SAVED,
                    GenerateActivityPassStateName.ENDED -> {
                        (activity as? MainActivity)?.showProgress(false)
                        generationErrorDialog?.dismiss()
                        findParentFragmentByType<WalletFullscreenPagerFragment>()?.refreshPager()
                    }
                }
            }
        }
    }

    private fun setupBottomSheet() {
        val bottomView = layoutInflater.inflate(R.layout.layout_button_bottom_sheet, binding?.root, true)
        bottomBinding = LayoutButtonBottomSheetBinding.bind(bottomView).apply {
            bottomSheetButton.setOnClickListener {
                val certificate = certificate
                if (certificate != null && certificate is EuropeanCertificate) {
                    findParentFragmentByType<WalletFullscreenPagerFragment>()?.let { pagerFragment ->
                        pagerFragment.setFragmentResultListener(
                            GenerateActivityPassBottomSheetFragment.CONFIRM_GENERATE_ACTIVITY_PASS_RESULT_KEY
                        ) { _: String, bundle: Bundle ->
                            if (bundle[GenerateActivityPassBottomSheetFragment.CONFIRM_GENERATE_ACTIVITY_PASS_BUNDLE_KEY_CONFIRM] == true) {
                                generateActivityPass(certificate, pagerFragment)
                            }
                        }
                        pagerFragment.openGenerateActivityPassBottomSheet()
                    }
                }
            }
        }
    }

    private fun generateActivityPass(
        certificate: EuropeanCertificate,
        pagerFragment: WalletFullscreenPagerFragment
    ) {
        val flow = viewModel.generateActivityPass(certificate)
        val appContext = activity?.applicationContext
        pagerFragment.lifecycleScope.launch {
            flow.collect { result ->
                val resultData = result.data
                when {
                    result is TacResult.Failure -> {
                        (activity as? MainActivity)?.showProgress(false)

                        generationErrorDialog = MaterialAlertDialogBuilder(requireContext())
                            .setTitle(strings["activityPass.fullscreen.unavailable.alert.title"])
                            .setMessage(strings["activityPass.fullscreen.unavailable.alert.message"])
                            .setNegativeButton(strings["common.cancel"], null)
                            .setPositiveButton(strings["common.show"]) { _, _ ->
                                pagerFragment.selectBorderTab()
                            }
                            .show()

                        // Retry with worker on internet error
                        if (result.throwable is NoInternetException) {
                            DccLightGenerationWorker.startDccLightGenerationWorker(requireContext(), certificate.id)
                            observeGenerationWorker()
                        } else {
                            Timber.e(result.throwable)
                        }
                    }
                    resultData is GenerateActivityPassState.FirstActivityPassSaved -> {
                        (activity as? MainActivity)?.showProgress(false)
                        if (resultData.activityPass?.activityPassValidFuture() == true) {
                            refreshViewState(resultData.activityPass)
                        } else {
                            pagerFragment.refreshPager()
                        }
                    }
                    resultData is GenerateActivityPassState.Ended -> {
                        (activity as? MainActivity)?.showProgress(false)
                        pagerFragment.refreshPager()
                    }
                    resultData is GenerateActivityPassState.Downloaded || resultData == null -> {
                        (activity as? MainActivity)?.showProgress(true)
                    }
                }
            }
        }.invokeOnCompletion {
            if (it is CancellationException && appContext != null) {
                DccLightGenerationWorker.startDccLightGenerationWorker(appContext, certificate.id)
            }
        }
    }

    override fun getTitleKey(): String = "walletController.title"

    override fun refreshScreen() {
        super.refreshScreen()
        bottomBinding?.bottomSheetButton?.text = strings["activityPass.fullscreen.button.generate"]
    }

    override suspend fun getItems(): List<GenericItem> {
        val activityPassState = activityPassState ?: return emptyList()

        val items = mutableListOf<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        when (activityPassState) {
            ActivityPassState.Eligible -> {
                val readMoreAction = strings["activityPass.fullscreen.readMore.url"].takeIf { it?.isNotBlank() == true }?.let { url ->
                    Action(label = strings["activityPass.fullscreen.readMore"]) {
                        url.openInExternalBrowser(requireContext())
                    }
                }

                items += cardWithActionItem {
                    mainTitle = strings["activityPass.fullscreen.title"]
                    mainBody = strings["activityPass.fullscreen.explanation"]
                    mainGravity = Gravity.CENTER
                    actions = listOfNotNull(readMoreAction)
                    identifier = "activityPass.fullscreen.title".hashCode().toLong()
                }
            }
            ActivityPassState.NonEligible -> {
                items += cardWithActionItem {
                    mainTitle = strings["activityPass.fullscreen.title"]
                    mainBody = strings["activityPass.fullscreen.notValid.message"]
                    mainGravity = Gravity.CENTER
                    identifier = "activityPass.fullscreen.title".hashCode().toLong()
                }
            }
            is ActivityPassState.Future -> {
                val dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT)
                val formattedDate = dateFormat.format(Date(activityPassState.activityPass.timestamp))

                items += cardWithActionItem {
                    mainTitle = strings["activityPass.fullscreen.title"]
                    mainBody = stringsFormat("activityPass.fullscreen.notAvailable.message", formattedDate)
                    mainGravity = Gravity.CENTER
                    identifier = "activityPass.fullscreen.title".hashCode().toLong()
                }
                items += buttonItem {
                    text = strings["activityPass.fullscreen.notAvailable.button.notify.title"]
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    onClickListener = View.OnClickListener {
                        certificate?.id?.let { certificateId ->
                            ActivityPassAvailableNotificationWorker.triggerActivityPassAvailableNotificationWorker(
                                requireContext(),
                                certificateId,
                                activityPassState.activityPass.timestamp,
                            )
                            Toast.makeText(requireContext(), strings["common.notifyMe.feedback"], Toast.LENGTH_SHORT).show()
                        }
                    }
                    identifier = "activityPass.fullscreen.notAvailable.button.notify.title".hashCode().toLong()
                }
                items += captionItem {
                    text = stringsFormat("activityPass.fullscreen.notAvailable.footer.notify", formattedDate)
                    textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
                    identifier = "activityPass.fullscreen.notAvailable.footer.notify".hashCode().toLong()
                }
            }
            ActivityPassState.EligibleRenewFail -> {
                items += cardWithActionItem {
                    mainTitle = strings["activityPass.fullscreen.title"]
                    mainBody = strings["activityPass.fullscreen.serverUnavailable.message"]
                    mainGravity = Gravity.CENTER
                    identifier = "activityPass.fullscreen.title".hashCode().toLong()
                }
            }
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        return items
    }

    private sealed class ActivityPassState {
        object Eligible : ActivityPassState()
        object NonEligible : ActivityPassState()
        class Future(val activityPass: EuropeanCertificate) : ActivityPassState()
        object EligibleRenewFail : ActivityPassState()
    }

    companion object {
        private const val CERTIFICATE_ID_ARG_KEY = "CERTIFICATE_ID_ARG_KEY"

        fun newInstance(id: String): WalletFullscreenActivityPassExplanationFragment {
            return WalletFullscreenActivityPassExplanationFragment().apply {
                arguments = bundleOf(
                    CERTIFICATE_ID_ARG_KEY to id,
                )
            }
        }
    }
}