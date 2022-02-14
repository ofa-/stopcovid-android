/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.domain.model.smartwallet.SmartWalletEligibilityPivot
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.manager.model.ApiSmartWalletEligibilityPivot
import java.lang.reflect.Type

class SmartWalletEligibilityManager(
    serverManager: ServerManager
) : RemoteJsonManager<List<ApiSmartWalletEligibilityPivot>>(serverManager) {

    override val type: Type = object : TypeToken<List<ApiSmartWalletEligibilityPivot>>() {}.type
    override fun getLocalFileName(context: Context): String = ConfigConstant.SmartWallet.ELIGIBILITY_FILENAME
    override fun getRemoteFileUrl(context: Context): String = ConfigConstant.SmartWallet.URL + getLocalFileName(context)
    override fun getAssetFilePath(context: Context): String = ConfigConstant.SmartWallet.FOLDER + getLocalFileName(context)

    var smartWalletEligibilityPivot: List<SmartWalletEligibilityPivot> = emptyList()
        private set

    suspend fun initialize(context: Context) {
        smartWalletEligibilityPivot = loadLocal(context)?.flatMap(ApiSmartWalletEligibilityPivot::toSmartWalletEligibilityPivots)
            ?: emptyList()
    }

    suspend fun onAppForeground(context: Context) {
        if (fetchLast(context)) {
            initialize(context)
        }
    }
}