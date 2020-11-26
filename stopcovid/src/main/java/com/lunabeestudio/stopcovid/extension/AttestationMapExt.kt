package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.model.AttestationMap
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.hours
import kotlin.time.milliseconds

@OptIn(ExperimentalTime::class)
fun AttestationMap.isExpired(robertManager: RobertManager): Boolean {
    val qrCodeExpired: Duration = when (this[Constants.Attestation.KEY_REASON]?.value) {
        Constants.Attestation.VALUE_REASON_SPORT -> 3.hours
        else -> robertManager.qrCodeExpiredHours.toDouble().hours
    }
    val attestationDuration = System.currentTimeMillis().milliseconds - (this[Constants.Attestation.KEY_DATE_TIME]?.value?.toLongOrNull()?.milliseconds
        ?: Duration.ZERO)
    return attestationDuration > qrCodeExpired
}