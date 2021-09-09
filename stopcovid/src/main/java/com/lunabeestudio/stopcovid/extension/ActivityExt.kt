/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/31/8 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.app.Activity
import com.lunabeestudio.stopcovid.InjectionContainer
import com.lunabeestudio.stopcovid.StopCovid

val Activity.injectionContainer: InjectionContainer
    get() = (application as StopCovid).injectionContainer