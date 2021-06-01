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
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.hideSoftKeyBoard
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.switchItem
import com.lunabeestudio.stopcovid.extension.attestationLabel
import com.lunabeestudio.stopcovid.extension.attestationPlaceholder
import com.lunabeestudio.stopcovid.extension.attestationShortLabelFromKey
import com.lunabeestudio.stopcovid.extension.robertManager
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
import com.lunabeestudio.stopcovid.widgetshomescreen.AttestationWidget
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
    private val dateTimeFormat: DateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)

    private val robertManager: RobertManager by lazy {
        requireContext().robertManager()
    }

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
                        viewModel.generateQrCode(robertManager, strings)
                        findNavControllerOrNull()?.navigateUp()
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
                placeholder = strings[formField.attestationPlaceholder()]
                hint = strings[formField.attestationLabel()]
                text = viewModel.infos[formField.dataKeyValue]?.value?.toLongOrNull()?.let { timestamp ->
                    dateFormat.format(Date(timestamp))
                }
                onClick = {
                    val initialTimestamp = viewModel.infos[formField.dataKeyValue]?.value?.toLongOrNull()
                        ?: Calendar.getInstance().apply {
                            timeInMillis = System.currentTimeMillis()
                            set(Calendar.YEAR, get(Calendar.YEAR) - 18)
                        }.timeInMillis
                    MaterialAlertDialogBuilder(requireContext()).showSpinnerDatePicker(strings, initialTimestamp) { newDate ->
                        text = dateFormat.format(Date(newDate))
                        viewModel.infos[formField.dataKeyValue] = FormEntry(newDate.toString(), formField.type, formField.key)
                        binding?.recyclerView?.adapter?.notifyDataSetChanged()
                    }
                }
            }
            "datetime" -> {
                if (viewModel.infos[formField.dataKeyValue] == null) {
                    viewModel.infos[formField.dataKeyValue] = FormEntry(
                        System.currentTimeMillis().toString(),
                        formField.type,
                        formField.key
                    )
                }
                pickerEditTextItem {
                    placeholder = strings[formField.attestationPlaceholder()]
                    hint = strings[formField.attestationLabel()]
                    text = viewModel.infos[formField.dataKeyValue]?.value?.toLongOrNull()?.let { timestamp ->
                        dateTimeFormat.format(
                            Calendar.getInstance().apply {
                                timeInMillis = timestamp
                            }.time
                        )
                    }
                    onClick = {
                        val initialTimestamp = viewModel.infos[formField.dataKeyValue]?.value?.toLongOrNull()
                            ?: Calendar.getInstance().timeInMillis
                        showDateTimePicker(initialTimestamp) { newDate ->
                            text = dateTimeFormat.format(
                                Calendar.getInstance().apply {
                                    timeInMillis = newDate
                                }.time
                            )
                            viewModel.infos[formField.dataKeyValue] = FormEntry(newDate.toString(), formField.type, formField.key)
                            binding?.recyclerView?.adapter?.notifyDataSetChanged()
                        }
                    }
                }
            }
            "list" -> pickerEditTextItem {
                placeholder = strings[formField.attestationPlaceholder()]
                hint = strings[formField.attestationLabel()]
                text = strings[viewModel.getInfoForFormField(formField)?.value?.attestationShortLabelFromKey()]
                onClick = {
                    findNavControllerOrNull()?.safeNavigate(
                        NewAttestationFragmentDirections.actionNewAttestationFragmentToNewAttestationPickerFragment(
                            formField.key,
                            formField.dataKeyValue,
                            viewModel.getInfoForFormField(formField)?.value
                        )
                    )
                }
            }
            else -> editTextItem {
                val currentValue = viewModel.infos[formField.dataKeyValue]
                placeholder = strings[formField.attestationPlaceholder()]
                hint = strings[formField.attestationLabel()]
                text = if (currentValue != null) {
                    viewModel.infos[formField.dataKeyValue]?.value
                } else {
                    viewModel.infos[formField.dataKeyValue] = FormEntry(formField.defaultValue, formField.type, formField.key)
                    formField.defaultValue
                }
                textInputType = when (formField.type) {
                    "text" -> when (formField.contentType) {
                        "firstName", "lastName" -> EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME
                        "addressLine1", "addressCity", "addressCountry" -> EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
                        else -> EditorInfo.TYPE_CLASS_TEXT
                    }
                    "number" -> EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_NORMAL
                    else -> EditorInfo.TYPE_CLASS_TEXT
                } or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
                textImeOptions = EditorInfo.IME_ACTION_NEXT
                onTextChange = { newValue ->
                    text = newValue.toString()
                    viewModel.infos[formField.dataKeyValue] = FormEntry(newValue.toString(), formField.type, formField.key)
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
