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
        override val cleaStatusBaseUrl: String = "https://s3.fr-par.scw.cloud/clea-batch/"
        override val cleaReportBaseUrl: String = "https://signal-api.tousanticovid.gouv.fr/"
        override val analyticsBaseUrl: String = "https://analytics-api.tousanticovid.gouv.fr"
        override val configFilename: String = "config.json"
        override val calibrationFilename: String = "calibrationBle.json"
        override val serverPublicKey: String =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEAc9IDt6qJq453SwyWPB94JaLB2VfTAcL43YVtMr3HhDCd22gKaQXIbX1d+tNhfvaKM51sxeaXziPjntUzbTNiw=="
        override val dccCertificatesFilename: String = "dcc-certs.json"
        override val conversionBaseUrl: String = "https://portail.tacv.myservices-ingroupe.com"
    };

    abstract val captchaApiKey: String
    abstract val baseUrl: String
    abstract val cleaStatusBaseUrl: String
    abstract val cleaReportBaseUrl: String
    abstract val analyticsBaseUrl: String
    abstract val configFilename: String
    abstract val calibrationFilename: String
    abstract val serverPublicKey: String
    abstract val dccCertificatesFilename: String
    abstract val conversionBaseUrl: String
}
