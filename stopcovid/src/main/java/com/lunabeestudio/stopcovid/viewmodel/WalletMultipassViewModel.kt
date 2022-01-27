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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lunabeestudio.stopcovid.model.MultipassProfile
import com.lunabeestudio.stopcovid.usecase.GetCloseMultipassProfilesUseCase
import com.lunabeestudio.stopcovid.usecase.GetMultipassProfilesUseCase

class WalletMultipassViewModel(
    private val getMultipassProfilesUseCase: GetMultipassProfilesUseCase,
    private val getCloseMultipassProfilesUseCase: GetCloseMultipassProfilesUseCase,
) : ViewModel() {

    fun getMultipassProfiles(): List<MultipassProfile> = getMultipassProfilesUseCase()

    fun getCloseMultipassProfiles(profiles: List<MultipassProfile>): List<MultipassProfile> =
        getCloseMultipassProfilesUseCase(profiles)
}

class WalletMultipassViewModelFactory(
    private val getMultipassProfilesUseCase: GetMultipassProfilesUseCase,
    private val getCloseMultipassProfilesUseCase: GetCloseMultipassProfilesUseCase,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WalletMultipassViewModel(
            getMultipassProfilesUseCase,
            getCloseMultipassProfilesUseCase,
        ) as T
    }
}
