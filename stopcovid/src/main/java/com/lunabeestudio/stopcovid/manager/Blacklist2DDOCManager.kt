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
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import java.lang.reflect.Type

object Blacklist2DDOCManager : RemoteJsonManager<List<String>>() {

    override val type: Type = object : TypeToken<List<String>>() {}.type
    override val localFileName: String = ConfigConstant.Blacklist2DDOC.FILENAME
    override val remoteFileUrl: String = ConfigConstant.Blacklist2DDOC.URL
    override val assetFilePath: String = ConfigConstant.Blacklist2DDOC.ASSET_FILE_PATH

    private val _blacklisted2DDOCHashes: MutableLiveData<List<String>?> = MutableLiveData(null)
    val blacklisted2DDOCHashes: LiveData<List<String>?>
        get() = _blacklisted2DDOCHashes

    suspend fun initialize(context: Context) {
        loadLocal(context)?.let { localBlacklisted2DDOC ->
            if (blacklisted2DDOCHashes.value != localBlacklisted2DDOC) {
                _blacklisted2DDOCHashes.postValue(localBlacklisted2DDOC)
            }
        }
    }

    suspend fun onAppForeground(context: Context) {
        if (fetchLast(context)) {
            loadLocal(context)?.let { localBlacklisted2DDOC ->
                if (blacklisted2DDOCHashes.value != localBlacklisted2DDOC) {
                    _blacklisted2DDOCHashes.postValue(localBlacklisted2DDOC)
                }
            }
        }
    }
}