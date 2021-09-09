/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/28/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.content.Context
import com.lunabeestudio.domain.model.Calibration
import com.lunabeestudio.robert.datasource.RobertCalibrationDataSource
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.stopcovid.coreui.manager.CalibrationManager
import com.lunabeestudio.stopcovid.extension.remoteToRobertException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class TacCalibrationDataSource(private val calibrationManager: CalibrationManager) : RobertCalibrationDataSource {

    override suspend fun fetchOrLoadCalibration(context: Context): RobertResultData<Calibration> {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            try {
                val calibration = calibrationManager.fetchOrLoad(context)
                RobertResultData.Success(calibration)
            } catch (e: Exception) {
                Timber.e(e)
                RobertResultData.Failure(e.remoteToRobertException())
            }
        }
    }

    override fun loadCalibration(context: Context): Calibration = calibrationManager.load(context)
}