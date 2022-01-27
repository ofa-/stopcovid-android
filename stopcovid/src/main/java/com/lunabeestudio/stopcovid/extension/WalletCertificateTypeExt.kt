/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/14/6 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import com.google.zxing.BarcodeFormat
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.ConfigConstant

val WalletCertificateType.stringKey: String
    get() = when (this) {
        WalletCertificateType.SANITARY -> "sanitaryCertificate"
        WalletCertificateType.VACCINATION -> "vaccinationCertificate"
        WalletCertificateType.SANITARY_EUROPE -> "sanitaryEurope"
        WalletCertificateType.VACCINATION_EUROPE -> "vaccinationEurope"
        WalletCertificateType.RECOVERY_EUROPE -> "recoveryEurope"
        WalletCertificateType.EXEMPTION -> "exemption"
        WalletCertificateType.DCC_LIGHT -> "activityPass"
        WalletCertificateType.MULTI_PASS -> "multiPass"
    }

val WalletCertificateType.errorStringKey: String
    get() = when (this) {
        WalletCertificateType.SANITARY -> "testCertificate"
        WalletCertificateType.VACCINATION -> "vaccinCertificate"
        WalletCertificateType.SANITARY_EUROPE -> "sanitaryEurope"
        WalletCertificateType.VACCINATION_EUROPE -> "vaccinationEurope"
        WalletCertificateType.RECOVERY_EUROPE -> "recoveryEurope"
        WalletCertificateType.EXEMPTION -> "exemption"
        WalletCertificateType.DCC_LIGHT -> "activityPass"
        WalletCertificateType.MULTI_PASS -> "multiPass"
    }

val WalletCertificateType.certificateFilename: String?
    get() = when (this) {
        WalletCertificateType.SANITARY -> ConfigConstant.Wallet.TEST_CERTIFICATE_FULL_FILE
        WalletCertificateType.VACCINATION -> ConfigConstant.Wallet.VACCIN_CERTIFICATE_FULL_FILE
        WalletCertificateType.SANITARY_EUROPE -> ConfigConstant.Wallet.TEST_EUROPE_CERTIFICATE_FULL_FILE
        WalletCertificateType.VACCINATION_EUROPE -> ConfigConstant.Wallet.VACCIN_EUROPE_CERTIFICATE_FULL_FILE
        WalletCertificateType.RECOVERY_EUROPE -> ConfigConstant.Wallet.RECOVERY_EUROPE_CERTIFICATE_FULL_FILE
        WalletCertificateType.EXEMPTION,
        WalletCertificateType.DCC_LIGHT,
        WalletCertificateType.MULTI_PASS -> null
    }

val WalletCertificateType.certificateThumbnailFilename: String?
    get() = when (this) {
        WalletCertificateType.SANITARY -> ConfigConstant.Wallet.TEST_CERTIFICATE_THUMBNAIL_FILE
        WalletCertificateType.VACCINATION -> ConfigConstant.Wallet.VACCIN_CERTIFICATE_THUMBNAIL_FILE
        WalletCertificateType.SANITARY_EUROPE -> ConfigConstant.Wallet.TEST_EUROPE_CERTIFICATE_THUMBNAIL_FILE
        WalletCertificateType.VACCINATION_EUROPE -> ConfigConstant.Wallet.VACCIN_EUROPE_CERTIFICATE_THUMBNAIL_FILE
        WalletCertificateType.RECOVERY_EUROPE -> ConfigConstant.Wallet.RECOVERY_EUROPE_CERTIFICATE_THUMBNAIL_FILE
        WalletCertificateType.EXEMPTION,
        WalletCertificateType.DCC_LIGHT,
        WalletCertificateType.MULTI_PASS -> null
    }

val WalletCertificateType.certificateThumbnailDrawable: Int?
    get() = when (this) {
        WalletCertificateType.SANITARY -> R.drawable.test_certificate
        WalletCertificateType.VACCINATION -> R.drawable.vaccin_certificate
        WalletCertificateType.SANITARY_EUROPE -> R.drawable.test_europe_certificate
        WalletCertificateType.VACCINATION_EUROPE -> R.drawable.vaccin_europe_certificate
        WalletCertificateType.RECOVERY_EUROPE -> R.drawable.recovery_europe_certificate
        WalletCertificateType.EXEMPTION,
        WalletCertificateType.DCC_LIGHT,
        WalletCertificateType.MULTI_PASS -> null
    }

val WalletCertificateType.certificateDrawable: Int?
    get() = when (this) {
        WalletCertificateType.SANITARY -> R.drawable.test_certificate_full
        WalletCertificateType.VACCINATION -> R.drawable.vaccin_certificate_full
        WalletCertificateType.SANITARY_EUROPE -> R.drawable.test_europe_certificate_full
        WalletCertificateType.VACCINATION_EUROPE -> R.drawable.vaccin_europe_certificate_full
        WalletCertificateType.RECOVERY_EUROPE -> R.drawable.recovery_europe_certificate_full
        WalletCertificateType.EXEMPTION,
        WalletCertificateType.DCC_LIGHT,
        WalletCertificateType.MULTI_PASS -> null
    }

val WalletCertificateType.barcodeFormat: BarcodeFormat
    get() = when (this.format) {
        WalletCertificateType.Format.WALLET_2D -> BarcodeFormat.DATA_MATRIX
        WalletCertificateType.Format.WALLET_DCC -> BarcodeFormat.QR_CODE
    }
