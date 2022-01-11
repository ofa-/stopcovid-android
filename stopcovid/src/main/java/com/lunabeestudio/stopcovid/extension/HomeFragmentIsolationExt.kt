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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.fixFormatter
import com.lunabeestudio.stopcovid.coreui.extension.formatWithSameValue
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLocale
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.coreui.model.CardTheme
import com.lunabeestudio.stopcovid.fragment.HomeFragment
import com.lunabeestudio.stopcovid.fragment.HomeFragmentDirections
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

internal fun HomeFragment.addIsolationItems(items: ArrayList<GenericItem>, analyticsManager: AnalyticsManager) {
    val dateFormat: DateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG, getApplicationLocale())
    val state = isolationManager.currentRecommendationState

    if (state == IsolationRecommendationStateEnum.INITIAL_CASE_SAFE) {
        items += cardWithActionItem {
            mainTitle = strings[state.getTitleStringKey()]
            mainBody = strings[state.getBodyStringKey()]
            mainImage = R.drawable.doctor
            mainLayoutDirection = LayoutDirection.RTL
            contentDescription = strings[state.getTitleStringKey()]
            onCardClick = {
                openIsolationForm(analyticsManager)
            }
            identifier = "isolationSafe".hashCode().toLong()
        }
    } else {
        items += cardWithActionItem(CardTheme.Primary) {
            cardTitle = strings[state.getTitleStringKey()]
            cardTitleIcon = R.drawable.ic_recommendation
            val isolationEndDateString = isolationManager.currentIsolationEndDate?.let { dateFormat.format(Date(it)) }
            mainBody = strings[state.getBodyStringKey()]?.fixFormatter()?.formatWithSameValue(isolationEndDateString)
            onCardClick = {
                openIsolationForm(analyticsManager)
            }
            actions = actionsForIsolationState(state, analyticsManager)
            identifier = "isolationNotSafe".hashCode().toLong()
        }
    }

    items += spaceItem {
        spaceRes = R.dimen.spacing_medium
        identifier = items.count().toLong()
    }
}

private fun HomeFragment.actionsForIsolationState(
    recommendationState: IsolationRecommendationStateEnum,
    analyticsManager: AnalyticsManager
): List<Action> {

    return when (recommendationState) {
        IsolationRecommendationStateEnum.INDETERMINATE,
        IsolationRecommendationStateEnum.INITIAL_CASE_SAFE,
        -> emptyList()
        IsolationRecommendationStateEnum.INITIAL_CASE_AT_RISK_OR_SICK ->
            listOf(defineIsolationAction(recommendationState, analyticsManager))
        IsolationRecommendationStateEnum.ALL_GOOD ->
            listOf(changeStateAction(recommendationState, analyticsManager))
        IsolationRecommendationStateEnum.SYMPTOMS -> listOf(
            testingSitesAction(recommendationState),
            positiveTestAction(recommendationState, analyticsManager),
            negativeTestAction(recommendationState, openForm = false, resetState = false, analyticsManager)
        )
        IsolationRecommendationStateEnum.SYMPTOMS_TESTED -> listOf(
            changeStateAction(recommendationState, analyticsManager)
        )
        IsolationRecommendationStateEnum.CONTACT_CASE_UNKNOWN_INDEX -> listOf(
            testingSitesAction(recommendationState),
            symptomsAction(recommendationState, analyticsManager),
            positiveTestAction(recommendationState, analyticsManager),
            negativeTestAction(recommendationState, openForm = false, resetState = false, analyticsManager)
        )
        IsolationRecommendationStateEnum.CONTACT_CASE_KNOWN_INDEX_NOT_TESTED -> listOf(
            testingSitesAction(recommendationState),
            positiveTestAction(recommendationState, analyticsManager),
            negativeTestAction(recommendationState, openForm = true, resetState = false, analyticsManager)
        )
        IsolationRecommendationStateEnum.CONTACT_CASE_KNOWN_INDEX_TESTED_KNOWN_DATE -> listOf(
            symptomsAction(recommendationState, analyticsManager)
        )
        IsolationRecommendationStateEnum.CONTACT_CASE_KNOWN_INDEX_TESTED_UNKNOWN_DATE -> listOf(
            havingDateAction(recommendationState, analyticsManager),
            symptomsAction(recommendationState, analyticsManager)
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
            answerStillHavingFeverAction(recommendationState, analyticsManager)
        )
        IsolationRecommendationStateEnum.POSITIVE_CASE_POST_ISOLATION_PERIOD -> emptyList()
        IsolationRecommendationStateEnum.POSITIVE_CASE_SYMPTOMS_AFTER_ISOLATION_STILL_HAVING_FEVER -> listOf(
            noMoreFeverAction(recommendationState)
        )
    }
}

