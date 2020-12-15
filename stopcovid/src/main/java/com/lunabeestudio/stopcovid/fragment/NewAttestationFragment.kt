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

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.domain.model.FormEntry
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.hideSoftKeyBoard
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.switchItem
import com.lunabeestudio.stopcovid.extension.attestationLabelFromKey
import com.lunabeestudio.stopcovid.extension.attestationPlaceholderFromKey
import com.lunabeestudio.stopcovid.extension.attestationShortLabelFromKey
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.showSpinnerDatePicker
import com.lunabeestudio.stopcovid.fastitem.editTextItem
import com.lunabeestudio.stopcovid.fastitem.pickerEditTextItem
import com.lunabeestudio.stopcovid.manager.FormManager
import com.lunabeestudio.stopcovid.model.AttestationMap
import com.lunabeestudio.stopcovid.model.FormField
import com.lunabeestudio.stopcovid.viewmodel.NewAttestationViewModel
import com.lunabeestudio.stopcovid.viewmodel.NewAttestationViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NewAttestationFragment : MainFragment() {

    private val gson: Gson = Gson()
    private val viewModel: NewAttestationViewModel by activityViewModels { NewAttestationViewModelFactory(requireContext().secureKeystoreDataSource()) }
    private val dateFormat: DateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG)
    private val dateTimeFormat: DateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.getDefault())

    override fun getTitleKey(): String = "newAttestationController.title"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        if (savedInstanceState != null) {
            viewModel.infos.clear()
            viewModel.infos.putAll(
                gson.fromJson(
                    savedInstanceState.getString(SAVE_INSTANCE_ATTESTATION_INFOS),
                    object : TypeToken<AttestationMap>() {}.type
                )
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FormManager.form.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }

        findNavControllerOrNull()?.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.attestationsFragment) {
                viewModel.resetInfos()
                activity?.hideSoftKeyBoard()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.text_menu, menu)
        menu.findItem(R.id.item_text).title = strings["newAttestationController.generate"]
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.item_text) {
            if (viewModel.areInfosValid()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["newAttestationController.generate.alert.title"])
                    .setMessage(strings["newAttestationController.generate.alert.message"])
                    .setPositiveButton(strings["newAttestationController.generate.alert.validate"]) { _, _ ->
                        viewModel.generateQrCode()
                        findNavControllerOrNull()?.navigateUp()
                    }
                    .setNegativeButton(strings["common.cancel"], null)
                    .show()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["newAttestationController.missingInfo.alert.title"])
                    .setMessage(strings["newAttestationController.missingInfo.alert.message"])
                    .setPositiveButton(strings["common.ok"], null)
                    .show()
            }
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun refreshScreen() {
        super.refreshScreen()
        activity?.invalidateOptionsMenu()
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        strings["newAttestationController.header"]?.takeIf { it.isNotBlank() }?.let { header ->
            items += captionItem {
                text = header
                textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
                identifier = "newAttestationController.header".hashCode().toLong()
            }
        }
        FormManager.form.value?.peekContent()?.let { form ->
            form.forEach { section ->
                section.forEach { formField ->
                    items += itemForFormField(formField).apply {
                        identifier = formField.key.hashCode().toLong()
                    }
                }
                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.count().toLong()
                }
            }
        }

        items += switchItem {
            title = strings["newAttestationController.saveMyData"]
            isChecked = viewModel.shouldSaveInfos
            onCheckChange = { checked ->
                isChecked = checked
                viewModel.shouldSaveInfos = checked
            }
            identifier = "newAttestationController.saveMyData".hashCode().toLong()
        }
        items += captionItem {
            text = strings["newAttestationController.footer"]
            textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
            identifier = "newAttestationController.footer".hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        return items
    }

    private fun itemForFormField(formField: FormField): GenericItem {
        return when (formField.type) {
            "date" -> pickerEditTextItem {
                placeholder = strings[formField.key.attestationPlaceholderFromKey()]
                hint = strings[formField.key.attestationLabelFromKey()]
                text = viewModel.infos[formField.key]?.value?.toLongOrNull()?.let { timestamp ->
                    dateFormat.format(Date(timestamp))
                }
                onClick = {
                    val initialTimestamp = viewModel.infos[formField.key]?.value?.toLongOrNull()
                        ?: Calendar.getInstance().apply {
                            timeInMillis = System.currentTimeMillis()
                            set(Calendar.YEAR, get(Calendar.YEAR) - 18)
                        }.timeInMillis
                    MaterialAlertDialogBuilder(requireContext()).showSpinnerDatePicker(strings, initialTimestamp) { newDate ->
                        text = dateFormat.format(Date(newDate))
                        viewModel.infos[formField.key] = FormEntry(newDate.toString(), formField.type)
                        binding?.recyclerView?.adapter?.notifyDataSetChanged()
                    }
                }
            }
            "datetime" -> {
                if (viewModel.infos[formField.key] == null) {
                    viewModel.infos[formField.key] = FormEntry(System.currentTimeMillis().toString(), formField.type)
                }
                pickerEditTextItem {
                    placeholder = strings[formField.key.attestationPlaceholderFromKey()]
                    hint = strings[formField.key.attestationLabelFromKey()]
                    text = viewModel.infos[formField.key]?.value?.toLongOrNull()?.let { timestamp ->
                        dateTimeFormat.format(Date(timestamp))
                    }
                    onClick = {
                        val initialTimestamp = viewModel.infos[formField.key]?.value?.toLongOrNull()
                            ?: System.currentTimeMillis()
                        showDateTimePicker(initialTimestamp) { newDate ->
                            text = dateTimeFormat.format(Date(newDate))
                            viewModel.infos[formField.key] = FormEntry(newDate.toString(), formField.type)
                            binding?.recyclerView?.adapter?.notifyDataSetChanged()
                        }
                    }
                }
            }
            "list" -> pickerEditTextItem {
                placeholder = strings[formField.key.attestationPlaceholderFromKey()]
                hint = strings[formField.key.attestationLabelFromKey()]
                text = strings[viewModel.infos[formField.key]?.value?.attestationShortLabelFromKey()]
                onClick = {
                    findNavControllerOrNull()?.safeNavigate(
                        NewAttestationFragmentDirections.actionNewAttestationFragmentToNewAttestationPickerFragment(
                            formField.key,
                            viewModel.infos[formField.key]?.value
                        )
                    )
                }
            }
            else -> editTextItem {
                placeholder = strings[formField.key.attestationPlaceholderFromKey()]
                hint = strings[formField.key.attestationLabelFromKey()]
                text = viewModel.infos[formField.key]?.value
                textInputType = when (formField.type) {
                    "text" -> when (formField.contentType) {
                        "firstName", "lastName" -> EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME
                        "addressLine1", "addressCity" -> EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
                        else -> EditorInfo.TYPE_CLASS_TEXT
                    }
                    "number" -> EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_NORMAL
                    else -> EditorInfo.TYPE_CLASS_TEXT
                } or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
                textImeOptions = EditorInfo.IME_ACTION_NEXT
                onTextChange = { newValue ->
                    text = newValue.toString()
                    viewModel.infos[formField.key] = FormEntry(newValue.toString(), formField.type)
                }
            }
        }
    }

    // Adapted from here https://stackoverflow.com/a/35745881/2794437
    private fun showDateTimePicker(initialTimestamp: Long, onDatePicked: (Long) -> Unit) {
        val currentDate = Calendar.getInstance().apply {
            timeInMillis = initialTimestamp
        }
        val date = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { datePicker, year, monthOfYear, dayOfMonth ->
                // Fix crash https://issuetracker.google.com/issues/37055335
                datePicker.isSaveFromParentEnabled = false
                datePicker.isSaveEnabled = false
                date.set(year, monthOfYear, dayOfMonth)
                TimePickerDialog(context, { timePicker, hourOfDay, minute ->
                    // Fix crash https://issuetracker.google.com/issues/37055335
                    timePicker.isSaveFromParentEnabled = false
                    timePicker.isSaveEnabled = false
                    date.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    date.set(Calendar.MINUTE, minute)
                    onDatePicked.invoke(date.timeInMillis)
                }, currentDate[Calendar.HOUR_OF_DAY], currentDate[Calendar.MINUTE], true)
                    .show()
            },
            currentDate[Calendar.YEAR],
            currentDate[Calendar.MONTH],
            currentDate[Calendar.DATE]
        )
        datePickerDialog.datePicker.apply {
            minDate = System.currentTimeMillis()
        }
        datePickerDialog.setTitle("")
        datePickerDialog.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SAVE_INSTANCE_ATTESTATION_INFOS, gson.toJson(viewModel.infos))
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val SAVE_INSTANCE_ATTESTATION_INFOS: String = "Save.Instance.Attestation.Info"
    }
}