package com.lunabeestudio.stopcovid.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.model.CovidException
import com.lunabeestudio.stopcovid.model.NeedRegisterException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HealthViewModel(private val robertManager: RobertManager) : ViewModel() {

    val loadingInProgress: MutableLiveData<Boolean> = MutableLiveData(false)
    val eraseNotificationSuccess: SingleLiveEvent<Unit> = SingleLiveEvent()
    val covidException: SingleLiveEvent<CovidException> = SingleLiveEvent()

    fun clearNotification() {

        if (robertManager.isRegistered) {
            if (loadingInProgress.value == false) {
                viewModelScope.launch(Dispatchers.IO) {
                    loadingInProgress.postValue(true)
                    when (val result = robertManager.eraseRemoteAlert()) {
                        is RobertResult.Success -> eraseNotificationSuccess.postValue(null)
                        is RobertResult.Failure -> covidException.postValue(result.error.toCovidException())
                    }
                    loadingInProgress.postValue(false)
                }
            }
        } else {
            covidException.postValue(NeedRegisterException())
        }
    }

}

class HealthViewModelFactory(private val robertManager: RobertManager) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HealthViewModel(robertManager) as T
    }
}