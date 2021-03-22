/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/22/03 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import com.lunabeestudio.domain.model.Attestation
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.extension.attestationLongLabelFromKey
import com.lunabeestudio.stopcovid.extension.attestationShortLabelFromKey
import com.lunabeestudio.stopcovid.model.AttestationMap
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

object AttestationsManager {
    private val dateFormat: DateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT)
    private val timeFormat: DateFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)

    fun addAttestation(
        robertManager: RobertManager,
        keystoreDataSource: LocalKeystoreDataSource,
        strings: LocalizedStrings,
        attestation: AttestationMap
    ) {
        val attestations = keystoreDataSource.attestations?.toMutableList() ?: mutableListOf()
        attestations.add(
            Attestation(
                qrCode = attestationToFormattedString(robertManager, strings, attestation),
                footer = attestationToFooterString(robertManager, strings, attestation),
                qrCodeString = attestationToFormattedStringDisplayed(robertManager, strings, attestation),
                timestamp = attestation[Constants.Attestation.KEY_DATE_TIME]?.value?.toLongOrNull() ?: 0L,
                reason = attestation[Constants.Attestation.DATA_KEY_REASON]?.value ?: ""
            )
        )
        keystoreDataSource.attestations = attestations
    }

    private fun attestationToFormattedString(robertManager: RobertManager, strings: LocalizedStrings, attestation: AttestationMap): String {
        return robertManager.configuration.qrCodeFormattedString
            .attestationReplaceKnownValue(strings, attestation)
            .attestationReplaceUnknownValues(strings)
    }

    private fun attestationToFormattedStringDisplayed(robertManager: RobertManager,
        strings: LocalizedStrings,
        attestation: AttestationMap): String {
        return robertManager.configuration.qrCodeFormattedStringDisplayed
            .attestationReplaceKnownValue(strings, attestation)
            .attestationReplaceUnknownValues(strings)
    }

    private fun attestationToFooterString(robertManager: RobertManager, strings: LocalizedStrings, attestation: AttestationMap): String {
        return robertManager.configuration.qrCodeFooterString
            .attestationReplaceKnownValue(strings, attestation)
            .attestationReplaceUnknownValues(strings)
    }

    private fun String.attestationReplaceKnownValue(strings: LocalizedStrings, attestation: AttestationMap): String {
        var result = this
        attestation.keys.forEach { key ->
            when (attestation[key]?.type) {
                "date" -> {
                    attestation[key]?.value?.toLongOrNull()?.let { timestamp ->
                        val date = Date(timestamp)
                        result = result.replace("<$key>", dateFormat.format(date))
                    }
                }
                "datetime" -> {
                    attestation[key]?.value?.toLongOrNull()?.let { timestamp ->
                        val date = Date(timestamp)
                        result = result.replace("<$key>", "${dateFormat.format(date)}, ${timeFormat.format(date)}")
                            .replace("<$key-day>", dateFormat.format(date))
                            .replace("<$key-hour>", timeFormat.format(date))
                    }
                }
                "list" -> {
                    result = result.replace("<$key>", attestation[key]?.value ?: strings["qrCode.infoNotAvailable"] ?: "")
                        .replace(
                            "<$key-code>",
                            attestation[key]?.value?.split(".")?.getOrNull(1)
                                ?: attestation[key]?.value
                                ?: strings["qrCode.infoNotAvailable"] ?: ""
                        )
                        .replace(
                            "<$key-shortlabel>",
                            strings[attestation[key]?.value?.attestationShortLabelFromKey()] ?: strings["qrCode.infoNotAvailable"] ?: ""
                        )
                        .replace(
                            "<$key-longlabel>",
                            strings[attestation[key]?.value?.attestationLongLabelFromKey()] ?: strings["qrCode.infoNotAvailable"] ?: ""
                        )
                }
                else -> result = result.replace("<$key>", attestation[key]?.value ?: strings["qrCode.infoNotAvailable"] ?: "")
            }
        }
        return result
    }

    private fun String.attestationReplaceUnknownValues(strings: LocalizedStrings): String = replace(
        regex = Regex("<[a-zA-Z0-9\\-]+>"),
        strings["qrCode.infoNotAvailable"] ?: ""
    )

    fun migrateAttestationsIfNeeded(robertManager: RobertManager, keystoreDataSource: LocalKeystoreDataSource, strings: LocalizedStrings) {
        keystoreDataSource.deprecatedAttestations?.forEach { deprecatedAttestation ->
            addAttestation(robertManager, keystoreDataSource, strings, deprecatedAttestation)
        }
        keystoreDataSource.deprecatedAttestations = null
    }
}