/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/4 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.domain.model.smartwallet.SmartWalletValidityPivot
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.manager.model.ApiSmartWalletValidityPivot
import java.lang.reflect.Type

class SmartWalletValidityManager(
    serverManager: ServerManager
) : RemoteJsonManager<List<ApiSmartWalletValidityPivot>>(serverManager) {

    override val type: Type = object : TypeToken<List<ApiSmartWalletValidityPivot>>() {}.type
    override fun getLocalFileName(context: Context): String = ConfigConstant.SmartWallet.EXPIRATION_FILENAME
    override fun getRemoteFileUrl(context: Context): String = ConfigConstant.SmartWallet.URL + getLocalFileName(context)
    override fun getAssetFilePath(context: Context): String = ConfigConstant.SmartWallet.FOLDER + getLocalFileName(context)

    var smartWalletValidityPivot: List<SmartWalletValidityPivot<out Any>> = emptyList()
        private set

    suspend fun initialize(context: Context) {
        smartWalletValidityPivot = loadLocal(context)?.flatMap(ApiSmartWalletValidityPivot::toSmartWalletValidityPivots).orEmpty()
    }

    suspend fun onAppForeground(context: Context) {
        if (fetchLast(context)) {
            initialize(context)
        }
    }
}