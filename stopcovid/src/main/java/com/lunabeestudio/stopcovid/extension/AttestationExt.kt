package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.domain.model.Attestation
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.model.AttestationMap
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.hours
import kotlin.time.milliseconds

@OptIn(ExperimentalTime::class)
fun Attestation.isExpired(robertManager: RobertManager): Boolean {
    val qrCodeExpired: Duration = when (reason) {
        Constants.Attestation.VALUE_REASON_SPORT -> 3.hours
        else -> robertManager.configuration.qrCodeExpiredHours.toDouble().hours
    }
    val attestationDuration = System.currentTimeMillis().milliseconds - timestamp.milliseconds
    return attestationDuration > qrCodeExpired
}