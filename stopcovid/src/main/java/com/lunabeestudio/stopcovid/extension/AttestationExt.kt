package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.domain.model.Attestation
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.stopcovid.Constants
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun Attestation.isExpired(configuration: Configuration): Boolean {
    val qrCodeExpired: Duration = when (reason) {
        Constants.Attestation.VALUE_REASON_SPORT -> Duration.hours(3)
        else -> Duration.hours(configuration.qrCodeExpiredHours.toDouble())
    }
    val attestationDuration = Duration.milliseconds(System.currentTimeMillis() - timestamp)
    return attestationDuration > qrCodeExpired
}

@OptIn(ExperimentalTime::class)
fun Attestation.isObsolete(configuration: Configuration): Boolean {
    val qrCodeObsolete: Duration = Duration.hours(configuration.qrCodeDeletionHours.toDouble())
    val attestationDuration = Duration.milliseconds(System.currentTimeMillis() - timestamp)
    return attestationDuration > qrCodeObsolete
}