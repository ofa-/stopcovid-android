/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/03/09 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lunabeestudio.framework.local.model.ActivityPassRoom

@Dao
interface ActivityPassRoomDao {

    @Query(
        """
        SELECT * FROM activitypassroom 
        WHERE root_uid = :id AND expire_at = (SELECT MIN(expire_at) FROM activitypassroom WHERE expire_at > :timestamp AND root_uid = :id)
        """
    )
    fun getForRootIdAndTime(id: String, timestamp: Long): ActivityPassRoom?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg certificates: ActivityPassRoom)

    @Query("DELETE FROM activitypassroom")
    fun deleteAll()

    @Query("DELETE FROM activitypassroom WHERE root_uid = :id")
    fun deleteAllForRootId(id: String)

    @Query("DELETE FROM activitypassroom WHERE expire_at < :timestamp")
    fun deleteExpired(timestamp: Long)

    @Query("SELECT COUNT(uid) FROM activitypassroom WHERE root_uid = :id AND expire_at > :timestamp")
    fun countForRootIdAndNotExpired(id: String, timestamp: Long): Int

    @Query("SELECT * FROM activitypassroom GROUP BY root_uid")
    fun getAllDistinctByRootId(): List<ActivityPassRoom>

    @Query("SELECT * FROM activitypassroom WHERE root_uid = :rootCertificateId")
    fun getAllActivityPassForRootId(rootCertificateId: String): List<ActivityPassRoom>

    @Query("DELETE FROM activitypassroom WHERE uid IN (:id)")
    fun deleteActivityPass(vararg id: String)
}