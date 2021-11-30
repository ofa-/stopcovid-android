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
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.domain.model.CaptchaType
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.model.CaptchaNextFragment
import com.lunabeestudio.stopcovid.model.CovidException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class CaptchaViewModel(private val robertManager: RobertManager) : ViewModel() {

    val imageSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val audioSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val codeSuccess: SingleLiveEvent<CaptchaNextFragment> = SingleLiveEvent()
    val covidException: SingleLiveEvent<CovidException> = SingleLiveEvent()
    val loadingInProgress: MutableLiveData<Boolean> = MutableLiveData(false)
    lateinit var audioPath: String
    lateinit var imagePath: String
    private lateinit var captchaId: String
    var code: String = ""
    var isImage: Boolean = true

    fun generateCaptcha() {
        if (loadingInProgress.value == false) {
            viewModelScope.launch(Dispatchers.IO) {
                loadingInProgress.postValue(true)
                val result = robertManager.generateCaptcha(
                    if (isImage) CaptchaType.IMAGE else CaptchaType.AUDIO,
                    Locale.getDefault().language
                )
                when (result) {
                    is RobertResultData.Success -> {
                        captchaId = result.data
                        if (isImage) {
                            getCaptchaImage()
                        } else {
                            getCaptchaAudio()
                        }
                    }
                    is RobertResultData.Failure -> {
                        loadingInProgress.postValue(false)
                        covidException.postValue(result.error.toCovidException())
                    }
                }
            }
        }
    }

    private suspend fun getCaptchaImage() {
        val result = robertManager.getCaptchaImage(captchaId, imagePath)
        loadingInProgress.postValue(false)
        when (result) {
            is RobertResult.Success -> imageSuccess.postValue(null)
            is RobertResult.Failure -> covidException.postValue(result.error.toCovidException())
        }
    }

    private suspend fun getCaptchaAudio() {
        val result = robertManager.getCaptchaAudio(captchaId, audioPath)
        loadingInProgress.postValue(false)
        when (result) {
            is RobertResult.Success -> audioSuccess.postValue(null)
            is RobertResult.Failure -> covidException.postValue(result.error.toCovidException())
        }
    }

    fun register(application: RobertApplication, captchaNextFragment: CaptchaNextFragment) {
        if (loadingInProgress.value == false) {
            viewModelScope.launch(Dispatchers.IO) {
                loadingInProgress.postValue(true)
                val result = robertManager.registerV2(application, code, captchaId, captchaNextFragment.activateProximity)
                loadingInProgress.postValue(false)
                when (result) {
                    is RobertResult.Success -> codeSuccess.postValue(captchaNextFragment)
                    is RobertResult.Failure -> covidException.postValue(result.error.toCovidException())
                }
            }
        }
    }
}

class CaptchaViewModelFactory(private val robertManager: RobertManager) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CaptchaViewModel(robertManager) as T
    }
}