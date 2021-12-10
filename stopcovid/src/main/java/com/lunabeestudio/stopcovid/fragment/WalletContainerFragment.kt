/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/29/10 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.analytics.model.ErrorEventName
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.domain.model.WalletCertificateError
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentWalletContainerBinding
import com.lunabeestudio.stopcovid.extension.analyticsManager
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.showDbFailure
import com.lunabeestudio.stopcovid.extension.showMigrationFailed
import com.lunabeestudio.stopcovid.extension.splitUrlFragment
import com.lunabeestudio.stopcovid.extension.toRaw
import com.lunabeestudio.stopcovid.manager.DeeplinkManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificateMalformedException
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import kotlinx.coroutines.launch

class WalletContainerFragment : BaseFragment(), DeeplinkFragment {

    private val args: WalletContainerFragmentArgs by navArgs()

    private lateinit var binding: FragmentWalletContainerBinding

    var dbFailureDialog: AlertDialog? = null

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val analyticsManager by lazy {
        requireContext().analyticsManager()
    }

    private val viewModel by navGraphViewModels<WalletViewModel>(R.id.nav_wallet) {
        WalletViewModelFactory(
            robertManager,
            injectionContainer.blacklistDCCManager,
            injectionContainer.blacklist2DDOCManager,
            injectionContainer.walletRepository,
            injectionContainer.generateActivityPassUseCase,
            injectionContainer.getSmartWalletCertificateUseCase,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injectionContainer.debugManager.logOpenWalletContainer(viewModel.certificates.value.toRaw())

        args.navCertificateId?.let { id ->
            navigateToFullscreenEuropeanCertificateId(id, true)
        }

        if (savedInstanceState == null) { // do not replay navigation on config change
            lifecycleScope.launch {
                val rawCodeHandled = handleRawCode(
                    args.code,
                    args.certificateFormat?.let { WalletCertificateType.Format.fromValue(it) },
                    args.origin
                )
                if (!rawCodeHandled) {
                    showDbFailureIfNeeded(false)
                }
            }
        }

        setupResultListener()
    }

    private suspend fun handleRawCode(
        rawCode: String?,
        certificateFormat: WalletCertificateType.Format?,
        origin: DeeplinkManager.Origin?,
        skipMaxWarning: Boolean = false,
    ): Boolean {
        val code = rawCode?.splitUrlFragment()
        val certificateValue = code?.getOrNull(0)
        val reportCode = code?.getOrNull(1)

        val shouldAddCertificate = robertManager.configuration.displaySanitaryCertificatesWallet && certificateValue != null
        val shouldReport = reportCode != null && robertManager.isRegistered
        val shouldShowMaxCertificatesWarning =
            shouldAddCertificate &&
                !skipMaxWarning &&
                viewModel.getCertificatesCount() >= robertManager.configuration.maxCertBeforeWarning

        when {
            shouldShowMaxCertificatesWarning -> {
                setFragmentResultListener(WalletQuantityWarningFragment.MAX_CERTIFICATES_CONFIRM_ADD_RESULT_KEY) { _, bundle ->
                    val confirmAdd = bundle.getBoolean(WalletQuantityWarningFragment.MAX_CERTIFICATES_CONFIRM_ADD_BUNDLE_KEY, false)
                    if (confirmAdd) {
                        lifecycleScope.launch {
                            handleRawCode(rawCode, certificateFormat, origin, true)
                        }
                    }
                }
                findNavControllerOrNull()?.safeNavigate(
                    WalletContainerFragmentDirections.actionWalletContainerFragmentToWalletQuantityWarningFragment()
                )
            }
            shouldAddCertificate && shouldReport -> findNavControllerOrNull()?.safeNavigate(
                WalletContainerFragmentDirections.actionWalletContainerFragmentToPositiveTestStepsFragment(
                    positiveTestDccValue = certificateValue!!,
                    reportCode = reportCode!!,
                )
            )
            shouldAddCertificate && !shouldReport -> {
                findNavControllerOrNull()?.safeNavigate(
                    WalletContainerFragmentDirections.actionWalletContainerFragmentToConfirmAddWalletCertificateFragment(
                        certificateValue!!,
                        certificateFormat?.name,
                    )
                )
            }
            !shouldAddCertificate && shouldReport -> findNavControllerOrNull()?.safeNavigate(
                WalletContainerFragmentDirections.actionWalletContainerFragmentToSymptomsOriginFragment(
                    code = reportCode!!,
                )
            )
            !shouldAddCertificate && !shouldReport && !robertManager.configuration.displaySanitaryCertificatesWallet ->
                findNavControllerOrNull()?.navigateUp()
            else -> return false
        }

        return true
    }

    private fun setupResultListener() {
        setFragmentResultListener(WalletQRCodeFragment.SCANNED_CODE_RESULT_KEY) { _, bundle ->
            val scannedData = bundle.getString(WalletQRCodeFragment.SCANNED_CODE_BUNDLE_KEY)
            scannedData?.let { data ->
                if (URLUtil.isValidUrl(data)) {
                    try {
                        val certificateData = injectionContainer.walletRepository.extractCertificateDataFromUrl(data)
                        lifecycleScope.launch {
                            handleRawCode(
                                certificateData.first,
                                certificateData.second,
                                null,
                            )
                        }
                    } catch (e: WalletCertificateMalformedException) {
                        findNavControllerOrNull()?.safeNavigate(
                            WalletContainerFragmentDirections.actionWalletContainerFragmentToWalletCertificateErrorFragment(
                                WalletCertificateType.VACCINATION_EUROPE,
                                WalletCertificateError.MALFORMED_CERTIFICATE,
                            )
                        )
                    }
                } else {
                    lifecycleScope.launch {
                        handleRawCode(data, null, null)
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentWalletContainerBinding.inflate(inflater, container, false)
        binding.walletBottomSheetButton.setOnClickListener {
            findNavControllerOrNull()
                ?.safeNavigate(WalletContainerFragmentDirections.actionWalletContainerFragmentToWalletQRCodeFragment())
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModelObserver()
    }

    private fun initViewModelObserver() {
        viewModel.migrationInProgress.observe(viewLifecycleOwner) { migrationInProgress ->
            (activity as? MainActivity)?.showProgress(migrationInProgress)
            if (!migrationInProgress) {
                showMigrationFailedIfNeeded()
            }
        }
    }

    override fun refreshScreen() {
        appCompatActivity?.supportActionBar?.title = strings["walletController.title"]
        binding.walletBottomSheetButton.text = strings["walletController.addCertificate"]
    }

    private fun showMigrationFailedIfNeeded() {
        if (viewModel.migrationInProgress.value == false && injectionContainer.debugManager.oldCertificateInSharedPrefs()) {
            context?.let {
                MaterialAlertDialogBuilder(it).showMigrationFailed(strings) {
                    viewModel.deleteDeprecatedCertificates()
                }
            }
        }
    }

    fun navigateToFullscreenCertificate(certificate: WalletCertificate) {
        if (certificate is EuropeanCertificate) {
            navigateToFullscreenEuropeanCertificateId(certificate.id, false)
        } else {
            findNavControllerOrNull()?.safeNavigate(
                WalletContainerFragmentDirections.actionWalletContainerFragmentToFullscreenQRCodeFragment(certificate.id)
            )
        }
    }

    private fun navigateToFullscreenEuropeanCertificateId(certificateId: String, popSelf: Boolean) {
        val navOptions = if (popSelf) {
            NavOptions.Builder().setPopUpTo(R.id.walletContainerFragment, true).build()
        } else {
            null
        }

        if (robertManager.configuration.displayActivityPass) {
            findNavControllerOrNull()?.safeNavigate(
                WalletContainerFragmentDirections.actionWalletContainerFragmentToWalletFullscreenPagerFragment(
                    id = certificateId,
                ),
                navOptions,
            )
        } else {
            findNavControllerOrNull()?.safeNavigate(
                WalletContainerFragmentDirections.actionWalletContainerFragmentToLegacyFullscreenDccFragment(
                    id = certificateId,
                ),
                navOptions,
            )
        }
    }

    override fun onNewIntent(bundleArgs: Bundle?) {
        bundleArgs?.runCatching { WalletContainerFragmentArgs.fromBundle(this) }?.getOrNull()?.let { fragmentArgs ->
            lifecycleScope.launch {
                val certificateFormat = fragmentArgs.certificateFormat?.let { it ->
                    WalletCertificateType.Format.fromValue(it)
                }
                handleRawCode(fragmentArgs.code, certificateFormat, fragmentArgs.origin)
            }
        }
    }

    private fun showDbFailureIfNeeded(isRetry: Boolean) {
        if (dbFailureDialog == null) {
            lifecycleScope.launch {
                val result = viewModel.certificates.value

                // Analytics
                if (result is TacResult.Failure) {
                    if (isRetry) {
                        analyticsManager.reportErrorEvent(ErrorEventName.ERR_WALLET_DB_RETRY_FAILED)
                    } else {
                        analyticsManager.reportErrorEvent(ErrorEventName.ERR_WALLET_DB)
                    }
                } else if (result is TacResult.Success && isRetry) {
                    analyticsManager.reportErrorEvent(ErrorEventName.ERR_WALLET_DB_RETRY_SUCCEEDED)
                }

                if (result is TacResult.Failure) {
                    context?.let { ctx ->
                        dbFailureDialog = MaterialAlertDialogBuilder(ctx)
                            .setOnDismissListener {
                                if (it == dbFailureDialog) { // In case of concurrent dismiss, do not nullify another dialog instance
                                    dbFailureDialog = null
                                }
                            }
                            .showDbFailure(
                                strings,
                                onRetry = {
                                    dbFailureDialog?.dismiss()
                                    dbFailureDialog = null
                                    lifecycleScope.launch {
                                        viewModel.forceRefreshCertificates()
                                        showDbFailureIfNeeded(true)
                                    }
                                },
                                onClear = "android.db.error.clearWallet" to {
                                    dbFailureDialog?.dismiss()
                                    dbFailureDialog = null
                                    viewModel.deleteLostCertificates()
                                    injectionContainer.debugManager.logReinitializeWallet()
                                }
                            )
                    }
                }
            }
        }
    }
}
