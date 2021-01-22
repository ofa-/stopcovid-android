/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.datasource

import androidx.lifecycle.LiveData
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.FormEntry
import com.lunabeestudio.domain.model.VenueQrCode

interface LocalKeystoreDataSource {
    var configuration: Configuration?
    var shouldReloadBleSettings: Boolean?
    var isRegistered: Boolean
    var kA: ByteArray?
    var kEA: ByteArray?
    var timeStart: Long?
    var isWarningAtRisk: Boolean?
    var atRiskLastRefresh: Long?
    var atRiskLastError: Long?
    var lastExposureTimeframe: Int?
    var proximityActive: Boolean?
    var isSick: Boolean?
    var lastRiskReceivedDate: Long?
    val attestationsLiveData: LiveData<List<Map<String, FormEntry>>?>
    var attestations: List<Map<String, FormEntry>>?
    var savedAttestationData: Map<String, FormEntry>?
    var saveAttestationData: Boolean?
    var reportDate: Long?
    var reportValidationToken: String?
    var reportToSendTime: Long?
    var venuesQrCode: List<VenueQrCode>?

    var reportPositiveTestDate: Long?
    var reportSymptomsStartDate: Long?
    var lastWarningReceivedDate: Long?

    // Isolation vars
    var isolationLastContactDate: Long?
    var isolationIsKnownIndexAtHome: Boolean?
    var isolationKnowsIndexSymptomsEndDate: Boolean?
    var isolationIndexSymptomsEndDate: Long?
    var isolationLastFormValidationDate: Long?
    var isolationIsTestNegative: Boolean?
    var isolationPositiveTestingDate: Long?
    var isolationIsHavingSymptoms: Boolean?
    var isolationSymptomsStartDate: Long?
    var isolationIsStillHavingFever: Boolean?
    var isolationIsFeverReminderScheduled: Boolean?
    var isolationFormState: Int?
}
