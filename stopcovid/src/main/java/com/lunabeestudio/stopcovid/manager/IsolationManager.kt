/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/12/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.extension.roundTimestampToStartOfDay
import com.lunabeestudio.stopcovid.model.IsolationRecommendationStateEnum
import java.util.Date
import java.util.concurrent.TimeUnit.SECONDS

class IsolationManager(
    private val stopCovid: StopCovid,
    val robertManager: RobertManager,
    val secureKeystoreDataSource: SecureKeystoreDataSource,
) {

    val changedEvent: SingleLiveEvent<Unit> = SingleLiveEvent()
    private var canTriggerChangedEvent: Boolean = true

    private fun notifyChangeIfAllowed() {
        if (canTriggerChangedEvent) {
            changedEvent.postValue(null)
        }
    }

    private val _currentFormState: MutableLiveData<Event<IsolationFormStateEnum?>> = MutableLiveData(Event(currentFormStateValue))
    val currentFormState: LiveData<Event<IsolationFormStateEnum?>>
        get() = _currentFormState

    var currentFormStateValue: IsolationFormStateEnum?
        get() = secureKeystoreDataSource.isolationFormState?.let { enumValues<IsolationFormStateEnum>()[it] }
        private set(value) {
            secureKeystoreDataSource.isolationFormState = value?.ordinal
            _currentFormState.postValue(Event(value))
        }

    val currentRecommendationState: IsolationRecommendationStateEnum
        get() = calculateRecommendationState()

    var isolationLastContactDate: Long?
        get() = secureKeystoreDataSource.isolationLastContactDate
        private set(value) {
            secureKeystoreDataSource.isolationLastContactDate = value
            notifyChangeIfAllowed()
        }

    var isolationIsKnownIndexAtHome: Boolean?
        get() = secureKeystoreDataSource.isolationIsKnownIndexAtHome
        private set(value) {
            secureKeystoreDataSource.isolationIsKnownIndexAtHome = value
            notifyChangeIfAllowed()
        }

    var isolationKnowsIndexSymptomsEndDate: Boolean?
        get() = secureKeystoreDataSource.isolationKnowsIndexSymptomsEndDate
        private set(value) {
            secureKeystoreDataSource.isolationKnowsIndexSymptomsEndDate = value
            notifyChangeIfAllowed()
        }

    var isolationIndexSymptomsEndDate: Long?
        get() = secureKeystoreDataSource.isolationIndexSymptomsEndDate
        private set(value) {
            secureKeystoreDataSource.isolationIndexSymptomsEndDate = value
            notifyChangeIfAllowed()
        }

    private var isolationLastFormValidationDate: Long?
        get() = secureKeystoreDataSource.isolationLastFormValidationDate
        private set(value) {
            secureKeystoreDataSource.isolationLastFormValidationDate = value
            notifyChangeIfAllowed()
        }

    var isolationIsTestNegative: Boolean?
        get() = secureKeystoreDataSource.isolationIsTestNegative
        private set(value) {
            secureKeystoreDataSource.isolationIsTestNegative = value
            notifyChangeIfAllowed()
        }

    var isolationPositiveTestingDate: Long?
        get() = secureKeystoreDataSource.isolationPositiveTestingDate
        private set(value) {
            secureKeystoreDataSource.isolationPositiveTestingDate = value
            notifyChangeIfAllowed()
        }

    var isolationIsHavingSymptoms: Boolean?
        get() = secureKeystoreDataSource.isolationIsHavingSymptoms
        private set(value) {
            secureKeystoreDataSource.isolationIsHavingSymptoms = value
            notifyChangeIfAllowed()
        }

    var isolationSymptomsStartDate: Long?
        get() = secureKeystoreDataSource.isolationSymptomsStartDate
        private set(value) {
            secureKeystoreDataSource.isolationSymptomsStartDate = value
            notifyChangeIfAllowed()
        }

    var isolationIsStillHavingFever: Boolean?
        get() = secureKeystoreDataSource.isolationIsStillHavingFever
        private set(value) {
            secureKeystoreDataSource.isolationIsStillHavingFever = value
            notifyChangeIfAllowed()
        }

    var isolationIsFeverReminderScheduled: Boolean?
        get() = secureKeystoreDataSource.isolationIsFeverReminderScheduled
        private set(value) {
            secureKeystoreDataSource.isolationIsFeverReminderScheduled = value
            notifyChangeIfAllowed()
        }

    val currentIsolationEndDate: Long?
        get() = when (currentFormStateValue) {
            IsolationFormStateEnum.CONTACT -> contactCaseIsolationEndDate
            IsolationFormStateEnum.POSITIVE -> positiveCaseIsolationEndDate
            else -> null
        }

    private val contactCaseIsolationStartDate: Long?
        get() {
            return (isolationIndexSymptomsEndDate ?: contactCaseIsolationContactCalculatedDate).roundTimestampToStartOfDay()
        }

    private val contactCaseIsolationContactCalculatedDate: Long
        get() {
            return when {
                robertManager.isImmune -> isolationLastContactDate
                else -> isolationLastContactDate
            } ?: System.currentTimeMillis()
        }

    private val contactCaseIsolationEndDate: Long?
        get() {
            return contactCaseIsolationStartDate?.let {
                it + SECONDS.toMillis(robertManager.configuration.isolationDuration)
            }.roundTimestampToStartOfDay()
        }

    private val isContactCaseIsolationEnded: Boolean?
        get() {
            return contactCaseIsolationEndDate?.let { Date().time > it }
        }

    private val contactCasePostIsolationEndDate: Long?
        get() {
            return contactCaseIsolationStartDate?.let {
                it + SECONDS.toMillis(robertManager.configuration.isolationDuration + robertManager.configuration.postIsolationDuration)
            }.roundTimestampToStartOfDay()
        }

    private val isContactCasePostIsolationEnded: Boolean?
        get() {
            return contactCasePostIsolationEndDate?.let { Date().time > it }
        }

    private val positiveCaseIsolationStartDate: Long?
        get() {
            return isolationSymptomsStartDate ?: isolationPositiveTestingDate
        }

    val positiveCaseIsolationEndDate: Long?
        get() {
            return positiveCaseIsolationStartDate?.let {
                it + (robertManager.configuration.covidIsolationDuration) * 1000
            }.roundTimestampToStartOfDay()
        }

    val isPositiveCaseIsolationEnded: Boolean?
        get() {
            return positiveCaseIsolationEndDate?.let { Date().time > it }
        }

    private val positiveCasePostIsolationEndDate: Long?
        get() {
            return positiveCaseIsolationStartDate?.let {
                it + (robertManager.configuration.covidIsolationDuration + robertManager.configuration.postIsolationDuration) * 1000
            }.roundTimestampToStartOfDay()
        }

    private val isPositiveCasePostIsolationEnded: Boolean?
        get() {
            return positiveCasePostIsolationEndDate?.let { Date().time > it }
        }

    // Public methods :
    fun updateState(formState: IsolationFormStateEnum) {
        canTriggerChangedEvent = false
        resetData()
        currentFormStateValue = formState
        prefillNeededInfoFor(formState)
        canTriggerChangedEvent = true
        notifyChangeIfAllowed()
    }

    private fun prefillNeededInfoFor(formState: IsolationFormStateEnum) {
        when (formState) {
            IsolationFormStateEnum.POSITIVE -> {
                isolationSymptomsStartDate = robertManager.reportSymptomsStartDate
                isolationPositiveTestingDate = robertManager.reportPositiveTestDate

                if (isolationSymptomsStartDate != null) {
                    isolationIsHavingSymptoms = true
                } else if (isolationPositiveTestingDate != null) {
                    isolationIsHavingSymptoms = false
                }
            }
            else -> Unit
        }
    }

    fun updateLastContactDate(date: Long) {
        isolationLastContactDate = date
    }

    fun setNegativeTest() {
        isolationIsTestNegative = true
    }

    fun setKnowsIndexSymptomsEndDate(knows: Boolean?) {
        isolationKnowsIndexSymptomsEndDate = knows
    }

    fun setStillHavingFever(fever: Boolean?) {
        isolationIsStillHavingFever = fever
    }

    fun setFeverReminderScheduled() {
        isolationIsFeverReminderScheduled = true
    }

    fun setIsKnownIndexAtHome(isAtHome: Boolean?) {
        isolationIsKnownIndexAtHome = isAtHome
    }

    fun updateIndexSymptomsEndDate(date: Long?) {
        isolationIndexSymptomsEndDate = date
    }

    fun updatePositiveTestingDate(date: Long?) {
        isolationPositiveTestingDate = date
    }

    fun setIsHavingSymptoms(symptoms: Boolean?) {
        isolationIsHavingSymptoms = symptoms
    }

    fun updateSymptomsStartDate(date: Long?) {
        isolationSymptomsStartDate = date
    }

    fun resetData() {
        _currentFormState.postValue(Event(null))

        val previousScanTriggerChangedEvent = canTriggerChangedEvent
        canTriggerChangedEvent = false
        currentFormStateValue = null
        isolationLastContactDate = null
        isolationIsKnownIndexAtHome = null
        isolationKnowsIndexSymptomsEndDate = null
        isolationIndexSymptomsEndDate = null
        isolationLastFormValidationDate = null
        isolationIsTestNegative = null
        isolationPositiveTestingDate = null
        isolationIsHavingSymptoms = null
        isolationSymptomsStartDate = null
        isolationIsStillHavingFever = null
        isolationIsFeverReminderScheduled = null
        stopCovid.cancelIsolationReminder()
        canTriggerChangedEvent = previousScanTriggerChangedEvent
        notifyChangeIfAllowed()
    }

    fun updateStateBasedOnAppMainStateIfNeeded() {
        when {
            robertManager.isImmune -> IsolationFormStateEnum.POSITIVE
            isAtRisk == true -> IsolationFormStateEnum.CONTACT
            else -> null
        }?.let {
            updateState(it)
        }
    }

    private fun calculateRecommendationState(): IsolationRecommendationStateEnum {
        return when (currentFormStateValue) {
            IsolationFormStateEnum.ALL_GOOD -> IsolationRecommendationStateEnum.ALL_GOOD
            IsolationFormStateEnum.SYMPTOMS -> calculateSymptomsRecommendationState()
            IsolationFormStateEnum.CONTACT -> calculateContactRecommendationState()
            IsolationFormStateEnum.POSITIVE -> calculatePositiveCaseRecommendationState()
            else -> calculateInitialCase()
        }
    }

    private fun calculateInitialCase(): IsolationRecommendationStateEnum {
        return when {
            isAtRisk == true || robertManager.isImmune -> {
                IsolationRecommendationStateEnum.INITIAL_CASE_AT_RISK_OR_SICK
            }
            else -> IsolationRecommendationStateEnum.INITIAL_CASE_SAFE
        }
    }

    private val isAtRisk: Boolean?
        get() = with(robertManager) {
            atRiskStatus?.riskLevel?.let { it >= configuration.isolationMinRiskLevel }
        }

    private fun calculateSymptomsRecommendationState(): IsolationRecommendationStateEnum {
        return when (isolationIsTestNegative) {
            null -> IsolationRecommendationStateEnum.SYMPTOMS
            else -> IsolationRecommendationStateEnum.SYMPTOMS_TESTED
        }
    }

    private fun calculateContactRecommendationState(): IsolationRecommendationStateEnum {
        var recommendation: IsolationRecommendationStateEnum = IsolationRecommendationStateEnum.INDETERMINATE

        when (isolationIsKnownIndexAtHome) {
            false -> {
                if (isolationIsTestNegative == null) {
                    recommendation = IsolationRecommendationStateEnum.CONTACT_CASE_UNKNOWN_INDEX
                } else {
                    if (isContactCasePostIsolationEnded != null) {
                        recommendation = if (isContactCasePostIsolationEnded == true) {
                            calculateInitialCase()
                        } else {
                            IsolationRecommendationStateEnum.CONTACT_CASE_POST_ISOLATION_PERIOD
                        }
                    }
                }
            }
            true -> {
                if (isolationIsTestNegative == null) {
                    recommendation = IsolationRecommendationStateEnum.CONTACT_CASE_KNOWN_INDEX_NOT_TESTED
                } else {
                    if (isolationKnowsIndexSymptomsEndDate == true) {
                        if (isolationIndexSymptomsEndDate != null) {
                            if (isContactCaseIsolationEnded == true) {
                                if (isContactCasePostIsolationEnded == true) {
                                    recommendation = calculateInitialCase()
                                } else if (isContactCasePostIsolationEnded == false) {
                                    recommendation = IsolationRecommendationStateEnum.CONTACT_CASE_POST_ISOLATION_PERIOD
                                }
                            } else if (isContactCaseIsolationEnded == false) {
                                recommendation = IsolationRecommendationStateEnum.CONTACT_CASE_KNOWN_INDEX_TESTED_KNOWN_DATE
                            }
                        }
                    } else if (isolationKnowsIndexSymptomsEndDate == false) {
                        recommendation = IsolationRecommendationStateEnum.CONTACT_CASE_KNOWN_INDEX_TESTED_UNKNOWN_DATE
                    }
                }
            }
        }
        return recommendation
    }

    private fun calculatePositiveCaseRecommendationState(): IsolationRecommendationStateEnum {
        var recommendation: IsolationRecommendationStateEnum = IsolationRecommendationStateEnum.INDETERMINATE

        val isolationIsHavingSymptoms = isolationIsHavingSymptoms ?: return recommendation
        val isPositiveCaseIsolationEnded = isPositiveCaseIsolationEnded ?: return recommendation

        when (isolationIsHavingSymptoms) {
            true -> if (isolationSymptomsStartDate != null) {
                if (isPositiveCaseIsolationEnded) {
                    if (isolationIsStillHavingFever == true) {
                        recommendation = IsolationRecommendationStateEnum.POSITIVE_CASE_SYMPTOMS_AFTER_ISOLATION_STILL_HAVING_FEVER
                    } else if (isolationIsStillHavingFever == false) {
                        if (isPositiveCasePostIsolationEnded == true) {
                            recommendation = calculateInitialCase()
                        } else if (isPositiveCasePostIsolationEnded == false) {
                            recommendation = IsolationRecommendationStateEnum.POSITIVE_CASE_POST_ISOLATION_PERIOD
                        }
                    } else {
                        recommendation = IsolationRecommendationStateEnum.POSITIVE_CASE_SYMPTOMS_AFTER_ISOLATION
                    }
                } else {
                    recommendation = IsolationRecommendationStateEnum.POSITIVE_CASE_SYMPTOMS_DURING_ISOLATION
                }
            }
            false -> {
                if (isPositiveCaseIsolationEnded) {
                    if (isPositiveCasePostIsolationEnded == true) {
                        recommendation = calculateInitialCase()
                    } else if (isPositiveCasePostIsolationEnded == false) {
                        recommendation = IsolationRecommendationStateEnum.POSITIVE_CASE_POST_ISOLATION_PERIOD
                    }
                } else {
                    recommendation = IsolationRecommendationStateEnum.POSITIVE_CASE_NO_SYMPTOMS
                }
            }
        }

        return recommendation
    }
}

enum class IsolationFormStateEnum {
    // DO NOT CHANGE ORDER OR REMOVE !!!
    ALL_GOOD,
    SYMPTOMS,
    CONTACT,
    POSITIVE
}