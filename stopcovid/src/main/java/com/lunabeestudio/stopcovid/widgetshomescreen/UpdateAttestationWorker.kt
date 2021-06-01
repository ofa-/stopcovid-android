/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/19/5 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.widgetshomescreen

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class UpdateAttestationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        AttestationWidget.updateWidget(applicationContext)
        return Result.success()
    }
}