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

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lunabeestudio.domain.model.Attestation
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource

class AttestationsViewModel(private val keystoreDataSource: LocalKeystoreDataSource) : ViewModel() {

    val attestations: LiveData<List<Attestation>?>
        get() = keystoreDataSource.attestationsLiveData

    fun removeAttestation(attestation: Attestation) {
        val attestations = keystoreDataSource.attestations?.toMutableList()
        attestations?.remove(attestation)
        keystoreDataSource.attestations = attestations
    }
}

class AttestationsViewModelFactory(private val secureKeystoreDataSource: SecureKeystoreDataSource) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AttestationsViewModel(secureKeystoreDataSource) as T
    }
}