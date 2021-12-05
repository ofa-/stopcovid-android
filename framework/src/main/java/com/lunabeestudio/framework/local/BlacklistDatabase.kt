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

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lunabeestudio.framework.local.dao.EuropeanCertificateBlacklistRoomDao
import com.lunabeestudio.framework.local.dao.FrenchCertificateBlacklistRoomDao
import com.lunabeestudio.framework.local.model.EuropeanCertificateBlacklistRoom
import com.lunabeestudio.framework.local.model.FrenchCertificateBlacklistRoom

@Database(
    version = 1,
    entities = [
        EuropeanCertificateBlacklistRoom::class,
        FrenchCertificateBlacklistRoom::class,
    ],
)
abstract class BlacklistDatabase : RoomDatabase() {
    abstract fun europeanCertificateBlacklistRoomDao(): EuropeanCertificateBlacklistRoomDao
    abstract fun frenchCertificateBlacklistRoomDao(): FrenchCertificateBlacklistRoomDao

    companion object {
        private const val BLACKLIST_DB_NAME = "blacklist.db"

        fun build(context: Context): BlacklistDatabase =
            Room.databaseBuilder(context, BlacklistDatabase::class.java, BLACKLIST_DB_NAME)
                .build()
    }
}