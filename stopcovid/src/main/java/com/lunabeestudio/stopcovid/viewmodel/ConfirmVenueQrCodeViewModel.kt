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
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.repository.VenueRepository
import kotlinx.coroutines.launch

class ConfirmVenueQrCodeViewModel(
    private val robertManager: RobertManager,
    private val analyticsManager: AnalyticsManager,
    private val venueRepository: VenueRepository,
) : ViewModel() {
    val venueProcessed: SingleLiveEvent<Unit> = SingleLiveEvent()
    val exception: SingleLiveEvent<Exception> = SingleLiveEvent()

    fun processVenue(venueContent: String, venueVersion: Int, venueTime: String?) {
        viewModelScope.launch {
            try {
                venueRepository.processVenue(
                    robertManager = robertManager,
                    venueContent,
                    venueVersion,
                    venueTime?.toLong()
                )
                analyticsManager.reportAppEvent(AppEventName.e14, null)
                venueProcessed.postValue(Unit)
            } catch (e: Exception) {
                exception.postValue(e)
            }
        }
    }
}

class ConfirmVenueQrCodeViewModelFactory(
    private val robertManager: RobertManager,
    private val analyticsManager: AnalyticsManager,
    private val venueRepository: VenueRepository,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ConfirmVenueQrCodeViewModel(
            robertManager,
            analyticsManager,
            venueRepository,
        ) as T
    }
}