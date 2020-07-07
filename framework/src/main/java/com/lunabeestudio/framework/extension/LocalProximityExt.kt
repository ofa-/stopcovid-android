/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/11/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.extension

import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.framework.proto.ProtoStorage

fun LocalProximity.toProto(): ProtoStorage.LocalProximityProto {
    val builder = ProtoStorage.LocalProximityProto.newBuilder().apply {
        eccBase64 = this@toProto.eccBase64
        ebidBase64 = this@toProto.ebidBase64
        macBase64 = this@toProto.macBase64
        helloTime = this@toProto.helloTime
        collectedTime = this@toProto.collectedTime
        rawRssi = this@toProto.rawRssi
        calibratedRssi = this@toProto.calibratedRssi
    }
    return builder.build()
}

fun List<LocalProximity>.toProto(): ProtoStorage.LocalProximityProtoList {
    val builder = ProtoStorage.LocalProximityProtoList.newBuilder().apply {
        this.addAllLocalProximityProtoList(map {
            it.toProto()
        })
    }
    return builder.build()
}

fun ProtoStorage.LocalProximityProto.toDomain(): LocalProximity =
    LocalProximity(eccBase64, ebidBase64, macBase64, helloTime, collectedTime, rawRssi, calibratedRssi)

fun ProtoStorage.LocalProximityProtoList.toDomain(): List<LocalProximity> =
    localProximityProtoListList.map {
        it.toDomain()
    }

fun localProximityFromString(it: String): LocalProximity {
    val data = it.split(" ")
    return LocalProximity(
        collectedTime = data[0].toLong(36),
        ebidBase64 = data[1],
        eccBase64 = data[2],
        macBase64 = data[3],
        helloTime = data[4].toInt(36),
        calibratedRssi = data[5].toInt(36),
        rawRssi = data[6].toInt(36)
    )
}

fun localProximityToString(it: LocalProximity): String {
    return listOf(
        it.collectedTime.toString(36),
        it.ebidBase64,
        it.eccBase64,
        it.macBase64,
        it.helloTime.toString(36),
        it.calibratedRssi.toString(36),
        it.rawRssi.toString(36)
    ).joinToString(" ")
}
