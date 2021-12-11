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
import com.lunabeestudio.framework.local.model.AttestationRoom
import kotlinx.coroutines.flow.Flow

@Dao
interface AttestationRoomDao {

    @Query("SELECT * FROM attestationroom")
    fun getAllFlow(): Flow<List<AttestationRoom>>

    @Query("SELECT * FROM attestationroom")
    fun getAll(): List<AttestationRoom>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg attestations: AttestationRoom)

    @Query("DELETE FROM attestationroom WHERE uid = :attestationId")
    fun delete(attestationId: String)

    @Query("DELETE FROM attestationroom")
    fun deleteAll()

    @Delete
    fun delete(vararg attestationRoom: AttestationRoom)

    @Query("UPDATE attestationroom SET uid = :uid WHERE uid = (SELECT uid FROM attestationroom LIMIT 1)")
    fun updateFirstAttestationUid(uid: String)
}