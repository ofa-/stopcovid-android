/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/12/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.util.LayoutDirection
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.fixFormatter
import com.lunabeestudio.stopcovid.coreui.extension.formatWithSameValue
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.fastitem.IsolationCardActions
import com.lunabeestudio.stopcovid.fastitem.imageBackgroundCardItem
import com.lunabeestudio.stopcovid.fastitem.isolationCardItem
import com.lunabeestudio.stopcovid.fragment.ProximityFragment
import com.lunabeestudio.stopcovid.fragment.ProximityFragmentDirections
import com.lunabeestudio.stopcovid.manager.IsolationFormStateEnum
import com.lunabeestudio.stopcovid.model.IsolationRecommendationStateEnum
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

internal fun ProximityFragment.addIsolationItems(items: ArrayList<GenericItem>) {
    val dateFormat: DateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG)
    val state = isolationManager.currentRecommendationState
    val formListener = View.OnClickListener {
        findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToIsolationFormFragment())
    }

    if (state == IsolationRecommendationStateEnum.INITIAL_CASE_SAFE) {
        items += imageBackgroundCardItem {
            title = strings[state.getTitleStringKey()]
            subtitle = strings[state.getBodyStringKey()]
            iconRes = R.drawable.doctor
            layoutDirection = LayoutDirection.RTL
            contentDescription = strings[state.getTitleStringKey()]
            onClickListener = formListener
            identifier = "isolationSafe".hashCode().toLong()
        }
    } else {
        items += isolationCardItem {
            title = strings[state.getTitleStringKey()]
            val isolationEndDateString = isolationManager.currentIsolationEndDate?.let { dateFormat.format(Date(it)) }
            content = strings[state.getBodyStringKey()]?.fixFormatter()?.formatWithSameValue(isolationEndDateString)
            onClickListener = formListener
            actions = actionsForIsolationState(state)
            identifier = "isolationNotSafe".hashCode().toLong()
        }
    }

    items += spaceItem {
        spaceRes = R.dimen.spacing_medium
        identifier = items.count().toLong()
    }
}

private fun ProximityFragment.actionsForIsolationState(recommendationState: IsolationRecommendationStateEnum): List<IsolationCardActions> {

    return when (recommendationState) {
        IsolationRecommendationStateEnum.INDETERMINATE,
        IsolationRecommendationStateEnum.INITIAL_CASE_SAFE,
        -> emptyList()
        IsolationRecommendationStateEnum.INITIAL_CASE_AT_RISK_OR_SICK ->
            listOf(defineIsolationAction(recommendationState))
        IsolationRecommendationStateEnum.ALL_GOOD ->
            listOf(changeStateAction(recommendationState))
        IsolationRecommendationStateEnum.SYMPTOMS -> listOf(
            testingSitesAction(recommendationState),
            positiveTestAction(recommendationState),
            negativeTestAction(recommendationState, openForm = false, resetState = false)
        )
        IsolationRecommendationStateEnum.SYMPTOMS_TESTED -> listOf(
            changeStateAction(recommendationState)
        )
        IsolationRecommendationStateEnum.CONTACT_CASE_UNKNOWN_INDEX -> listOf(
            testingSitesAction(recommendationState),
            symptomsAction(recommendationState),
            positiveTestAction(recommendationState),
            negativeTestAction(recommendationState, openForm = false, resetState = false)
        )
        IsolationRecommendationStateEnum.CONTACT_CASE_KNOWN_INDEX_NOT_TESTED -> listOf(
            testingSitesAction(recommendationState),
            positiveTestAction(recommendationState),
            negativeTestAction(recommendationState, openForm = true, resetState = false)
        )
        IsolationRecommendationStateEnum.CONTACT_CASE_KNOWN_INDEX_TESTED_KNOWN_DATE -> listOf(
            symptomsAction(recommendationState)
        )
        IsolationRecommendationStateEnum.CONTACT_CASE_KNOWN_INDEX_TESTED_UNKNOWN_DATE -> listOf(
            havingDateAction(recommendationState),
            symptomsAction(recommendationState)
        )
        IsolationRecommendationStateEnum.CONTACT_CASE_POST_ISOLATION_PERIOD -> emptyList()
        IsolationRecommendationStateEnum.POSITIVE_CASE_NO_SYMPTOMS -> emptyList()
        IsolationRecommendationStateEnum.POSITIVE_CASE_SYMPTOMS_DURING_ISOLATION ->
            if (isolationManager.isolationIsFeverReminderScheduled != true) {
                listOf(scheduleReminderAction(recommendationState))
            } else {
                emptyList()
            }
        IsolationRecommendationStateEnum.POSITIVE_CASE_SYMPTOMS_AFTER_ISOLATION -> listOf(
            answerStillHavingFeverAction(recommendationState)
        )
        IsolationRecommendationStateEnum.POSITIVE_CASE_POST_ISOLATION_PERIOD -> emptyList()
        IsolationRecommendationStateEnum.POSITIVE_CASE_SYMPTOMS_AFTER_ISOLATION_STILL_HAVING_FEVER -> listOf(
            noMoreFeverAction(recommendationState)
        )
    }
}

