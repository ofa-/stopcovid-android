/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/24/6 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.repository.WalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VaccineCompletionViewModel(
    certificateId: String,
    private val walletRepository: WalletRepository,
    private val dispatcherIO: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    val certificate: LiveData<WalletCertificate?> = walletRepository.getById(certificateId).asLiveData(timeoutInMs = 0)

    val showWalletEvent: SingleLiveEvent<Unit> = SingleLiveEvent()

    fun addCertificateInFavorite() {
        viewModelScope.launch(dispatcherIO) {
            (certificate.value as? EuropeanCertificate)?.let {
                if (!it.isFavorite) {
                    walletRepository.toggleFavorite(
                        it,
                    )
                }
            }
            showWalletEvent.postValue(null)
        }
    }
}

class VaccineCompletionViewModelFactory(
    private val certificateId: String,
    private val walletRepository: WalletRepository,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return VaccineCompletionViewModel(
            certificateId,
            walletRepository,
        ) as T
    }
}