/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.domain.model

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

data class EphemeralBluetoothIdentifier(
    val epochId: Long,
    val ntpStartTimeS: Long,
    val ntpEndTimeS: Long,
    val ecc: ByteArray,
    val ebid: ByteArray
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EphemeralBluetoothIdentifier

        if (ntpStartTimeS != other.ntpStartTimeS) return false
        if (ntpEndTimeS != other.ntpEndTimeS) return false
        if (!ecc.contentEquals(other.ecc)) return false
        if (!ebid.contentEquals(other.ebid)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ntpStartTimeS.hashCode()
        result = 31 * result + ntpEndTimeS.hashCode()
        result = 31 * result + ecc.contentHashCode()
        result = 31 * result + ebid.contentHashCode()
        return result
    }

    companion object {
        fun createFromTuples(
            ntpInitialTimeS: Long,
            epochDurationS: Int,
            ebidTuplesJson: String
        ): List<EphemeralBluetoothIdentifier> {

            val ebids = mutableListOf<EphemeralBluetoothIdentifier>()
            val jsonArray = JSONArray(ebidTuplesJson)
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray[i] as JSONObject

                val epochId = (json["epochId"] as Int).toLong()
                val key = (json["key"] as JSONObject)
                val ebid = key["ebid"] as String
                val ecc = key["ecc"] as String

                ebids += EphemeralBluetoothIdentifier(epochId = epochId,
                    ntpStartTimeS = ntpInitialTimeS + epochId * epochDurationS,
                    ntpEndTimeS = ntpInitialTimeS + (epochId + 1) * epochDurationS,
                    ecc = Base64.decode(ecc, Base64.NO_WRAP),
                    ebid = Base64.decode(ebid, Base64.NO_WRAP))
            }

            return ebids
        }
    }
}