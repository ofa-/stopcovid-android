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
import androidx.lifecycle.map
import com.lunabeestudio.stopcovid.manager.WalletManager
import com.lunabeestudio.stopcovid.model.WalletCertificate

class VaccineCompletionViewModel(
    private val certificateValue: String,
) : ViewModel() {

    val certificate: LiveData<WalletCertificate?> = WalletManager.walletCertificateLiveData.map { certificates ->
        certificates?.firstOrNull { walletCertificate ->
            walletCertificate.value == certificateValue
        }
    }
}

class VaccineCompletionViewModelFactory(
    private val certificateValue: String,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return VaccineCompletionViewModel(certificateValue) as T
    }
}