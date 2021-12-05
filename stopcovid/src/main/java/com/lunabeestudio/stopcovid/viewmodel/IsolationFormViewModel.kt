/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/12/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.manager.IsolationFormStateEnum
import com.lunabeestudio.stopcovid.manager.IsolationManager

class IsolationFormViewModel(private val isolationManager: IsolationManager) : ViewModel() {

    val isolationFormState: LiveData<Event<IsolationFormStateEnum?>> = isolationManager.currentFormState
    val isolationDataChanged: SingleLiveEvent<Unit> = isolationManager.changedEvent

    fun updateFormState(formState: IsolationFormStateEnum) {
        isolationManager.updateState(formState)
    }

    fun updateLastContactDate(newDate: Long) {
        isolationManager.updateLastContactDate(newDate)
    }

    fun setIsKnownIndexAtHome(isAtHome: Boolean?) {
        isolationManager.setIsKnownIndexAtHome(isAtHome)
    }

    fun setKnowsIndexSymptomsEndDate(knowsDate: Boolean?) {
        isolationManager.setKnowsIndexSymptomsEndDate(knowsDate)
    }

    fun updateIndexSymptomsEndDate(newDate: Long?) {
        isolationManager.updateIndexSymptomsEndDate(newDate)
    }

    fun updatePositiveTestingDate(newDate: Long?) {
        isolationManager.updatePositiveTestingDate(newDate)
    }

    fun setIsHavingSymptoms(symptoms: Boolean?) {
        isolationManager.setIsHavingSymptoms(symptoms)
    }

    fun updateSymptomsStartDate(newDate: Long?) {
        isolationManager.updateSymptomsStartDate(newDate)
    }

    fun setStillHavingFever(fever: Boolean?) {
        isolationManager.setStillHavingFever(fever)
    }
}

class IsolationFormViewModelFactory(
    private val isolationManager: IsolationManager,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return IsolationFormViewModel(isolationManager) as T
    }
}