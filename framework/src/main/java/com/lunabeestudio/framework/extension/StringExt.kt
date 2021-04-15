package com.lunabeestudio.framework.extension

fun String.removePublicKeyDecoration(): String {
    return this
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")

}