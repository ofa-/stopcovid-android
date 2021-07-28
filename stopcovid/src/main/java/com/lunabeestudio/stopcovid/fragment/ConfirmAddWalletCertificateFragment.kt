package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentConfirmAddWalletCertificateBinding

class ConfirmAddWalletCertificateFragment : BaseFragment() {

    private val args: ConfirmAddWalletCertificateFragmentArgs by navArgs()

    private lateinit var binding: FragmentConfirmAddWalletCertificateBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentConfirmAddWalletCertificateBinding.inflate(inflater, container, false)
        return binding.root
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

        binding.walletCaptionTextView.textView.setTextOrHide(strings["confirmWalletQrCodeController.explanation.title"])
        binding.walletCaptionTextView.textView.gravity = Gravity.CENTER

        binding.walletTitleTextView.textSwitcher.setText(strings["confirmWalletQrCodeController.explanation.subtitle"]?.safeEmojiSpanify())
        binding.walletTitleTextView.textView1.gravity = Gravity.CENTER
        binding.walletTitleTextView.textView2.gravity = Gravity.CENTER

        binding.walletAddButton.setTextOrHide(strings["confirmWalletQrCodeController.confirm"])
        binding.walletAddButton.setOnClickListener {
            setFragmentResult(
                WalletContainerFragment.CONFIRM_ADD_CODE_RESULT_KEY,
                bundleOf(
                    WalletContainerFragment.CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM to true,
                    WalletContainerFragment.CONFIRM_ADD_CODE_BUNDLE_KEY_CODE to args.certificateCode,
                    WalletContainerFragment.CONFIRM_ADD_CODE_BUNDLE_KEY_FORMAT to args.certificateFormat,
                )
            )
            findNavControllerOrNull()?.navigateUp()
        }

        binding.walletCancelButton.setTextOrHide(strings["common.cancel"])
        binding.walletCancelButton.setOnClickListener {
            findNavControllerOrNull()?.navigateUp()
        }
    }
}