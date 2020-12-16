package com.lunabeestudio.stopcovid.manager

import android.content.SharedPreferences
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeMs
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.domain.model.VenueQrType
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.extension.isValidUUID
import com.lunabeestudio.stopcovid.extension.isVenueOnBoardingDone
import com.lunabeestudio.stopcovid.extension.privateEventQrCode
import com.lunabeestudio.stopcovid.extension.privateEventQrCodeGenerationDate
import com.lunabeestudio.stopcovid.extension.roundedTimeIntervalSince1900
import com.lunabeestudio.stopcovid.extension.sha256
import com.lunabeestudio.stopcovid.extension.venuesFeaturedWasActivatedAtLeastOneTime
import timber.log.Timber
import java.net.URL
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.days

object VenuesManager {

    fun processVenueUrl(
        robertManager: RobertManager,
        secureKeystoreDataSource: SecureKeystoreDataSource,
        stringUrl: String,
    ): String? {
        return try {
            val url = URL(stringUrl)
            val path: String = url.path.drop(1)
            if ("${url.protocol}://${url.host}/" == Constants.Url.VENUE_ROOT_URL) {
                processVenuePath(robertManager, secureKeystoreDataSource, path)
            } else {
                throw Exception("invalid rootUrl")
            }
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    fun processVenuePath(
        robertManager: RobertManager,
        secureKeystoreDataSource: SecureKeystoreDataSource,
        path: String,
    ): String? {
        try {
            val info: List<String> = path.trimEnd('/').split("/")

            if (info.count() < 3) throw Exception("not enough parameter")

            // Values
            val qrType = VenueQrType.fromValue(info[0].toInt())
            val uuid = info[1]
            val venueType = info[2].toUpperCase(Locale.getDefault())
            val venueCategory = if (info.count() >= 4) info[3].toInt() else 0
            val venueCapacity = if (info.count() >= 5) info[4].toInt() else 0

            // Conditions
            if (!(uuid.isValidUUID()
                    && qrType != null
                    && (1..3).contains(venueType.count())
                    && (0..5).contains(venueCategory)
                    && venueCapacity >= 0)
            ) throw Exception("invalid UUID, qrType, venueType, venueCategory or venueCapacity")

            val nowRoundedNtpTimestamp: Long = Date().roundedTimeIntervalSince1900(robertManager.venuesTimestampRoundingInterval.toLong())
            val id = "$uuid$nowRoundedNtpTimestamp"

            val salt: Int = Random.nextInt(1, 1000)
            val payload: String = "$salt$uuid".sha256()

            val venueQrCode = VenueQrCode(
                id,
                uuid,
                qrType,
                venueType,
                nowRoundedNtpTimestamp,
                venueCategory,
                venueCapacity,
                payload
            )
            saveVenue(secureKeystoreDataSource, venueQrCode)
            return venueType
        } catch (e: Exception) {
            Timber.e(e, "Fail to proces path $path")
            return null
        }
    }

    private fun saveVenue(keystoreDataSource: SecureKeystoreDataSource, venueQrCode: VenueQrCode) {
        val venuesQrCode = keystoreDataSource.venuesQrCode?.toMutableList() ?: mutableListOf()
        venueQrCode.takeIf {
            venuesQrCode.none {
                venueQrCode.id == it.id
            }
        }?.let(venuesQrCode::add)
        keystoreDataSource.venuesQrCode = venuesQrCode
    }

    fun getVenuesQrCode(
        keystoreDataSource: SecureKeystoreDataSource,
        startNtpTimestamp: Long? = null,
    ): List<VenueQrCode>? = startNtpTimestamp?.let {
        keystoreDataSource.venuesQrCode?.filter {
            it.ntpTimestamp >= startNtpTimestamp
        }
    } ?: keystoreDataSource.venuesQrCode

    @OptIn(ExperimentalTime::class)
    fun clearExpired(
        robertManager: RobertManager,
        keystoreDataSource: SecureKeystoreDataSource,
    ) {
        val gracePeriod = robertManager.venuesRetentionPeriod.days.toLongMilliseconds()
        keystoreDataSource.venuesQrCode = keystoreDataSource.venuesQrCode?.filter { venueQrCode ->
            val recordTimestamp = venueQrCode.ntpTimestamp.ntpTimeSToUnixTimeMs()
            recordTimestamp + gracePeriod >= System.currentTimeMillis()
        }
    }

    fun removeVenue(keystoreDataSource: SecureKeystoreDataSource, venueId: String) {
        val venuesQrCode = keystoreDataSource.venuesQrCode?.toMutableList() ?: mutableListOf()
        venuesQrCode.firstOrNull { it.id == venueId }?.let(venuesQrCode::remove)
        keystoreDataSource.venuesQrCode = venuesQrCode
    }

    fun clearAllData(preferences: SharedPreferences, keystoreDataSource: SecureKeystoreDataSource) {
        keystoreDataSource.venuesQrCode = null
        preferences.isVenueOnBoardingDone = false
        preferences.venuesFeaturedWasActivatedAtLeastOneTime = false
    }

    fun generateNewQRCodeIfNeeded(
        sharedPreferences: SharedPreferences,
        robertManager: RobertManager,
        keystoreDataSource: SecureKeystoreDataSource,
    ) {
        val nowCalendar = Calendar.getInstance()
        val lastGenerationCalendar = Calendar.getInstance().apply {
            timeInMillis = sharedPreferences.privateEventQrCodeGenerationDate
        }
        if (nowCalendar.get(Calendar.DAY_OF_YEAR) != lastGenerationCalendar.get(Calendar.DAY_OF_YEAR)
            || nowCalendar.get(Calendar.YEAR) != lastGenerationCalendar.get(Calendar.YEAR)) {
            val venueUrl = "${Constants.Url.VENUE_ROOT_URL}0/${UUID.randomUUID()}/${robertManager.privateEventVenueType}"
            sharedPreferences.privateEventQrCode = venueUrl
            processVenueUrl(robertManager, keystoreDataSource, venueUrl)
            sharedPreferences.privateEventQrCodeGenerationDate = System.currentTimeMillis()
        }
    }
}