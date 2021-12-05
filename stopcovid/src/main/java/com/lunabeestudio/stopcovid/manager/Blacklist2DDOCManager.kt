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
import com.lunabeestudio.framework.local.dao.FrenchCertificateBlacklistRoomDao
import com.lunabeestudio.framework.local.model.FrenchCertificateBlacklistRoom
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.extension.blacklist2DdocIteration
import java.io.File

class Blacklist2DDOCManager(
    context: Context,
    serverManager: ServerManager,
    dao: FrenchCertificateBlacklistRoomDao,
    private val sharedPreferences: SharedPreferences,
) : RemoteProtoGzipRoomBlacklistManager<FrenchCertificateBlacklistRoom>(serverManager, dao) {
    override val remoteTemplateUrl: String = ConfigConstant.Blacklist.TwoDDOC.URL
    override val tmpFile: File = File(context.cacheDir, ConfigConstant.Blacklist.TwoDDOC.FILENAME)

    override var blacklistIteration: Int
        get() = sharedPreferences.blacklist2DdocIteration
        set(value) {
            sharedPreferences.blacklist2DdocIteration = value
        }

    override fun mapToRoom(hashList: List<String>): Array<FrenchCertificateBlacklistRoom> =
        hashList.map(::FrenchCertificateBlacklistRoom).toTypedArray()
}
