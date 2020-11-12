package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.stopcovid.model.AttestationMap
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@OptIn(ExperimentalTime::class)
fun AttestationMap.isExpired(qrCodeExpired: Duration): Boolean {
    val expired = System.currentTimeMillis().milliseconds - qrCodeExpired
    return (this["datetime"]?.value?.toLongOrNull()?.milliseconds ?: Duration.ZERO) < expired
}