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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.repository.VenueRepository
import kotlinx.coroutines.launch

class VenueQrCodeViewModel(
    private val robertManager: RobertManager,
    private val venueRepository: VenueRepository,
) : ViewModel() {
    val venueProcessed: SingleLiveEvent<Unit> = SingleLiveEvent()
    val exception: SingleLiveEvent<Exception> = SingleLiveEvent()

    fun processVenue(venueContent: String, venueVersion: String, venueTime: String?) {
        viewModelScope.launch {
            try {
                venueRepository.processVenue(
                    robertManager = robertManager,
                    base64URLCode = venueContent,
                    version = venueVersion.toInt(),
                    unixTimeInSeconds = venueTime?.toLongOrNull(),
                )
                venueProcessed.postValue(null)
            } catch (e: Exception) {
                exception.postValue(e)
            }
        }
    }

    fun processVenueUrl(code: String) {
        viewModelScope.launch {
            try {
                venueRepository.processVenueUrl(
                    robertManager = robertManager,
                    code
                )
                venueProcessed.postValue(null)
            } catch (e: Exception) {
                exception.postValue(e)
            }
        }
    }
}

class VenueQrCodeViewModelFactory(
    private val robertManager: RobertManager,
    private val venueRepository: VenueRepository,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return VenueQrCodeViewModel(robertManager, venueRepository) as T
    }
}
