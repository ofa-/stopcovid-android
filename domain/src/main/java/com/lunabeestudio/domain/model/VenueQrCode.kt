package com.lunabeestudio.domain.model

data class VenueQrCode(
    val id: String,
    val ltid: String,
    val ntpTimestamp: Long,
    val base64URL: String,
    val version: Int
)
