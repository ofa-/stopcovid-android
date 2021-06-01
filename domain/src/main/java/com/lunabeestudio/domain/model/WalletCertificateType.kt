package com.lunabeestudio.domain.model

enum class WalletCertificateType {
    SANITARY {
        override val code: String = "B2"

        override val validationRegexp: Regex = "^[A-Z\\d]{4}" // Characters 0 to 3 are ignored. They represent the document format version.
            .plus("([A-Z\\d]{4})") // 1 - Characters 4 to 7 represent the document signing authority.
            .plus("([A-Z\\d]{4})") // 2 - Characters 8 to 11 represent the id of the certificate used to sign the document.
            .plus("[A-Z\\d]{8}") // Characters 12 to 19 are ignored.
            .plus("B2") // Characters 20 and 21 represent the wallet certificate type (sanitary, ...)
            .plus("[A-Z\\d]{4}") // Characters 22 to 25 are ignored.
            .plus("F0([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 3 - We capture the field F0. It must have at least one character.
            .plus("F1([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 4 - We capture the field F1. It must have at least one character.
            .plus("F2(\\d{8})") // 5 - We capture the field F2. It can only contain digits.
            .plus("F3([FMU]{1})") // 6 - We capture the field F3. It can only contain "F", "M" or "U".
            .plus("F4([A-Z\\d]{3,7})\\x1D?") // 7 - We capture the field F4. It can contain 3 to 7 uppercase letters and/or digits. It can also be ended by the GS ASCII char (29) if the field reaches its max length.
            .plus("F5([PNIX]{1})") // 8 - We capture the field F5. It can only contain "P", "N", "I" or "X".
            .plus("F6(\\d{12})") // 9 - We capture the field F6. It can only contain digits.
            .plus("\\x1F{1}") // This character is separating the message from its signature.
            .plus("([A-Z\\d\\=]+)$").toRegex() // 10 - This is the message signature.

        override val headerDetectionRegex: Regex = "^[A-Z\\d]{4}" // Characters 0 to 3 are ignored. They represent the document format version.
            .plus("([A-Z\\d]{4})") // 1 - Characters 4 to 7 represent the document signing authority.
            .plus("([A-Z\\d]{4})") // 2 - Characters 8 to 11 represent the id of the certificate used to sign the document.
            .plus("[A-Z\\d]{8}") // Characters 12 to 19 are ignored.
            .plus("B2") // Characters 20 and 21 represent the wallet certificate type (sanitary, ...)
            .toRegex()
        override val stringKey: String = "sanitaryCertificate"
    },
    VACCINATION {
        override val code: String = "L1"

        override val validationRegexp: Regex = "^[A-Z\\d]{4}" // Characters 0 to 3 are ignored. They represent the document format version.
            .plus("([A-Z\\d]{4})") // 1 - Characters 4 to 7 represent the document signing authority.
            .plus("([A-Z\\d]{4})") // 2 - Characters 8 to 11 represent the id of the certificate used to sign the document.
            .plus("[A-Z\\d]{8}") // Characters 12 to 19 are ignored.
            .plus("L1") // Characters 20 and 21 represent the wallet certificate type (sanitary, ...)
            .plus("[A-Z\\d]{4}") // Characters 22 to 25 are ignored.
            .plus("L0([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 3 - We capture the field L0. It can contain uppercased letters and spaces. It can also be ended by the GS ASCII char (29) if the field reaches its max length.
            .plus("L1([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 4 - We capture the field L1. It must have at least one character.
            .plus("L2(\\d{8})\\x1D?") // 5 - We capture the field L2. It can only contain 8 digits.
            .plus("L3([^\\x1D\\x1E]*)[\\x1D\\x1E]") // // 6 - We capture the field L3. It can contain any characters.
            .plus("L4([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 7 - We capture the field L4. It must have at least one character
            .plus("L5([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 8 - We capture the field L5. It must have at least one character
            .plus("L6([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 9 - We capture the field L6. It must have at least one character
            .plus("L7(\\d{1})") // 10 - We capture the field L7. It can contain only one digit.
            .plus("L8(\\d{1})") // 11 - We capture the field L8. It can contain only one digit.
            .plus("L9(\\d{8})") // 12 - We capture the field L9. It can only contain 8 digits.
            .plus("LA([A-Z\\d]{2})") // 13 - We capture the field LA. 2 characters letters or digits
            .plus("\\x1F{1}") // This character is separating the message from its signature.
            .plus("([A-Z\\d\\=]+)$").toRegex() // 14 - This is the message signature.

        override val headerDetectionRegex: Regex = "^[A-Z\\d]{4}" // Characters 0 to 3 are ignored. They represent the document format version.
            .plus("([A-Z\\d]{4})") // 1 - Characters 4 to 7 represent the document signing authority.
            .plus("([A-Z\\d]{4})") // 2 - Characters 8 to 11 represent the id of the certificate used to sign the document.
            .plus("[A-Z\\d]{8}") // Characters 12 to 19 are ignored.
            .plus("L1") // Characters 20 and 21 represent the wallet certificate type (sanitary, ...)
            .toRegex()

        override val stringKey: String = "vaccinationCertificate"
    };

    abstract val code: String
    abstract val stringKey: String
    abstract val validationRegexp: Regex
    abstract val headerDetectionRegex: Regex
}