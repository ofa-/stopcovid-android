package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.analytics.model.ErrorEventName
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.extension.safeEnumValueOf
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentConfirmAddWalletCertificateBinding
import com.lunabeestudio.stopcovid.extension.analyticsManager
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.isFrench
import com.lunabeestudio.stopcovid.extension.isSignatureExpired
import com.lunabeestudio.stopcovid.extension.raw
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.showDbFailure
import com.lunabeestudio.stopcovid.extension.showUnknownErrorAlert
import com.lunabeestudio.stopcovid.extension.smartWalletState
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.extension.walletCertificateError
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.Expired
import com.lunabeestudio.stopcovid.model.SecretKeyAlreadyGeneratedException
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfirmAddWalletCertificateFragment : BaseFragment() {

    private val args: ConfirmAddWalletCertificateFragmentArgs by navArgs()

    private var binding: FragmentConfirmAddWalletCertificateBinding? = null

    private var dbFailureDialog: AlertDialog? = null

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

    private var certificate: WalletCertificate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            certificate = validateAndGetCertificate(
                certificateCode = args.certificateCode,
                certificateFormat = args.certificateFormat?.let { safeEnumValueOf<WalletCertificateType.Format>(it) }
            )
            refreshScreen()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentConfirmAddWalletCertificateBinding.inflate(inflater, container, false)
        (activity as? MainActivity)?.showProgress(true)
        return binding?.root!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if ((activity as? MainActivity)?.binding?.tabLayout?.isVisible == true) {
            postponeEnterTransition()
            (activity as? MainActivity)?.binding?.appBarLayout?.doOnNextLayout {
                startPostponedEnterTransition()
            }
            (activity as? MainActivity)?.binding?.tabLayout?.isVisible = false
        }
    }

    override fun refreshScreen() {
        appCompatActivity?.supportActionBar?.title = strings["confirmWalletQrCodeController.title"]

        binding?.apply {
            certificate?.let { certificate ->
                walletCaptionTextView.textView.setTextOrHide(strings["confirmWalletQrCodeController.explanation.title"])
                walletCaptionTextView.textView.gravity = Gravity.CENTER

                walletTitleTextView.textSwitcher.setText(
                    strings["confirmWalletQrCodeController.explanation.subtitle"]?.safeEmojiSpanify()
                )
                walletTitleTextView.textView1.gravity = Gravity.CENTER
                walletTitleTextView.textView2.gravity = Gravity.CENTER

                walletAddButton.setTextOrHide(strings["confirmWalletQrCodeController.confirm"])
                walletAddButton.setOnClickListener {
                    lifecycleScope.launch {
                        checkAndProcessCertificate(certificate)
                    }
                }

                walletCancelButton.setTextOrHide(strings["common.cancel"])
                walletCancelButton.setOnClickListener {
                    findNavControllerOrNull()?.navigateUp()
                }

                (activity as? MainActivity)?.showProgress(false)
                walletConfirmContentGroup.isVisible = true
            }
        }
    }

    private suspend fun checkAndProcessCertificate(certificate: WalletCertificate) {
        val dcc = (certificate as? EuropeanCertificate)
        showDuplicateOrBlacklistedWarningIfNeeded(certificate) {
            showAlertForeignDccIfNeeded(dcc) {
                lifecycleScope.launch {
                    processCertificate(certificate)
                }
            }
        }
    }

    private suspend fun validateAndGetCertificate(
        certificateCode: String,
        certificateFormat: WalletCertificateType.Format?
    ): WalletCertificate? {
        return try {
            injectionContainer.verifyAndGetCertificateCodeValueUseCase(
                certificateCode,
                certificateFormat,
            )
        } catch (e: Exception) {
            (activity as? MainActivity)?.showProgress(false)
            handleCertificateError(e, certificateCode, null)
            null
        }
    }

    private suspend fun processCertificate(certificate: WalletCertificate) {
        try {
            (activity as? MainActivity)?.showProgress(true)
            viewModel.saveCertificate(certificate)
            injectionContainer.debugManager.logSaveCertificates(certificate.raw, "from wallet")

            val europeanCertificate = certificate as? EuropeanCertificate
            val vaccination = (certificate as? EuropeanCertificate)?.greenCertificate?.vaccinations?.lastOrNull()

            val showVaccineCompletion = vaccination != null
                && vaccination.doseNumber >= vaccination.totalSeriesOfDoses
                && !viewModel.isBlacklisted(certificate)
                && europeanCertificate?.smartWalletState(robertManager.configuration) !is Expired
                && europeanCertificate?.isSignatureExpired == false

            if (showVaccineCompletion) {
                findNavControllerOrNull()?.safeNavigate(
                    ConfirmAddWalletCertificateFragmentDirections.actionConfirmAddWalletCertificateFragmentToVaccineCompletionFragment(
                        certificate.id
                    )
                )
            } else {
                strings["walletController.addCertificate.addSucceeded"]?.let {
                    (activity as? MainActivity)?.showSnackBar(it)
                }
                findNavControllerOrNull()?.navigateUp()
            }
        } catch (e: Exception) {
            handleCertificateError(e, certificate.type.code, certificate)
        } finally {
            (activity as? MainActivity)?.showProgress(false)
        }
    }

    private suspend fun showDuplicateOrBlacklistedWarningIfNeeded(
        certificate: WalletCertificate,
        onContinue: () -> Unit,
    ) {
        val isDuplicated = viewModel.isDuplicated(certificate)
        val isBlacklisted = viewModel.isBlacklisted(certificate)

        if (isDuplicated || isBlacklisted) {
            val messages = mutableListOf<String>()
            if (isDuplicated) {
                messages += strings["walletController.alert.duplicatedCertificate.subtitle"].orEmpty()
            }
            if (isBlacklisted) {
                messages += strings["wallet.blacklist.warning"].orEmpty()
            }

            val confirm = if (isDuplicated) {
                strings["walletController.alert.duplicatedCertificate.confirm"]
            } else {
                strings["walletController.addForeignCertificate.alert.add"]
            }

            withContext(Dispatchers.Main) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["common.warning"])
                    .setMessage(messages.joinToString("\n\n"))
                    .setPositiveButton(confirm) { _, _ ->
                        onContinue()
                    }
                    .setNegativeButton(strings["common.cancel"]) { _, _ ->
                        findNavControllerOrNull()?.navigateUp()
                    }
                    .show()
            }
        } else {
            onContinue()
        }
    }

    private fun showAlertForeignDccIfNeeded(
        dcc: EuropeanCertificate?,
        onContinue: () -> Unit,
    ) {
        if (dcc != null && !dcc.greenCertificate.isFrench) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(strings["common.warning"])
                .setMessage(strings["walletController.addForeignCertificate.alert.message"])
                .setPositiveButton(strings["walletController.addForeignCertificate.alert.add"]) { _, _ ->
                    onContinue()
                }
                .setNegativeButton(strings["common.cancel"], null)
                .show()
        } else {
            onContinue()
        }
    }

    private suspend fun handleCertificateError(error: Exception, certificateCode: String?, certificate: WalletCertificate?) {
        val certificateType = certificateCode?.let { WalletCertificate.getTypeFromValue(it) } ?: WalletCertificateType.VACCINATION_EUROPE
        handleCertificateError(error, certificateType, certificate)
    }

    private fun handleCertificateError(error: Exception, certificateType: WalletCertificateType, certificate: WalletCertificate?) {
        val certificateError = error.walletCertificateError()
        when {
            certificateError != null -> {
                findNavControllerOrNull()?.safeNavigate(
                    ConfirmAddWalletCertificateFragmentDirections.actionConfirmAddWalletCertificateFragmentToWalletCertificateErrorFragment(
                        certificateType,
                        certificateError,
                    )
                )
            }
            (error as? RobertException)?.toCovidException() is SecretKeyAlreadyGeneratedException && certificate != null -> {
                // Dismiss db failure dialog which might be trigger by db read failure
                dbFailureDialog?.dismiss()
                dbFailureDialog = null
                showDbFailureIfNeeded(false, certificate)
            }
            else -> showUnknownErrorAlert(null)
        }
    }

    private fun showDbFailureIfNeeded(isRetry: Boolean, certificate: WalletCertificate?) {
        if (dbFailureDialog == null) {
            lifecycleScope.launch {
                val result = viewModel.certificates.value

                val resetKeystoreAndSaveCertificate = { certificate: WalletCertificate ->
                    kotlin.runCatching {
                        lifecycleScope.launch {
                            viewModel.resetWalletCryptoKeyGeneratedFlag()
                            viewModel.saveCertificate(certificate)
                            viewModel.deleteLostCertificates() // key has been override, no more chance to recover
                            processCertificate(certificate) // re-process without checks
                        }
                    }.onFailure {
                        showDbFailureIfNeeded(true, certificate)
                    }
                }

                // Analytics
                // certificate != null -> error during save certificate flow
                if (result is TacResult.Failure || certificate != null) {
                    if (isRetry) {
                        analyticsManager.reportErrorEvent(ErrorEventName.ERR_WALLET_DB_RETRY_FAILED)
                    } else {
                        analyticsManager.reportErrorEvent(ErrorEventName.ERR_WALLET_DB)
                    }
                } else if (result is TacResult.Success && isRetry) {
                    analyticsManager.reportErrorEvent(ErrorEventName.ERR_WALLET_DB_RETRY_SUCCEEDED)
                }

                // result will always be Success if the wallet is empty, so also check for certificate saving flow (certificate != null)
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
                                        if (certificate == null) {
                                            showDbFailureIfNeeded(true, certificate)
                                        } else {
                                            kotlin.runCatching {
                                                viewModel.saveCertificate(certificate)
                                            }.onFailure {
                                                showDbFailureIfNeeded(true, certificate)
                                            }
                                        }
                                    }
                                },
                                onClear = "android.db.error.clearWallet" to {
                                    dbFailureDialog?.dismiss()
                                    dbFailureDialog = null
                                    if (certificate != null) {
                                        resetKeystoreAndSaveCertificate(certificate)
                                    } else {
                                        viewModel.deleteLostCertificates()
                                    }

                                    injectionContainer.debugManager.logReinitializeWallet()
                                }
                            )
                    }
                } else if (result is TacResult.Success && certificate != null) {
                    // If the user wants to add a new certificate, but
                    //  • We lost the keystore
                    //  • There no certificate in the wallet (result is TacResult.Success)
                    // -> Silently reset the key generated flag and add the certificate. Else show the dbFailureDialog
                    resetKeystoreAndSaveCertificate(certificate)
                }
            }
        }
    }
}