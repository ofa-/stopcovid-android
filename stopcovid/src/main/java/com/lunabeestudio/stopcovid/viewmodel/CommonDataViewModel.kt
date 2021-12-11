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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.manager.IsolationManager
import com.lunabeestudio.stopcovid.manager.VaccinationCenterManager
import com.lunabeestudio.stopcovid.repository.VenueRepository
import com.lunabeestudio.stopcovid.repository.WalletRepository
import com.lunabeestudio.stopcovid.widgetshomescreen.AttestationWidget
import kotlinx.coroutines.launch

abstract class CommonDataViewModel(
    private val secureKeystoreDataSource: SecureKeystoreDataSource,
    private val robertManager: RobertManager,
    private val isolationManager: IsolationManager,
    private val vaccinationCenterManager: VaccinationCenterManager,
    private val venueRepository: VenueRepository,
    private val walletRepository: WalletRepository,
) : ViewModel() {

    @CallSuper
    open fun eraseVenues(application: RobertApplication) {
        viewModelScope.launch {
            venueRepository.clearAllData()
        }
    }

    @CallSuper
    open fun eraseAttestations(context: Context) {
        viewModelScope.launch {
            secureKeystoreDataSource.savedAttestationData = null
            secureKeystoreDataSource.saveAttestationData = null
            secureKeystoreDataSource.deleteAllAttestations()
            @Suppress("DEPRECATION")
            secureKeystoreDataSource.deprecatedAttestations = null
            AttestationWidget.updateWidget(context)
        }
    }

    @CallSuper
    open fun eraseIsolation() {
        isolationManager.resetData()
    }

    @CallSuper
    open fun eraseCertificates() {
        viewModelScope.launch {
            walletRepository.deleteAllCertificates()
        }
    }

    protected suspend fun clearLocalData(application: RobertApplication) {
        robertManager.clearLocalData(application)
        WorkManager.getInstance(application.getAppContext())
            .cancelUniqueWork(Constants.WorkerNames.AT_RISK_NOTIFICATION)
        clearNotifications(application)
        eraseAttestations(application.getAppContext())
        eraseVenues(application)
        (application.getAppContext() as StopCovid).cancelActivateReminder()
        eraseIsolation()
        eraseCertificates()
        vaccinationCenterManager.clearAllData(application.getAppContext())
    }

    protected fun clearNotifications(application: RobertApplication) {
        val notificationManager = application.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(UiConstants.Notification.ERROR.notificationId)
        notificationManager.cancel(UiConstants.Notification.TIME.notificationId)
        notificationManager.cancel(UiConstants.Notification.AT_RISK.notificationId)
    }
}