/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/15/04 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.analytics.extension

import com.lunabeestudio.analytics.model.TimestampedEvent
import com.lunabeestudio.analytics.network.model.TimestampedEventRQ
import com.lunabeestudio.analytics.proto.ProtoStorage

fun TimestampedEvent.toProto(): ProtoStorage.TimestampedEventProto {
    val builder = ProtoStorage.TimestampedEventProto.newBuilder().apply {
        name = this@toProto.name
        timestamp = this@toProto.timestamp
        desc = this@toProto.desc
    }
    return builder.build()
}

fun List<TimestampedEvent>.toProto(): ProtoStorage.TimestampedEventProtoList {
    val builder = ProtoStorage.TimestampedEventProtoList.newBuilder().apply {
        this.addAllTimestampedEventProtoList(map {
            it.toProto()
        })
    }
    return builder.build()
}

private fun TimestampedEvent.toAPI(): TimestampedEventRQ {
    return TimestampedEventRQ(
        name = name,
        timestamp = timestamp,
        desc = desc.takeIf { it.isNotBlank() }
    )
}

fun List<TimestampedEvent>.toAPI(): List<TimestampedEventRQ> {
    return this.map { timestampedEvent ->
        timestampedEvent.toAPI()
    }
}

private fun ProtoStorage.TimestampedEventProto.toDomain(): TimestampedEvent =
    TimestampedEvent(name, timestamp, desc)

fun ProtoStorage.TimestampedEventProtoList.toDomain(): List<TimestampedEvent> =
    timestampedEventProtoListList.map {
        it.toDomain()
    }