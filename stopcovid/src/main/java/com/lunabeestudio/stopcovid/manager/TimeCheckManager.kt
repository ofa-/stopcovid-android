/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/11/06 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import com.lunabeestudio.stopcovid.Constants
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

object TimeCheckManager {

    fun isTimeAlignedWithServer(response: Response): Boolean? {
        return response.header("Date")?.let { serverDateString ->
            val serverDate = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).parse(serverDateString)!!
            abs(serverDate.time - System.currentTimeMillis()) < Constants.ServerConstant.MAX_GAP_DEVICE_SERVER
        }
    }
}