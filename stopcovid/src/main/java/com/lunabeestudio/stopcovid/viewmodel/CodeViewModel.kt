/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.model.CovidException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class CodeViewModel(private val robertManager: RobertManager) : ViewModel() {

    val codeSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val covidException: SingleLiveEvent<CovidException> = SingleLiveEvent()
    val loadingInProgress: MutableLiveData<Float?> = MutableLiveData(null)
    val code: MutableLiveData<String> = MutableLiveData("")

    @OptIn(DelicateCoroutinesApi::class)
    fun verifyCode(code: String, firstSymptoms: Int?, positiveTest: Int?, application: RobertApplication) {
        if (loadingInProgress.value == null) {
            GlobalScope.launch(Dispatchers.IO) {
                loadingInProgress.postValue(0f)
                val startTime = System.currentTimeMillis()
                val result = robertManager.report(code, firstSymptoms, positiveTest, application) {
                    loadingInProgress.postValue(it)
                }
                Timber.d("report total duration = ${(System.currentTimeMillis() - startTime).milliseconds}")
                loadingInProgress.postValue(null)
                when (result) {
                    is RobertResult.Success -> codeSuccess.postValue(Unit)
                    is RobertResult.Failure -> covidException.postValue(result.error.toCovidException())
                }
            }
        }
    }
}

class CodeViewModelFactory(private val robertManager: RobertManager) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CodeViewModel(robertManager) as T
    }
}