private fun ProximityFragment.openIsolationForm() {
    findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToIsolationFormFragment())
}

private fun ProximityFragment.changeStateAction(isolationRecommendationState: IsolationRecommendationStateEnum): IsolationCardActions =
    IsolationCardActions(strings["isolation.recommendation.${isolationRecommendationState.key}.changeMyState"])
    {
        isolationManager.resetData()
        openIsolationForm()
    }

private fun ProximityFragment.defineIsolationAction(isolationRecommendationState: IsolationRecommendationStateEnum) =
    IsolationCardActions(strings["isolation.recommendation.${isolationRecommendationState.key}.defineIsolationPeriod"])
    {
        isolationManager.resetData()
        openIsolationForm()
        executeActionAfterAnimation {
            isolationManager.updateStateBasedOnAppMainStateIfNeeded()
        }
    }

private fun ProximityFragment.testingSitesAction(isolationRecommendationState: IsolationRecommendationStateEnum): IsolationCardActions =
    IsolationCardActions(strings["isolation.recommendation.${isolationRecommendationState.key}.testingSites"])
    {
        strings["myHealthController.testingSites.url"]?.openInExternalBrowser(requireContext())
    }

private fun ProximityFragment.positiveTestAction(isolationRecommendationState: IsolationRecommendationStateEnum): IsolationCardActions =
    IsolationCardActions(strings["isolation.recommendation.${isolationRecommendationState.key}.positiveTest"])
    {
        openIsolationForm()
        executeActionAfterAnimation {
            isolationManager.updateState(IsolationFormStateEnum.POSITIVE)
        }
    }

private fun ProximityFragment.negativeTestAction(
    isolationRecommendationState: IsolationRecommendationStateEnum,
    openForm: Boolean = true, resetState: Boolean = false,
): IsolationCardActions =
    IsolationCardActions(strings["isolation.recommendation.${isolationRecommendationState.key}.negativeTest"])
    {
        if (resetState) {
            isolationManager.resetData()
        }
        if (openForm) {
            openIsolationForm()
            executeActionAfterAnimation {
                isolationManager.setNegativeTest()
            }
        } else {
            isolationManager.setNegativeTest()
        }
    }

private fun ProximityFragment.symptomsAction(isolationRecommendationState: IsolationRecommendationStateEnum): IsolationCardActions =
    IsolationCardActions(strings["isolation.recommendation.${isolationRecommendationState.key}.symptoms"]) {
        MaterialAlertDialogBuilder(requireContext()).showSymptomConfirmationDialog(strings) {
            if (it){
                isolationManager.updateState(IsolationFormStateEnum.SYMPTOMS)
                openIsolationForm()
            }
        }
    }

private fun ProximityFragment.havingDateAction(isolationRecommendationState: IsolationRecommendationStateEnum): IsolationCardActions =
    IsolationCardActions(strings["isolation.recommendation.${isolationRecommendationState.key}.havingTheDate"])
    {
        openIsolationForm()
        isolationManager.setKnowsIndexSymptomsEndDate(true)
    }

private fun ProximityFragment.scheduleReminderAction(isolationRecommendationState: IsolationRecommendationStateEnum): IsolationCardActions =
    IsolationCardActions(strings["isolation.recommendation.${isolationRecommendationState.key}.scheduleReminder"])
    {
        val triggerDate = Date(isolationManager.positiveCaseIsolationEndDate ?: System.currentTimeMillis())
        (context?.applicationContext as? StopCovid)?.setIsolationReminder(triggerDate)
        isolationManager.setFeverReminderScheduled()
    }

private fun ProximityFragment.answerStillHavingFeverAction(isolationRecommendationState: IsolationRecommendationStateEnum): IsolationCardActions =
    IsolationCardActions(strings["isolation.recommendation.${isolationRecommendationState.key}.stillHavingFever"])
    {
        openIsolationForm()
    }

private fun ProximityFragment.noMoreFeverAction(isolationRecommendationState: IsolationRecommendationStateEnum): IsolationCardActions =
    IsolationCardActions(strings["isolation.recommendation.${isolationRecommendationState.key}.noMoreFever"])
    {
        isolationManager.setStillHavingFever(false)
    }

private fun executeActionAfterAnimation(action: () -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        delay(Constants.Android.ANIMATION_DELAY)
        action()
    }
}