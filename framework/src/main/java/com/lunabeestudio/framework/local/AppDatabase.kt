/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/03/09 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lunabeestudio.framework.local.dao.AttestationRoomDao
import com.lunabeestudio.framework.local.dao.CertificateRoomDao
import com.lunabeestudio.framework.local.dao.VenueRoomDao
import com.lunabeestudio.framework.local.model.AttestationRoom
import com.lunabeestudio.framework.local.model.CertificateRoom
import com.lunabeestudio.framework.local.model.VenueRoom

@Database(entities = [AttestationRoom::class, CertificateRoom::class, VenueRoom::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attestationRoomDao(): AttestationRoomDao
    abstract fun certificateRoomDao(): CertificateRoomDao
    abstract fun venueRoomDao(): VenueRoomDao
}