package com.lunabeestudio.stopcovid.extension

import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment

fun BaseFragment.showUnknownErrorAlert(listener: DialogInterface.OnDismissListener?) {
    context?.let { context ->
        MaterialAlertDialogBuilder(context)
            .setTitle(strings["common.error.unknown"])
            .setPositiveButton(strings["common.ok"], null)
            .setOnDismissListener(listener)
            .show()
    }
}
