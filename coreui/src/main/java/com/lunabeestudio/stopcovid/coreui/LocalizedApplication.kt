/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/27/8 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui

import androidx.lifecycle.LiveData
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings

interface LocalizedApplication {
    suspend fun initializeStrings()
    val localizedStrings: LocalizedStrings
    val liveLocalizedStrings: LiveData<Event<LocalizedStrings>>
}