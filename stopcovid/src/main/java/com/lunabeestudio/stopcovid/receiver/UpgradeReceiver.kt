/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/03/06 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.service.ProximityService
import com.lunabeestudio.stopcovid.widgetshomescreen.AttestationWidget
import com.lunabeestudio.stopcovid.widgetshomescreen.KeyFiguresWidget
import com.lunabeestudio.stopcovid.widgetshomescreen.ProximityWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class UpgradeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED && context.robertManager().isProximityActive) {
                GlobalScope.launch(Dispatchers.Main) {
                    StringsManager.initialize(context)
                    ProximityService.start(context)
                }
            }
            if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                ProximityWidget.updateWidget(context)
                AttestationWidget.updateWidget(context)
                KeyFiguresWidget.updateWidget(context)
            }
        } catch (t: Throwable) {
            // Keychain may not be fully initialized
            Timber.e(t)
        }
    }
}
