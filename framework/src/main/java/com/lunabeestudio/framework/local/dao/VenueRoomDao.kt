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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lunabeestudio.framework.local.model.VenueRoom
import kotlinx.coroutines.flow.Flow

@Dao
interface VenueRoomDao {

    @Query("SELECT * FROM venueroom")
    fun getAllFlow(): Flow<List<VenueRoom>>

    @Query("SELECT * FROM venueroom")
    fun getAll(): List<VenueRoom>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg venues: VenueRoom)

    @Query("DELETE FROM venueroom WHERE uid = :venueId")
    fun delete(venueId: String)

    @Delete
    fun delete(vararg venueRoom: VenueRoom)

    @Query("DELETE FROM venueroom")
    fun deleteAll()

    @Query("UPDATE venueroom SET uid = :uid WHERE uid = (SELECT uid FROM venueroom LIMIT 1)")
    fun updateFirstVenueUid(uid: String)
}