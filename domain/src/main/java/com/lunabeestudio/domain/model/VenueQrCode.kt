package com.lunabeestudio.domain.model

data class VenueQrCode(
    val id: String,
    val uuid: String,
    val qrType: VenueQrType,
    val venueType: String,
    val ntpTimestamp: Long,
    val venueCategory: Int?,
    val venueCapacity: Int?,
    val payload: String
)

enum class VenueQrType(val value: Int) {
    STATIC(0), DYNAMIC(1);

    companion object {
        fun fromValue(value: Int): VenueQrType? {
            return when (value) {
                0 -> STATIC
                1 -> DYNAMIC
                else -> null
            }
        }
    }
}