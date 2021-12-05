package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.domain.model.Attestation
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.stopcovid.Constants
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

fun Attestation.isExpired(configuration: Configuration): Boolean {
    val qrCodeExpired: Duration = when (reason) {
        Constants.Attestation.VALUE_REASON_SPORT -> 3.hours
        else -> configuration.qrCodeExpiredHours.toDouble().hours
    }
    val attestationDuration = (System.currentTimeMillis() - timestamp).milliseconds
    return attestationDuration > qrCodeExpired
}

fun Attestation.isObsolete(configuration: Configuration): Boolean {
    val qrCodeObsolete: Duration = configuration.qrCodeDeletionHours.toDouble().hours
    val attestationDuration = (System.currentTimeMillis() - timestamp).milliseconds
    return attestationDuration > qrCodeObsolete
}