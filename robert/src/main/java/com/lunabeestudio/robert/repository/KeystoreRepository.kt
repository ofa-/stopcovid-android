/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert.repository

import com.lunabeestudio.domain.model.ClientFilteringAlgorithmConfiguration
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource

internal class KeystoreRepository(
    private val keystoreDataSource: LocalKeystoreDataSource) {

    var sharedKey: ByteArray?
        get() = keystoreDataSource.sharedKey
        set(value) {
            keystoreDataSource.sharedKey = value
        }

    var timeStart: Long?
        get() = keystoreDataSource.timeStart
        set(value) {
            keystoreDataSource.timeStart = value
        }

    var atRisk: Boolean?
        get() = keystoreDataSource.atRisk
        set(value) {
            keystoreDataSource.atRisk = value
        }

    var lastExposureTimeframe: Int?
        get() = keystoreDataSource.lastExposureTimeframe
        set(value) {
            keystoreDataSource.lastExposureTimeframe = value
        }

    var proximityActive: Boolean?
        get() = keystoreDataSource.proximityActive
        set(value) {
            keystoreDataSource.proximityActive = value
        }

    var isSick: Boolean?
        get() = keystoreDataSource.isSick
        set(value) {
            keystoreDataSource.isSick = value
        }

    var filteringInfo: List<ClientFilteringAlgorithmConfiguration>?
        get() = keystoreDataSource.filteringInfo
        set(value) {
            keystoreDataSource.filteringInfo = value
        }
}