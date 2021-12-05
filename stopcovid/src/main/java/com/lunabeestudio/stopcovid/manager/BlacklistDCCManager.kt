/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/2/11 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.content.Context
import android.content.SharedPreferences
import com.lunabeestudio.framework.local.dao.EuropeanCertificateBlacklistRoomDao
import com.lunabeestudio.framework.local.model.EuropeanCertificateBlacklistRoom
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.extension.blacklistDccIteration
import java.io.File

class BlacklistDCCManager(
    context: Context,
    serverManager: ServerManager,
    dao: EuropeanCertificateBlacklistRoomDao,
    private val sharedPreferences: SharedPreferences,
) : RemoteProtoGzipRoomBlacklistManager<EuropeanCertificateBlacklistRoom>(serverManager, dao) {
    override val remoteTemplateUrl: String = ConfigConstant.Blacklist.DCC.URL
    override val tmpFile: File = File(context.cacheDir, ConfigConstant.Blacklist.DCC.FILENAME)

    override var blacklistIteration: Int
        get() = sharedPreferences.blacklistDccIteration
        set(value) {
            sharedPreferences.blacklistDccIteration = value
        }

    override fun mapToRoom(hashList: List<String>): Array<EuropeanCertificateBlacklistRoom> =
        hashList.map(::EuropeanCertificateBlacklistRoom).toTypedArray()
}
