package com.lunabeestudio.domain.model

enum class WalletCertificateType(val code: String, val format: Format) {
    SANITARY("B2", Format.WALLET_2D),
    VACCINATION("L1", Format.WALLET_2D),
    SANITARY_EUROPE("test", Format.WALLET_DCC),
    VACCINATION_EUROPE("vaccine", Format.WALLET_DCC),
    RECOVERY_EUROPE("recovery", Format.WALLET_DCC),
    EXEMPTION("exemption", Format.WALLET_DCC),
    ACTIVITY_PASS("activity", Format.WALLET_DCC);

    enum class Format(val values: List<String>) {
        WALLET_2D(listOf("wallet2d", "wallet")), WALLET_DCC(listOf("walletdcc"));

        companion object {
            fun fromValue(value: String): Format? = values().firstOrNull { it.values.contains(value) }
        }
    }
}
