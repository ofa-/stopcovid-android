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
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.model.ContactDateFormat
import com.lunabeestudio.stopcovid.model.RisksUILevel
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object RisksLevelManager : RemoteFileManager<List<RisksUILevel>>() {

    override val type: Type = object : TypeToken<List<RisksUILevel>>() {}.type
    override val localFileName: String = ConfigConstant.Risks.FILENAME
    override val remoteFileUrl: String = ConfigConstant.Risks.URL
    override val assetFilePath: String = ConfigConstant.Risks.ASSET_FILE_PATH

    private val _risksLevels: MutableLiveData<Event<List<RisksUILevel>>> = MutableLiveData()

    fun getLastContactDateFrom(riskLevel: Float?, lastContactDate: Long): Long? = when (getCurrentLevel(riskLevel)?.contactDateFormat) {
        ContactDateFormat.DATE -> lastContactDate
        ContactDateFormat.RANGE -> lastContactDate - TimeUnit.DAYS.toMillis(1)
        else -> null
    }

    fun getLastContactDateTo(riskLevel: Float?, lastContactDate: Long): Long? = when (getCurrentLevel(riskLevel)?.contactDateFormat) {
        ContactDateFormat.RANGE -> lastContactDate + TimeUnit.DAYS.toMillis(1)
        else -> null
    }

    val risksLevels: LiveData<Event<List<RisksUILevel>>>
        get() = _risksLevels

    fun getCurrentLevel(riskLevel: Float?): RisksUILevel? {
        return _risksLevels.value?.peekContent()?.let {
            it.firstOrNull { risksUILevel ->
                risksUILevel.riskLevel == riskLevel
            }
        }
    }

    suspend fun initialize(context: Context) {
        loadLocal(context)?.let { risksLevels ->
            if (_risksLevels.value?.peekContent() != risksLevels) {
                _risksLevels.postValue(Event(risksLevels))
            }
        }
    }

    suspend fun onAppForeground(context: Context) {
        if (fetchLast(context)) {
            loadLocal(context)?.let { risksLevels ->
                if (_risksLevels.value?.peekContent() != risksLevels) {
                    _risksLevels.postValue(Event(risksLevels))
                }
            }
        }
    }
}