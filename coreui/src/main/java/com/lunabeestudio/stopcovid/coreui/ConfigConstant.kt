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

object ConfigConstant {
    private const val BASE_URL: String = "https://app-static.tousanticovid.gouv.fr/"
    private const val VERSION_PATH: String = "json/version-35/"
    private const val VERSIONED_SERVER_URL: String = BASE_URL + VERSION_PATH

    object Maintenance {
        private const val BASE_URL: String = "https://app.tousanticovid.gouv.fr/"
        private const val FOLDER: String = "maintenance/"
        private const val FILENAME: String = "info-maintenance-v2.json"
        const val URL: String = BASE_URL + FOLDER + FILENAME
    }

    object Vaccination {
        const val ASSET_ZIP_GEOLOC_FILE_PATH: String = "VaccinationCenter/zip-geoloc.json"
        const val CENTER_FILENAME: String = "centres-vaccination.json"
        const val CENTER_LAST_UPDATE_FILENAME: String = "lastUpdate.json"
        private const val FOLDER: String = "/infos/dep/"
        const val URL: String = BASE_URL + FOLDER
        const val CENTER_SUFFIX: String = "-centers.json"
        const val LAST_UPDATE_SUFFIX: String = "-lastUpdate.json"
    }

    object KeyFigures {
        const val NATIONAL_SUFFIX: String = "nat"
        const val LOCAL_FILENAME_TEMPLATE: String = "key-figures-%s.pb.gz"
        const val URL: String = BASE_URL + "infos/v2%s/$LOCAL_FILENAME_TEMPLATE"
    }

    object MoreKeyFigures {
        const val FOLDER: String = "MoreKeyFigures/"
        const val URL: String = VERSIONED_SERVER_URL + FOLDER
        const val FILE_PREFIX: String = "morekeyfigures-"
    }

    object Attestations {
        const val FILENAME: String = "form.json"
        private const val FOLDER: String = "Attestations/"
        const val URL: String = VERSIONED_SERVER_URL + FOLDER + FILENAME
        const val ASSET_FILE_PATH: String = FOLDER + FILENAME
    }

    object InfoCenter {
        private const val FOLDER: String = "InfoCenter/"
        const val URL: String = VERSIONED_SERVER_URL + FOLDER
        const val LOCAL_FALLBACK_FILENAME: String = "info-labels-en.json"
        const val STRINGS_PREFIX: String = "info-labels-"
        const val TAGS_PREFIX: String = "info-tags"
        const val INFOS_PREFIX: String = "info-center"
        const val LAST_UPDATE_PREFIX: String = "info-center-lastupdate"
    }

    object Links {
        const val FOLDER: String = "Links/"
        const val URL: String = VERSIONED_SERVER_URL + FOLDER
        const val FILE_PREFIX: String = "links-"
    }

    object Privacy {
        const val FOLDER: String = "Privacy/"
        const val URL: String = VERSIONED_SERVER_URL + FOLDER
        const val FILE_PREFIX: String = "privacy-"
    }

    object Risks {
        const val FILENAME: String = "risks.json"
        private const val FOLDER: String = "Risks/"
        const val URL: String = VERSIONED_SERVER_URL + FOLDER + FILENAME
        const val ASSET_FILE_PATH: String = FOLDER + FILENAME
    }

    object Labels {
        const val FOLDER: String = "Strings/"
        const val URL: String = VERSIONED_SERVER_URL + FOLDER
        const val FILE_PREFIX: String = "strings-"
    }

    object Wallet {
        private const val FOLDER: String = "Wallet/"
        private const val URL: String = VERSIONED_SERVER_URL + FOLDER

        const val TEST_CERTIFICATE_THUMBNAIL_FILE: String = "test-certificate.png"
        private const val TEST_CERTIFICATE_THUMBNAIL_TEMPLATE_FILE: String = "test-certificate-%s.png"
        const val TEST_CERTIFICATE_THUMBNAIL_TEMPLATE_URL: String = URL + TEST_CERTIFICATE_THUMBNAIL_TEMPLATE_FILE

        const val TEST_CERTIFICATE_FULL_FILE: String = "test-certificate-full.png"
        private const val TEST_CERTIFICATE_FULL_TEMPLATE_FILE: String = "test-certificate-full-%s.png"
        const val TEST_CERTIFICATE_FULL_TEMPLATE_URL: String = URL + TEST_CERTIFICATE_FULL_TEMPLATE_FILE

        const val VACCIN_CERTIFICATE_THUMBNAIL_FILE: String = "vaccin-certificate.png"
        private const val VACCIN_CERTIFICATE_THUMBNAIL_TEMPLATE_FILE: String = "vaccin-certificate-%s.png"
        const val VACCIN_CERTIFICATE_THUMBNAIL_TEMPLATE_URL: String = URL + VACCIN_CERTIFICATE_THUMBNAIL_TEMPLATE_FILE

