package com.lunabeestudio.domain.model

import java.util.Date

data class RawWalletCertificate(
    val id: String,
    val type: WalletCertificateType,
    val value: String,
    val timestamp: Long,
    var isFavorite: Boolean,
    @Transient
    var expireAt: Date? = null,
    @Transient
    var rootWalletCertificateId: String? = null,
    var canRenewDccLight: Boolean?,
)