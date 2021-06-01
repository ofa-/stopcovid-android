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
import com.lunabeestudio.robert.RobertConstant
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.model.ContactDateFormat
import com.lunabeestudio.stopcovid.model.RisksUILevel
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object RisksLevelManager : RemoteJsonManager<List<RisksUILevel>>() {

    override val type: Type = object : TypeToken<List<RisksUILevel>>() {}.type
    override val localFileName: String = ConfigConstant.Risks.FILENAME
    override val remoteFileUrl: String = ConfigConstant.Risks.URL
    override val assetFilePath: String = ConfigConstant.Risks.ASSET_FILE_PATH

    var risksLevels: List<RisksUILevel>? = null

    fun getLastContactDateFrom(riskLevel: Float?, lastContactDate: Long): Long? = when (getCurrentLevel(riskLevel)?.contactDateFormat) {
        ContactDateFormat.DATE -> lastContactDate
        ContactDateFormat.RANGE -> lastContactDate - TimeUnit.SECONDS.toMillis(RobertConstant.LAST_CONTACT_DELTA_S)
        else -> null
    }

    fun getLastContactDateTo(riskLevel: Float?, lastContactDate: Long): Long? = when (getCurrentLevel(riskLevel)?.contactDateFormat) {
        ContactDateFormat.RANGE -> lastContactDate + TimeUnit.SECONDS.toMillis(RobertConstant.LAST_CONTACT_DELTA_S)
        else -> null
    }

    fun getCurrentLevel(riskLevel: Float?): RisksUILevel? {
        return risksLevels?.let {
            it.firstOrNull { risksUILevel ->
                risksUILevel.riskLevel == riskLevel
            }
        }
    }

    suspend fun initialize(context: Context) {
        loadLocal(context)?.let { localRisksLevels ->
            if (risksLevels != localRisksLevels) {
                risksLevels = localRisksLevels
            }
        }
    }

    suspend fun onAppForeground(context: Context) {
        if (fetchLast(context)) {
            loadLocal(context)?.let { localRisksLevels ->
                if (this.risksLevels != localRisksLevels) {
                    this.risksLevels = localRisksLevels
                }
            }
        }
    }
}