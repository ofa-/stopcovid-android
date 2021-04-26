package com.lunabeestudio.stopcovid.extension

import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.coreui.BuildConfig
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.model.WalletCertificateInvalidSignatureException
import com.lunabeestudio.stopcovid.model.WalletCertificateMalformedException
import com.lunabeestudio.stopcovid.model.WalletCertificateNoKeyError
import timber.log.Timber

fun BaseFragment.catchWalletException(e: Exception, listener: DialogInterface.OnClickListener? = null) {
    Timber.e(e)
    when (e) {
        is WalletCertificateInvalidSignatureException -> showInvalidCertificateSignatureAlert(listener)
        is WalletCertificateMalformedException -> showMalformedCertificateAlert(listener)
        is WalletCertificateNoKeyError -> showInvalidCertificateSignatureAlert(listener)
        else -> showUnknownErrorAlert(e, listener)
    }
}

private fun BaseFragment.showMalformedCertificateAlert(listener: DialogInterface.OnClickListener?) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(strings["wallet.proof.error.1.title"])
        .setMessage(strings["wallet.proof.error.1.message"])
        .setPositiveButton(strings["common.ok"], listener)
        .show()
}

private fun BaseFragment.showInvalidCertificateSignatureAlert(listener: DialogInterface.OnClickListener?) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(strings["wallet.proof.error.2.title"])
        .setMessage(strings["wallet.proof.error.2.message"])
        .setPositiveButton(strings["common.ok"], listener)
        .show()
}

private fun BaseFragment.showUnknownErrorAlert(e: Exception, listener: DialogInterface.OnClickListener?) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(strings["common.error.unknown"])
        .setPositiveButton(strings["common.ok"], listener)
        .show()
}
