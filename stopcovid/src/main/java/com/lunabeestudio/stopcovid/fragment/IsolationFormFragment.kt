/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/12/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.RadioGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.isolationManager
import com.lunabeestudio.stopcovid.extension.showSpinnerDayPicker
import com.lunabeestudio.stopcovid.extension.showSymptomConfirmationDialog
import com.lunabeestudio.stopcovid.fastitem.isolationBooleanRadioGroupItem
import com.lunabeestudio.stopcovid.fastitem.isolationStateRadioGroupItem
import com.lunabeestudio.stopcovid.fastitem.pickerEditTextItem
import com.lunabeestudio.stopcovid.manager.IsolationFormStateEnum
import com.lunabeestudio.stopcovid.model.IsolationRecommendationStateEnum
import com.lunabeestudio.stopcovid.viewmodel.IsolationFormViewModel
import com.lunabeestudio.stopcovid.viewmodel.IsolationFormViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class IsolationFormFragment : MainFragment() {

    private val dateFormat: DateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG)

    private val isolationManager by lazy {
        requireContext().isolationManager()
    }

    private val viewModel: IsolationFormViewModel by viewModels {
        IsolationFormViewModelFactory(isolationManager)
    }

    override fun getTitleKey(): String = "isolationFormController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModelObserver()
    }

    private fun initViewModelObserver() {
        viewModel.isolationFormState.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
            scrollToBottom()
        }

        viewModel.isolationDataChanged.observe(viewLifecycleOwner) {
            refreshScreen()
            scrollToBottom()
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        val header = strings["isolationFormController.header.title"]
        if (!header.isNullOrBlank()) {
            items += captionItem {
                text = header
                identifier = "isolationFormController.header.title".hashCode().toLong()
            }
        }

        items += isolationStateRadioGroupItem {
            groupTitle = strings["isolationFormController.state.sectionTitle"]
            allGoodLabel = strings["isolationFormController.state.allGood"]
            symptomsLabel = strings["isolationFormController.state.symptoms"]
            contactLabel = strings["isolationFormController.state.contactCase"]
            positiveLabel = strings["isolationFormController.state.positiveCase"]
            selectedState = isolationManager.currentFormState.value?.peekContent()
            onStateSymptomsClick = { radioGroup ->
                MaterialAlertDialogBuilder(requireContext()).showSymptomConfirmationDialog(strings) {
                    if (it) radioGroup.check(R.id.stateSymptomsRadioButton)
                }
            }
            onStateChangedListener = RadioGroup.OnCheckedChangeListener { _: RadioGroup, i: Int ->
                viewModel.updateFormState(
                    when (i) {
                        R.id.stateAllGoodRadioButton -> IsolationFormStateEnum.ALL_GOOD
                        R.id.stateSymptomsRadioButton -> IsolationFormStateEnum.SYMPTOMS
                        R.id.stateContactRadioButton -> IsolationFormStateEnum.CONTACT
                        R.id.statePositiveRadioButton -> IsolationFormStateEnum.POSITIVE
                        else -> throw IllegalStateException("Unknown state checked!!")
                    }
                )
            }
            identifier = groupTitle.hashCode().toLong()
        }

        val state = isolationManager.currentFormStateValue ?: return items
        when (state) {
            IsolationFormStateEnum.CONTACT -> items += contactItems()
            IsolationFormStateEnum.POSITIVE -> items += positiveItems()
            else -> Unit
        }

        if (isolationManager.currentRecommendationState != IsolationRecommendationStateEnum.INDETERMINATE) {
            items += readRecommendationItems()
        } else {
            val footer = strings["isolationFormController.footer.title"]
            if (!footer.isNullOrBlank()) {
                items += captionItem {
                    text = footer
                    identifier = "isolationFormController.footer.title".hashCode().toLong()
                }
            }
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = "spaceAtTheBottom".hashCode().toLong()
        }

        return items
    }

    private fun contactItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = "contactItemsSpaceOnTop".hashCode().toLong()
        }

        items += pickerEditTextItem {
            placeholder = "-"
            hint = strings["isolationFormController.contactCase.lastContactDate"]
            text = isolationManager.isolationLastContactDate?.let { timestamp ->
                dateFormat.format(Date(timestamp))
            }
            onClick = {
                val initialTimestamp = isolationManager.isolationLastContactDate
                    ?: Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                    }.timeInMillis
                MaterialAlertDialogBuilder(requireContext()).showSpinnerDayPicker(
                    strings, initialTimestamp,
                    PICKER_DAY_IN_PAST
                ) { newDate ->
                    text = dateFormat.format(Date(newDate))
                    viewModel.updateLastContactDate(newDate)
                }
            }
            identifier = hint.hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = "contactItemsSpaceAfterDate".hashCode().toLong()
        }

        items += isolationBooleanRadioGroupItem {
            groupTitle = strings["isolationFormController.contactCase.index.sectionTitle"]
            yesLabel = strings["common.yes"]
            noLabel = strings["common.no"]
            selectedState = isolationManager.isolationIsKnownIndexAtHome
            onStateChangedListener = { state ->
                viewModel.setIsKnownIndexAtHome(state)
            }
            identifier = groupTitle.hashCode().toLong()
        }

        if (isolationManager.isolationIsKnownIndexAtHome != true) {
            return items
        }
        if (isolationManager.isolationIsTestNegative == null) {
            return items
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = "contactItemsSpaceAfterIndexQuestion".hashCode().toLong()
        }

        items += isolationBooleanRadioGroupItem {
            groupTitle = strings["isolationFormController.contactCase.haveIndexSymptomsEndDate.sectionTitle"]
            yesLabel = strings["common.yes"]
            noLabel = strings["common.no"]
            selectedState = isolationManager.isolationKnowsIndexSymptomsEndDate
            onStateChangedListener = { state ->
                viewModel.setKnowsIndexSymptomsEndDate(state)
            }
            identifier = groupTitle.hashCode().toLong()
        }

        if (isolationManager.isolationKnowsIndexSymptomsEndDate != true) {
            return items
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = "isolationFormController.contactCase.symptomsEndDate.sectionTitle.spacing".hashCode().toLong()
        }

        items += titleItem {
            text = strings["isolationFormController.contactCase.symptomsEndDate.sectionTitle"]
            identifier = "isolationFormController.contactCase.symptomsEndDate.sectionTitle".hashCode().toLong()
        }

        items += pickerEditTextItem {
            placeholder = "-"
            hint = strings["isolationFormController.contactCase.symptomsEndDate"]
            text = isolationManager.isolationIndexSymptomsEndDate?.let { timestamp ->
                dateFormat.format(Date(timestamp))
            }
            onClick = {
                val initialTimestamp = isolationManager.isolationIndexSymptomsEndDate
                    ?: Calendar.getInstance().apply {
                        timeInMillis = System.currentTimeMillis()
                    }.timeInMillis
                MaterialAlertDialogBuilder(requireContext()).showSpinnerDayPicker(
                    strings, initialTimestamp,
                    PICKER_DAY_IN_PAST
                ) { newDate ->
                    text = dateFormat.format(Date(newDate))
                    viewModel.updateIndexSymptomsEndDate(newDate)
                }
            }
            identifier = hint.hashCode().toLong()
        }

        return items
    }

    private fun positiveItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = "positiveItemsSpaceOnTop".hashCode().toLong()
        }

        items += positiveTestItems()

        items += isolationBooleanRadioGroupItem {
            groupTitle = strings["isolationFormController.positiveCase.haveSymptoms.sectionTitle"]
            yesLabel = strings["common.yes"]
            noLabel = strings["common.no"]
            selectedState = isolationManager.isolationIsHavingSymptoms
            onStateChangedListener = { state ->
                viewModel.setIsHavingSymptoms(state)
            }
            identifier = groupTitle.hashCode().toLong()
        }

        if (isolationManager.isolationIsHavingSymptoms != true) {
            return items
        }

        items += symptomsStartDateItems()

        if (isolationManager.isPositiveCaseIsolationEnded != true) {
            return items
        }

        items += isolationBooleanRadioGroupItem {
            groupTitle = strings["isolationFormController.positiveCase.stillHavingFever.sectionTitle"]
            yesLabel = strings["common.yes"]
            noLabel = strings["common.no"]
            selectedState = isolationManager.isolationIsStillHavingFever
            onStateChangedListener = { state ->
                viewModel.setStillHavingFever(state)
            }
            identifier = groupTitle.hashCode().toLong()
        }

        return items
    }

    private fun positiveTestItems(): List<GenericItem> {
        return listOf(
            pickerEditTextItem {
                placeholder = "-"
                hint = strings["isolationFormController.positiveCase.positiveTestDate"]
                text = isolationManager.isolationPositiveTestingDate?.let { timestamp ->
                    dateFormat.format(Date(timestamp))
                }
                onClick = {
                    val initialTimestamp = isolationManager.isolationPositiveTestingDate
                        ?: Calendar.getInstance().apply {
                            timeInMillis = System.currentTimeMillis()
                        }.timeInMillis
                    MaterialAlertDialogBuilder(requireContext()).showSpinnerDayPicker(
                        strings,
                        initialTimestamp,
                        PICKER_DAY_IN_PAST
                    ) { newDate ->
                        text = dateFormat.format(Date(newDate))
                        viewModel.updatePositiveTestingDate(newDate)
                    }
                }
                identifier = hint.hashCode().toLong()
            }
        )
    }

    private fun symptomsStartDateItems(): List<GenericItem> {
        return listOf(
            pickerEditTextItem {
                placeholder = "-"
                hint = strings["isolationFormController.positiveCase.symptomsStartDate"]
                text = isolationManager.isolationSymptomsStartDate?.let { timestamp ->
                    dateFormat.format(Date(timestamp))
                }
                onClick = {
                    val initialTimestamp = isolationManager.isolationSymptomsStartDate
                        ?: Calendar.getInstance().apply {
                            timeInMillis = System.currentTimeMillis()
                        }.timeInMillis
                    MaterialAlertDialogBuilder(requireContext()).showSpinnerDayPicker(
                        strings,
                        initialTimestamp,
                        PICKER_DAY_IN_PAST
                    ) { newDate ->
                        text = dateFormat.format(Date(newDate))
                        viewModel.updateSymptomsStartDate(newDate)
                    }
                }
                identifier = hint.hashCode().toLong()
            }
        )
    }

    private fun readRecommendationItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = "readRecommendationItemsSpaceOnTop".hashCode().toLong()
        }

        items += buttonItem {
            text = strings["isolationFormController.readRecommendations"]
            gravity = Gravity.CENTER_HORIZONTAL
            onClickListener = View.OnClickListener {
                findNavControllerOrNull()?.navigateUp()
            }
            identifier = "isolationFormController.readRecommendations".hashCode().toLong()
        }

        return items
    }

    private fun scrollToBottom() {
        viewLifecycleOwnerOrNull()?.lifecycleScope?.launch(Dispatchers.Main) {
            delay(Constants.Android.ANIMATION_DELAY)
            binding?.recyclerView?.smoothScrollToPosition(Int.MAX_VALUE)
        }
    }

    companion object {
        private const val PICKER_DAY_IN_PAST: Int = 30
    }
}