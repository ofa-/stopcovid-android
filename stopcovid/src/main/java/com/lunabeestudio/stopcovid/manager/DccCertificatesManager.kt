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
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.EnvConstant
import com.lunabeestudio.stopcovid.model.DccCertificates
import java.lang.reflect.Type

class DccCertificatesManager : RemoteJsonManager<DccCertificates>() {

    override val type: Type = object : TypeToken<DccCertificates>() {}.type
    override val localFileName: String
        get() = EnvConstant.Prod.dccCertificatesFilename
    override val remoteFileUrl: String
        get() = ConfigConstant.DccCertificates.URL + localFileName
    override val assetFilePath: String = ConfigConstant.DccCertificates.FOLDER + localFileName

    var certificates: DccCertificates = emptyMap()
        private set

    suspend fun initialize(context: Context) {
        certificates = loadLocal(context) ?: emptyMap()
    }

    suspend fun onAppForeground(context: Context) {
        if (fetchLast(context)) {
            initialize(context)
        }
    }
}