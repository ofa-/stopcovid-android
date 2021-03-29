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
    private const val BASE_URL: String = "https://app.tousanticovid.gouv.fr/"
    private const val VERSION_PATH: String = "json/version-29/"
    private const val VERSIONED_SERVER_URL: String = BASE_URL + VERSION_PATH
    const val SERVER_CERTIFICATE_SHA256: String = "sha256/ckVocY6+T4RvpXWtbqOF45qEvNls4oFWi83BryOQgOk="

    object Maintenance {
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
        const val MASTER_URL: String = BASE_URL + "infos/key-figures.json"
        const val MASTER_LOCAL_FILENAME: String = "key-figures.json"
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
        const val URL: String = VERSIONED_SERVER_URL
        const val FILE_PREFIX: String = "privacy-"
        const val ASSET_FOLDER_NAME: String = "Privacy/"
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

    object Store {
        const val GOOGLE: String = "market://details?id=fr.gouv.android.stopcovid"
        const val HUAWEI: String = "appmarket://details?id=fr.gouv.android.stopcovid"
        const val WEBSITE: String = "https://bonjour.tousanticovid.gouv.fr"
    }
}