package com.lunabeestudio.framework.testutils

fun ByteArray.toHexString() = "size($size) " + joinToString(" ") { "0x%02X".format(it) }
