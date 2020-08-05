/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/06/30 - for the STOP-COVID project
 */

package com.orange.proximitynotification.tools

import android.os.SystemClock
import java.util.Date

internal fun Long.nanosTimestampToDate(): Date =
    Date(System.currentTimeMillis() - SystemClock.elapsedRealtime() + (this / 1000000))