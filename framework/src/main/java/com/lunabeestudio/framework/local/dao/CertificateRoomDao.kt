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
import com.lunabeestudio.framework.local.model.CertificateRoom
import kotlinx.coroutines.flow.Flow

@Dao
interface CertificateRoomDao {

    @Query("SELECT * FROM certificateroom")
    fun getAllFlow(): Flow<List<CertificateRoom>>

    @Query("SELECT * FROM certificateroom")
    fun getAll(): List<CertificateRoom>

    @Query("SELECT * FROM certificateroom WHERE uid = :id")
    fun getById(id: String): Flow<CertificateRoom?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg certificates: CertificateRoom)

    @Query("DELETE FROM certificateroom WHERE uid = :certificateId")
    fun delete(certificateId: String)

    @Query("DELETE FROM certificateroom")
    fun deleteAll()

    @Query("SELECT COUNT(uid) FROM certificateroom")
    fun getAllCount(): Int

    @Query("SELECT COUNT(uid) FROM certificateroom")
    fun getAllCountFlow(): Flow<Int>
}