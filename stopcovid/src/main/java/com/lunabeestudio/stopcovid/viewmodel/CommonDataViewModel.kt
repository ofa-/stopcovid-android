/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.viewmodel

import android.app.NotificationManager
import android.content.Context
import androidx.annotation.CallSuper
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.manager.IsolationManager
import com.lunabeestudio.stopcovid.manager.VaccinationCenterManager
import com.lunabeestudio.stopcovid.manager.VenuesManager

abstract class CommonDataViewModel(
    private val secureKeystoreDataSource: SecureKeystoreDataSource,
    private val robertManager: RobertManager,
    private val isolationManager: IsolationManager,
) : ViewModel() {

    @CallSuper
    open fun eraseVenues(application: RobertApplication) {
        VenuesManager.clearAllData(PreferenceManager.getDefaultSharedPreferences(application.getAppContext()), secureKeystoreDataSource)
    }

    @CallSuper
    open fun eraseAttestations() {
        secureKeystoreDataSource.savedAttestationData = null
        secureKeystoreDataSource.saveAttestationData = null
        secureKeystoreDataSource.attestations = null
        secureKeystoreDataSource.deprecatedAttestations = null
    }

    @CallSuper
    open fun eraseIsolation() {
        isolationManager.resetData()
    }

    protected suspend fun clearLocalData(application: RobertApplication) {
        robertManager.clearLocalData(application)
        WorkManager.getInstance(application.getAppContext())
            .cancelUniqueWork(Constants.WorkerNames.AT_RISK_NOTIFICATION)
        clearNotifications(application)
        eraseAttestations()
        eraseVenues(application)
        (application.getAppContext() as StopCovid).cancelActivateReminder()
        eraseIsolation()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application.getAppContext())
        VaccinationCenterManager.clearAllData(sharedPreferences)
        sharedPreferences.edit {
            remove(Constants.SharedPrefs.PRIVATE_EVENT_QR_CODE_GENERATION_DATE)
            remove(Constants.SharedPrefs.PRIVATE_EVENT_QR_CODE)
        }
    }

    protected fun clearNotifications(application: RobertApplication) {
        val notificationManager = application.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(UiConstants.Notification.ERROR.notificationId)
        notificationManager.cancel(UiConstants.Notification.TIME.notificationId)
        notificationManager.cancel(UiConstants.Notification.AT_RISK.notificationId)
    }
}