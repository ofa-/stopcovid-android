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
import com.lunabeestudio.framework.local.model.FrenchCertificateBlacklistRoom

@Dao
interface FrenchCertificateBlacklistRoomDao : CertificateBlacklistRoomDao<FrenchCertificateBlacklistRoom> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override fun insertAll(vararg certificate: FrenchCertificateBlacklistRoom)

    @Delete
    override fun deleteAll(vararg certificate: FrenchCertificateBlacklistRoom)

    @Query("SELECT * FROM frenchcertificateblacklistroom WHERE hash = :certificateHash")
    override fun getByHash(certificateHash: String): FrenchCertificateBlacklistRoom?
}
