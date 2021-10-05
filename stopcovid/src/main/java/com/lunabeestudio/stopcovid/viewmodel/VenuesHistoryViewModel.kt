/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/09/09 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.stopcovid.repository.VenueRepository
import kotlinx.coroutines.launch

class VenuesHistoryViewModel(
    private val keystoreDataSource: SecureKeystoreDataSource,
    private val venueRepository: VenueRepository,
) : ViewModel() {

    val venuesQrCodeLiveData: LiveData<List<VenueQrCode>> = venueRepository.venuesQrCodeFlow.asLiveData(timeoutInMs = 0)

    fun removeVenue(venueId: String) {
        viewModelScope.launch {
            venueRepository.removeVenue(keystoreDataSource, venueId)
        }
    }
}

class VenuesHistoryViewModelFactory(
    private val keystoreDataSource: SecureKeystoreDataSource,
    private val venueRepository: VenueRepository,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return VenuesHistoryViewModel(keystoreDataSource, venueRepository) as T
    }
}
