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

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.DatePicker
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.databinding.DialogPostalCodeEditTextBinding
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.model.RisksUILevel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

fun MaterialAlertDialogBuilder.showPostalCodeDialog(
    layoutInflater: LayoutInflater,
    strings: LocalizedStrings,
    onPositiveButton: (String) -> Unit,
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

        if (result.isPostalCode() && KeyFiguresManager.figures.value?.peekContent()
            .postalCodeExists(result)
        ) {
            onPositiveButton(postalCodeEditTextBinding.textInputEditText.text.toString())
            dialog.dismiss()
        } else {
            postalCodeEditTextBinding.textInputLayout.error = strings["home.infoSection.newPostalCode.alert.wrongPostalCode"]
        }
    }
}

@SuppressLint("InflateParams")
fun MaterialAlertDialogBuilder.showSpinnerDayPicker(
    strings: LocalizedStrings,
    initialTimestamp: Long,
    dayInThePast: Int,
    onDatePicked: (Long) -> Unit,
) {
    val dateFormat: DateFormat = SimpleDateFormat.getDateInstance(DateFormat.FULL)

    val initialCalendar: Calendar = Calendar.getInstance().apply {
        timeInMillis = initialTimestamp
    }
    var newDate: Date = initialCalendar.time
    val numberPicker = LayoutInflater.from(context).inflate(
        R.layout.number_picker,
        null,
        false
    ) as NumberPicker
    // Fix crash https://issuetracker.google.com/issues/37055335
    numberPicker.isSaveEnabled = false
    numberPicker.isSaveFromParentEnabled = false

    numberPicker.minValue = 0
    numberPicker.maxValue = dayInThePast - 1
    val browsingCalendar: Calendar = Calendar.getInstance()
    numberPicker.displayedValues = (0 until dayInThePast).map { index ->
        if (index != 0) {
            browsingCalendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        dateFormat.format(browsingCalendar.time).capitalizeWords()
    }.toTypedArray()
    numberPicker.wrapSelectorWheel = false

    browsingCalendar.timeInMillis = System.currentTimeMillis()
    browsingCalendar.add(Calendar.DAY_OF_YEAR, 1)
    numberPicker.value = (numberPicker.minValue until numberPicker.maxValue).firstOrNull {
        browsingCalendar.add(Calendar.DAY_OF_YEAR, -1)
        browsingCalendar.get(Calendar.DAY_OF_YEAR) == initialCalendar.get(Calendar.DAY_OF_YEAR)
    } ?: 0

    numberPicker.setOnValueChangedListener { _, _, newVal ->
        val newCalendar = Calendar.getInstance()
        newCalendar.add(Calendar.DAY_OF_YEAR, -newVal)
        newDate = newCalendar.time
    }

    setView(numberPicker)
    setPositiveButton(strings["common.ok"]) { _, _ ->
        onDatePicked.invoke(newDate.time)
    }
    setNegativeButton(strings["common.cancel"], null)
    show()
}

@SuppressLint("InflateParams")
fun MaterialAlertDialogBuilder.showSpinnerDatePicker(
    strings: LocalizedStrings,
    initialTimestamp: Long,
    onDatePicked: (Long) -> Unit,
) {
    val initialCalendar: Calendar = Calendar.getInstance().apply {
        timeInMillis = initialTimestamp
    }
    var newDate: Date = initialCalendar.time
    val datePicker = LayoutInflater.from(context).inflate(
        R.layout.date_picker,
        null,
        false
    ) as DatePicker
    // Fix crash https://issuetracker.google.com/issues/37055335
    datePicker.isSaveEnabled = false
    datePicker.isSaveFromParentEnabled = false
    datePicker.init(
        initialCalendar.get(Calendar.YEAR),
        initialCalendar.get(Calendar.MONTH),
        initialCalendar.get(Calendar.DAY_OF_MONTH)
    ) { _, year, month, dayOfMonth ->
        val newCalendar = Calendar.getInstance()
        newCalendar.set(Calendar.YEAR, year)
        newCalendar.set(Calendar.MONTH, month)
        newCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        newDate = newCalendar.time
    }
    datePicker.maxDate = System.currentTimeMillis()

    setView(datePicker)
    setPositiveButton(strings["common.ok"]) { _, _ ->
        onDatePicked.invoke(newDate.time)
    }
    setNegativeButton(strings["common.cancel"], null)
    show()
}

fun MaterialAlertDialogBuilder.showSymptomConfirmationDialog(
    strings: LocalizedStrings,
    onConfirmation: (Boolean) -> Unit,
) {
    setTitle(strings["isolation.symptoms.alert.title"])
    setMessage(strings["isolation.symptoms.alert.message"])
    setPositiveButton(strings["isolation.symptoms.alert.yes"]) { _, _ ->
        onConfirmation.invoke(true)
    }
    setNegativeButton(strings["isolation.symptoms.alert.no"]) { _, _ ->
        onConfirmation.invoke(false)
    }
    setNeutralButton(strings["isolation.symptoms.alert.readMore"]) { _, _ ->
        strings["myHealthController.covidAdvices.url"]?.openInExternalBrowser(context)
    }
    show()
}

fun MaterialAlertDialogBuilder.showAlertRiskLevelChanged(
    strings: LocalizedStrings,
    sharedPreferences: SharedPreferences,
    risksUILevel: RisksUILevel?,
) {
    sharedPreferences.alertRiskLevelChanged = false
    if (risksUILevel != null
        && risksUILevel.labels.notifTitle != null
        && risksUILevel.labels.notifBody != null
    ) {
        setTitle(strings[risksUILevel.labels.notifTitle])
        setMessage(strings[risksUILevel.labels.notifBody])
        setPositiveButton(strings["common.ok"], null)
        setCancelable(false)
        show()
    }
}

fun MaterialAlertDialogBuilder.showAlertSickVenue(
    strings: LocalizedStrings,
    onIgnore: (() -> Unit)?,
    onCancel: (() -> Unit)?,
) {
    setTitle(strings["home.venuesSection.sickAlert.title"])
    setMessage(strings["home.venuesSection.sickAlert.message"])
    setPositiveButton(strings["home.venuesSection.sickAlert.positiveButton"], onIgnore?.let { { _, _ -> it() } })
    setNegativeButton(strings["home.venuesSection.sickAlert.negativeButton"], onCancel?.let { { _, _ -> it() } })
    setCancelable(false)
    show()
}