        const val VACCIN_CERTIFICATE_FULL_FILE: String = "vaccin-certificate-full.png"
        private const val VACCIN_CERTIFICATE_FULL_TEMPLATE_FILE: String = "vaccin-certificate-full-%s.png"
        const val VACCIN_CERTIFICATE_FULL_TEMPLATE_URL: String = URL + VACCIN_CERTIFICATE_FULL_TEMPLATE_FILE

        const val VACCIN_EUROPE_CERTIFICATE_FULL_FILE: String = "vaccin-europe-certificate-full.png"
        private const val VACCIN_EUROPE_CERTIFICATE_FULL_TEMPLATE_FILE: String = "vaccin-europe-certificate-full-%s.png"
        const val VACCIN_EUROPE_CERTIFICATE_FULL_TEMPLATE_URL: String = URL + VACCIN_EUROPE_CERTIFICATE_FULL_TEMPLATE_FILE

        const val VACCIN_EUROPE_CERTIFICATE_THUMBNAIL_FILE: String = "vaccin-europe-certificate.png"
        private const val VACCIN_EUROPE_CERTIFICATE_THUMBNAIL_TEMPLATE_FILE: String = "vaccin-europe-certificate-%s.png"
        const val VACCIN_EUROPE_CERTIFICATE_THUMBNAIL_TEMPLATE_URL: String = URL + VACCIN_EUROPE_CERTIFICATE_THUMBNAIL_TEMPLATE_FILE

        const val TEST_EUROPE_CERTIFICATE_THUMBNAIL_FILE: String = "test-europe-certificate.png"
        private const val TEST_EUROPE_CERTIFICATE_THUMBNAIL_TEMPLATE_FILE: String = "test-europe-certificate-%s.png"
        const val TEST_EUROPE_CERTIFICATE_THUMBNAIL_TEMPLATE_URL: String = URL + TEST_EUROPE_CERTIFICATE_THUMBNAIL_TEMPLATE_FILE

        const val TEST_EUROPE_CERTIFICATE_FULL_FILE: String = "test-europe-certificate-full.png"
        private const val TEST_EUROPE_CERTIFICATE_FULL_TEMPLATE_FILE: String = "test-europe-certificate-full-%s.png"
        const val TEST_EUROPE_CERTIFICATE_FULL_TEMPLATE_URL: String = URL + TEST_EUROPE_CERTIFICATE_FULL_TEMPLATE_FILE

        const val RECOVERY_EUROPE_CERTIFICATE_THUMBNAIL_FILE: String = "recovery-europe-certificate.png"
        private const val RECOVERY_EUROPE_CERTIFICATE_THUMBNAIL_TEMPLATE_FILE: String = "recovery-europe-certificate-%s.png"
        const val RECOVERY_EUROPE_CERTIFICATE_THUMBNAIL_TEMPLATE_URL: String = URL + RECOVERY_EUROPE_CERTIFICATE_THUMBNAIL_TEMPLATE_FILE

        const val RECOVERY_EUROPE_CERTIFICATE_FULL_FILE: String = "recovery-europe-certificate-full.png"
        private const val RECOVERY_EUROPE_CERTIFICATE_FULL_TEMPLATE_FILE: String = "recovery-europe-certificate-full-%s.png"
        const val RECOVERY_EUROPE_CERTIFICATE_FULL_TEMPLATE_URL: String = URL + RECOVERY_EUROPE_CERTIFICATE_FULL_TEMPLATE_FILE
    }

    object DccCertificates {
        const val FOLDER: String = "Certs/"
        const val URL: String = VERSIONED_SERVER_URL + FOLDER
    }

    object Config {
        const val FOLDER: String = "Config/"
        const val URL: String = VERSIONED_SERVER_URL + FOLDER
        const val LOCAL_FILENAME: String = "config.json"
    }

    object Calibration {
        const val FOLDER: String = "Calibration/"
        const val URL: String = VERSIONED_SERVER_URL + FOLDER
        const val LOCAL_FILENAME: String = "calibrationBle.json"
    }

    object BlacklistDCC {
        const val FILENAME: String = "certlist.pb.gz"
        private const val ASSET_FILENAME: String = "certlist.pb_gz"
        const val FOLDER: String = "CertList/"
        const val URL: String = VERSIONED_SERVER_URL + FOLDER + FILENAME
        val ASSET_FILE_PATH: String = FOLDER + ASSET_FILENAME
    }

    object Blacklist2DDOC {
        const val FILENAME: String = "2ddoc_list.pb.gz"
        private const val ASSET_FILENAME: String = "2ddoc_list.pb_gz"
        const val FOLDER: String = "CertList/"
        const val URL: String = VERSIONED_SERVER_URL + FOLDER + FILENAME
        val ASSET_FILE_PATH: String = FOLDER + ASSET_FILENAME
    }

    object Store {
        const val GOOGLE: String = "market://details?id=fr.gouv.android.stopcovid"
        const val HUAWEI: String = "appmarket://details?id=fr.gouv.android.stopcovid"
        const val TAC_WEBSITE: String = "https://bonjour.tousanticovid.gouv.fr"
        const val STOPCOVID_WEBSITE: String = "https://bonjour.stopcovid.gouv.fr"
    }
}
