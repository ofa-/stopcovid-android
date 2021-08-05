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

object BlacklistManager : RemoteJsonManager<List<String>>() {

    override val type: Type = object : TypeToken<List<String>>() {}.type
    override val localFileName: String = ConfigConstant.Blacklist.FILENAME
    override val remoteFileUrl: String = ConfigConstant.Blacklist.URL
    override val assetFilePath: String = ConfigConstant.Blacklist.ASSET_FILE_PATH

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
}