private fun HomeFragment.openIsolationForm(analyticsManager: AnalyticsManager) {
    analyticsManager.reportAppEvent(AppEventName.e6, null)
    findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToIsolationFormFragment())
}

private fun HomeFragment.changeStateAction(
    isolationRecommendationState: IsolationRecommendationStateEnum,
    analyticsManager: AnalyticsManager
): Action =
    Action(label = strings["isolation.recommendation.${isolationRecommendationState.key}.changeMyState"]) {
        isolationManager.resetData()
        openIsolationForm(analyticsManager)
    }

private fun HomeFragment.defineIsolationAction(
    isolationRecommendationState: IsolationRecommendationStateEnum,
    analyticsManager: AnalyticsManager
) =
    Action(label = strings["isolation.recommendation.${isolationRecommendationState.key}.defineIsolationPeriod"]) {
        isolationManager.resetData()
        openIsolationForm(analyticsManager)
        executeActionAfterAnimation {
            isolationManager.updateStateBasedOnAppMainStateIfNeeded()
        }
    }

private fun HomeFragment.testingSitesAction(isolationRecommendationState: IsolationRecommendationStateEnum) =
    Action(label = strings["isolation.recommendation.${isolationRecommendationState.key}.testingSites"]) {
        strings["myHealthController.testingSites.url"]?.openInExternalBrowser(requireContext())
    }

private fun HomeFragment.positiveTestAction(
    isolationRecommendationState: IsolationRecommendationStateEnum,
    analyticsManager: AnalyticsManager
) =
    Action(label = strings["isolation.recommendation.${isolationRecommendationState.key}.positiveTest"]) {
        openIsolationForm(analyticsManager)
        executeActionAfterAnimation {
            isolationManager.updateState(IsolationFormStateEnum.POSITIVE)
        }
    }

private fun HomeFragment.negativeTestAction(
    isolationRecommendationState: IsolationRecommendationStateEnum,
    openForm: Boolean = true, resetState: Boolean = false,
    analyticsManager: AnalyticsManager,
) =
    Action(label = strings["isolation.recommendation.${isolationRecommendationState.key}.negativeTest"]) {
        if (resetState) {
            isolationManager.resetData()
        }
        if (openForm) {
            openIsolationForm(analyticsManager)
            executeActionAfterAnimation {
                isolationManager.setNegativeTest()
            }
        } else {
            isolationManager.setNegativeTest()
        }
    }

private fun HomeFragment.symptomsAction(
    isolationRecommendationState: IsolationRecommendationStateEnum,
    analyticsManager: AnalyticsManager
) =
    Action(label = strings["isolation.recommendation.${isolationRecommendationState.key}.symptoms"]) {
        MaterialAlertDialogBuilder(requireContext()).showSymptomConfirmationDialog(strings) {
            if (it) {
                isolationManager.updateState(IsolationFormStateEnum.SYMPTOMS)
                openIsolationForm(analyticsManager)
            }
        }
    }

private fun HomeFragment.havingDateAction(
    isolationRecommendationState: IsolationRecommendationStateEnum,
    analyticsManager: AnalyticsManager
) =
    Action(label = strings["isolation.recommendation.${isolationRecommendationState.key}.havingTheDate"]) {
        openIsolationForm(analyticsManager)
        isolationManager.setKnowsIndexSymptomsEndDate(true)
    }

private fun HomeFragment.scheduleReminderAction(isolationRecommendationState: IsolationRecommendationStateEnum) =
    Action(label = strings["isolation.recommendation.${isolationRecommendationState.key}.scheduleReminder"]) {
        val triggerDate = Date(isolationManager.positiveCaseIsolationEndDate ?: System.currentTimeMillis())
        (context?.applicationContext as? StopCovid)?.setIsolationReminder(triggerDate)
        isolationManager.setFeverReminderScheduled()
    }

private fun HomeFragment.answerStillHavingFeverAction(
    isolationRecommendationState: IsolationRecommendationStateEnum,
    analyticsManager: AnalyticsManager
) =
    Action(label = strings["isolation.recommendation.${isolationRecommendationState.key}.stillHavingFever"]) {
        openIsolationForm(analyticsManager)
    }

private fun HomeFragment.noMoreFeverAction(isolationRecommendationState: IsolationRecommendationStateEnum) =
    Action(label = strings["isolation.recommendation.${isolationRecommendationState.key}.noMoreFever"]) {
        isolationManager.setStillHavingFever(false)
    }

private fun executeActionAfterAnimation(action: () -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        delay(Constants.Android.ANIMATION_DELAY)
        action()
    }
}
