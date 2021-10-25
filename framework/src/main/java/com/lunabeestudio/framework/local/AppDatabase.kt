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
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lunabeestudio.framework.local.dao.ActivityPassRoomDao
import com.lunabeestudio.framework.local.dao.AttestationRoomDao
import com.lunabeestudio.framework.local.dao.CertificateRoomDao
import com.lunabeestudio.framework.local.dao.VenueRoomDao
import com.lunabeestudio.framework.local.model.ActivityPassRoom
import com.lunabeestudio.framework.local.model.AttestationRoom
import com.lunabeestudio.framework.local.model.CertificateRoom
import com.lunabeestudio.framework.local.model.VenueRoom
import java.util.Date

@TypeConverters(Converters::class)
@Database(
    version = 2,
    entities = [AttestationRoom::class, CertificateRoom::class, VenueRoom::class, ActivityPassRoom::class],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attestationRoomDao(): AttestationRoomDao
    abstract fun certificateRoomDao(): CertificateRoomDao
    abstract fun activityPassRoomDao(): ActivityPassRoomDao
    abstract fun venueRoomDao(): VenueRoomDao

    companion object {
        fun build(context: Context, name: String): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE `ActivityPassRoom` (`uid` TEXT NOT NULL, `encrypted_value` TEXT NOT NULL, `expire_at` INTEGER NOT NULL, 
            `root_uid` TEXT NOT NULL, PRIMARY KEY(`uid`), FOREIGN KEY(`root_uid`) REFERENCES `CertificateRoom`(`uid`)
            ON UPDATE NO ACTION ON DELETE CASCADE )
            """
        )
        database.execSQL("CREATE INDEX `index_ActivityPassRoom_root_uid` ON `ActivityPassRoom` (`root_uid`)")
    }
}