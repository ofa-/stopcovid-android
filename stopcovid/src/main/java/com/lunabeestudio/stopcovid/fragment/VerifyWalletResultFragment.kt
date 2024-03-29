package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentVerifyWalletResultBinding
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.fullDescription
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.walletRepository
import com.lunabeestudio.stopcovid.model.WalletCertificate
import kotlinx.coroutines.launch

class VerifyWalletResultFragment : BaseFragment() {

    private var binding: FragmentVerifyWalletResultBinding? = null
    private val args: VerifyWalletResultFragmentArgs by navArgs()
    private val titleKey: String = "flashDataMatrixCodeController.title"
    private var certificate: WalletCertificate? = null
    private val robertManager by lazy {
        requireContext().robertManager()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        lifecycleScope.launch {
            certificate = try {
                injectionContainer.verifyAndGetCertificateCodeValueUseCase(
                    args.certificateCode,
                    null,
                )
            } catch (e: Exception) {
                null
            }

            refreshScreen()
        }

        binding = FragmentVerifyWalletResultBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun refreshScreen() {
        appCompatActivity?.supportActionBar?.title = strings[titleKey]

        binding?.textView?.text = certificate?.fullDescription(strings, robertManager.configuration, requireContext(), null)
        binding?.button?.text = strings["walletCertificateVerifiedController.validateAnotherProof"]
        binding?.button?.setOnClickListener {
            findNavControllerOrNull()?.safeNavigate(
                VerifyWalletResultFragmentDirections.actionVerifyWalletResultFragmentToVerifyWalletQRCodeFragment()
            )
        }
    }
}
