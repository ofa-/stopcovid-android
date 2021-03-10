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
        override val baseUrl: String = "https://api.stopcovid.gouv.fr"
        override val warningBaseUrl: String = "https://tacw.tousanticovid.gouv.fr"
        override val certificateSha256: String = "sha256/Up+TDyVDu8vKvd22TeAnXYxQqfPd2oNOU9Y04JahHpQ="
        override val warningCertificateSha256: String = "sha256/b7w+uqyD+XILNIlRc3XVmEROwFCVTv5yOchb2i5FJbo="
        override val configFilename: String = "config.json"
        override val calibrationFilename: String = "calibrationBle.json"
        override val serverPublicKey: String = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEAc9IDt6qJq453SwyWPB94JaLB2VfTAcL43YVtMr3HhDCd22gKaQXIbX1d+tNhfvaKM51sxeaXziPjntUzbTNiw=="
    };

    abstract val captchaApiKey: String
    abstract val baseUrl: String
    abstract val warningBaseUrl: String
    abstract val certificateSha256: String
    abstract val warningCertificateSha256: String
    abstract val configFilename: String
    abstract val calibrationFilename: String
    abstract val serverPublicKey: String
}