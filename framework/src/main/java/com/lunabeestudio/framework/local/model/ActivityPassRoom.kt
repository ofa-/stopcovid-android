/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/03/09 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = CertificateRoom::class,
            parentColumns = arrayOf("uid"),
            childColumns = arrayOf("root_uid"),
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class ActivityPassRoom(
    @PrimaryKey val uid: String,
    @ColumnInfo(name = "encrypted_value") val encryptedValue: String,
    @ColumnInfo(name = "expire_at") val expireAt: Date,
    @ColumnInfo(name = "root_uid", index = true) val rootUid: String,
)