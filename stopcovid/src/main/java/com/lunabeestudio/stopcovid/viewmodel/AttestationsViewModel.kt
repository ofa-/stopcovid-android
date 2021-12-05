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

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.domain.model.Attestation
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.stopcovid.repository.AttestationRepository
import com.lunabeestudio.stopcovid.widgetshomescreen.AttestationWidget
import kotlinx.coroutines.launch

class AttestationsViewModel(
    private val keystoreDataSource: LocalKeystoreDataSource,
    attestationRepository: AttestationRepository,
) : ViewModel() {

    val attestations: LiveData<List<Attestation>> = attestationRepository.attestationsFlow.asLiveData(timeoutInMs = 0)

    fun deleteAttestation(context: Context?, attestation: Attestation) {
        viewModelScope.launch {
            keystoreDataSource.deleteAttestation(attestation.id)
            context?.let { AttestationWidget.updateWidget(it, false) }
        }
    }

    fun deleteDeprecatedAttestations() {
        keystoreDataSource.deleteDeprecatedAttestations()
    }
}

class AttestationsViewModelFactory(
    private val secureKeystoreDataSource: SecureKeystoreDataSource,
    private val attestationRepository: AttestationRepository,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AttestationsViewModel(secureKeystoreDataSource, attestationRepository) as T
    }
}