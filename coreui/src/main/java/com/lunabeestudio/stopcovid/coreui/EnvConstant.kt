/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/15/07 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui

enum class EnvConstant {
    Prod {
        override val captchaApiKey: String = "6LettPsUAAAAAHYaFdRBOilHUgmTMSIPKNZN4D7l"
        override val baseUrl: String = "https://api.tousanticovid.gouv.fr"
        override val warningBaseUrl: String = "https://tacw.tousanticovid.gouv.fr"
        override val analyticsBaseUrl: String = "https://analytics-api.tousanticovid.gouv.fr"
        override val certificateSha256: String = "sha256/xrPKKhmYeHgk4v57GcqYPrFpnI3f1FTmEfol9WIicaI="
        override val warningCertificateSha256: String = "sha256/b7w+uqyD+XILNIlRc3XVmEROwFCVTv5yOchb2i5FJbo="
        override val analyticsCertificateSha256: String = "sha256/KhFx3fIev58nbXs9m2WqXDbqYrE/7r4J9cP1QqPHtVk="
        override val configFilename: String = "config.json"
        override val calibrationFilename: String = "calibrationBle.json"
        override val serverPublicKey: String = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEAc9IDt6qJq453SwyWPB94JaLB2VfTAcL43YVtMr3HhDCd22gKaQXIbX1d+tNhfvaKM51sxeaXziPjntUzbTNiw=="
    };

    abstract val captchaApiKey: String
    abstract val baseUrl: String
    abstract val warningBaseUrl: String
    abstract val analyticsBaseUrl: String
    abstract val certificateSha256: String
    abstract val warningCertificateSha256: String
    abstract val analyticsCertificateSha256: String
    abstract val configFilename: String
    abstract val calibrationFilename: String
    abstract val serverPublicKey: String
}