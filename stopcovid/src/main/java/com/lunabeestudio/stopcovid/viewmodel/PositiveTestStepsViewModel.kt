/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/27/7 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.viewmodel

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.repository.WalletRepository
import kotlinx.coroutines.launch

class PositiveTestStepsViewModel(
    private val handle: SavedStateHandle,
    private val walletRepository: WalletRepository,
) : ViewModel() {

    val currentStep: LiveData<Int> = handle.getLiveData(CURRENT_STEP_KEY, 0)

    fun saveCertificate(walletCertificate: WalletCertificate) {
        viewModelScope.launch {
            walletRepository.saveCertificate(walletCertificate)
            handle.set(CURRENT_STEP_KEY, (currentStep.value ?: 0) + 1)
        }
    }

    fun skipCertificate() {
        handle.set(CURRENT_STEP_KEY, (currentStep.value ?: 0) + 1)
    }

    companion object {
        private const val CURRENT_STEP_KEY: String = "CURRENT_STEP_KEY"
    }
}

class PositiveTestStepsViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val walletRepository: WalletRepository,
) : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        @Suppress("UNCHECKED_CAST")
        return PositiveTestStepsViewModel(handle, walletRepository) as T
    }
}
