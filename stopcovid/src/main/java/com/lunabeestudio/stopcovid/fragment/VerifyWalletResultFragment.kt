package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentVerifyWalletResultBinding
import com.lunabeestudio.stopcovid.extension.fullDescription
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.shortDescription
import com.lunabeestudio.stopcovid.manager.WalletManager
import com.lunabeestudio.stopcovid.model.WalletCertificate

class VerifyWalletResultFragment : BaseFragment() {

    private var binding: FragmentVerifyWalletResultBinding? = null
    private val args: VerifyWalletResultFragmentArgs by navArgs()
    private val titleKey: String = "flashDataMatrixCodeController.title"
    private var certificate: WalletCertificate? = null
    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        certificate = try {
            WalletManager.verifyCertificateCodeValue(sharedPreferences, robertManager.configuration, args.certificateCode)
        } catch (e: Exception) {
            null
        }

        binding = FragmentVerifyWalletResultBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun refreshScreen() {
        appCompatActivity?.supportActionBar?.title = strings[titleKey]

        binding?.textView?.text = certificate?.fullDescription(strings, robertManager.configuration)
        binding?.button?.text = strings["walletCertificateVerifiedController.validateAnotherProof"]
        binding?.button?.setOnClickListener {
            findNavControllerOrNull()?.safeNavigate(VerifyWalletResultFragmentDirections.actionVerifyWalletResultFragmentToVerifyWalletQRCodeFragment())
        }
    }
}