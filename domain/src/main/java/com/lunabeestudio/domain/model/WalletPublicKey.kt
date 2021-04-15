package com.lunabeestudio.domain.model

data class WalletPublicKey(
    val auth: String,
    val pubKeys: Map<String, String>
)