package com.lunabeestudio.domain.model

data class RawWalletCertificate(
    val id: String,
    val type: WalletCertificateType,
    val value: String,
    val timestamp: Long,
    var isFavorite: Boolean
)