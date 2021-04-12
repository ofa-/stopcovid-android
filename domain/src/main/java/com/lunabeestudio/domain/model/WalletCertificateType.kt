package com.lunabeestudio.domain.model

enum class WalletCertificateType {
    SANITARY {
        override val code: String = "B2"
        override val validationRegexp: Regex = "^[A-Z\\d]{4}([A-Z\\d]{4})([A-Z\\d]{4})[A-Z\\d]{8}B2([A-Z\\d]{4})F0([A-Z\\d\\s\\/]+)\\x1D?F1([A-Z\\s]+)\\x1D?F2(\\d{8})F3([FMU]{1})F4([A-Z\\d]{3,7})\\x1D?F5([PNIX]{1})F6(\\d{12})\\x1F{1}([A-Z\\d]{103})$".toRegex()
        override val stringKey: String = "sanitaryCertificate"
    };

    abstract val code: String
    abstract val stringKey: String
    abstract val validationRegexp: Regex
}