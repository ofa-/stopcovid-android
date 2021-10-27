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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import blacklist.Blacklist
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import java.util.zip.GZIPInputStream

class BlacklistDCCManager(serverManager: ServerManager) : RemoteProtoGzipManager<Blacklist.BlackListMessage, List<String>>(serverManager) {

    override fun getLocalFileName(context: Context): String = ConfigConstant.BlacklistDCC.FILENAME
    override fun getRemoteFileUrl(context: Context): String = ConfigConstant.BlacklistDCC.URL
    override fun getAssetFilePath(context: Context): String = ConfigConstant.BlacklistDCC.ASSET_FILE_PATH

    private val _blacklistedDCCHashes: MutableLiveData<List<String>?> = MutableLiveData(null)
    val blacklistedDCCHashes: LiveData<List<String>?>
        get() = _blacklistedDCCHashes

    suspend fun initialize(context: Context) {
        loadLocal(context)?.let { localBlacklistedDcc ->
            if (blacklistedDCCHashes.value != localBlacklistedDcc) {
                _blacklistedDCCHashes.postValue(localBlacklistedDcc)
            }
        }
    }

    suspend fun onAppForeground(context: Context) {
        if (fetchLast(context)) {
            loadLocal(context)?.let { localBlacklistedDcc ->
                if (blacklistedDCCHashes.value != localBlacklistedDcc) {
                    _blacklistedDCCHashes.postValue(localBlacklistedDcc)
                }
            }
        }
    }

    override fun parseProtoGzipStream(gzipInputStream: GZIPInputStream): Blacklist.BlackListMessage {
        return Blacklist.BlackListMessage.parseFrom(gzipInputStream)
    }

    override fun Blacklist.BlackListMessage.mapProtoToApp(): List<String> {
        return this.itemsList
    }
}