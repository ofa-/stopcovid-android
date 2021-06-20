package com.lunabeestudio.framework.extension

fun String.removePublicKeyDecoration(): String {
    return this
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
}

private val replaceCharacters = arrayOf(
    arrayOf("+", "-"),
    arrayOf("/", "_")
)

fun String.fromBase64URL(): String {
    var result = this
    replaceCharacters.forEach {
        result = result.replace(it[1], it[0])
    }
    return result
}
