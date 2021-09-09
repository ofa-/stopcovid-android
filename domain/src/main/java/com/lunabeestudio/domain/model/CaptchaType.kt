/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/02/09 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.domain.model

enum class CaptchaType {
    AUDIO {
        override val header: String = "audio/*"
        override val path: String = "audio"
    },
    IMAGE {
        override val header: String = "image/png"
        override val path: String = "image"
    };

    abstract val header: String
    abstract val path: String
}