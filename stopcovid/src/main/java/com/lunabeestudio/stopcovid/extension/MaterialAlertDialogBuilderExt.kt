/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/29 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.databinding.DialogPostalCodeEditTextBinding
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager

fun MaterialAlertDialogBuilder.showPostalCodeDialog(
    layoutInflater: LayoutInflater,
    strings: Map<String, String>,
    onPositiveButton: (String) -> Unit
) {
    val postalCodeEditTextBinding = DialogPostalCodeEditTextBinding.inflate(layoutInflater)

    postalCodeEditTextBinding.textInputEditText.doOnTextChanged { _, _, _, _ ->
        postalCodeEditTextBinding.textInputLayout.error = null
    }

    postalCodeEditTextBinding.textInputLayout.hint = strings["home.infoSection.newPostalCode.alert.placeholder"]
    setTitle(strings["home.infoSection.newPostalCode.alert.title"])
    setMessage(strings["home.infoSection.newPostalCode.alert.subtitle"])
    setView(postalCodeEditTextBinding.textInputLayout)
    setPositiveButton(strings["common.confirm"]) { _, _ ->
        // Nothing to do here => we want to prevent dialog dismiss if error
    }
    setCancelable(false)
    setNegativeButton(strings["common.cancel"], null)
    val dialog = show()

    val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

    postalCodeEditTextBinding.textInputEditText.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            positiveButton.callOnClick()
            true
        } else {
            false
        }
    }

    positiveButton.setOnClickListener {
        val result = postalCodeEditTextBinding.textInputEditText.text.toString()

        if (result.isPostalCode() && KeyFiguresManager.figures.value?.peekContent().postalCodeExists(result)) {
            onPositiveButton(postalCodeEditTextBinding.textInputEditText.text.toString())
            dialog.dismiss()
        } else {
            postalCodeEditTextBinding.textInputLayout.error = strings["home.infoSection.newPostalCode.alert.wrongPostalCode"]
        }
    }
}