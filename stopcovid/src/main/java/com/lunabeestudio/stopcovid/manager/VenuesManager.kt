package com.lunabeestudio.stopcovid.manager

import android.content.SharedPreferences
import android.net.UrlQuerySanitizer
import android.util.Base64
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeMs
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.framework.extension.fromBase64URL
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.extension.isValidUUID
import com.lunabeestudio.stopcovid.extension.isVenueOnBoardingDone
import com.lunabeestudio.stopcovid.extension.venuesFeaturedWasActivatedAtLeastOneTime
import com.lunabeestudio.stopcovid.model.VenueExpiredException
import com.lunabeestudio.stopcovid.model.VenueInvalidFormatException
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.Arrays.copyOfRange
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.days

object VenuesManager {

    fun processVenueUrl(
        robertManager: RobertManager,
        secureKeystoreDataSource: SecureKeystoreDataSource,
        stringUrl: String,
    ) {
        try {
            val sanitizer = UrlQuerySanitizer()
            sanitizer.registerParameters(arrayOf("code", "v", "t")) {
                it // Do nothing since there are plenty of non legal characters in this value
            }
            sanitizer.parseUrl(DeeplinkManager.transformAnchorParam(stringUrl))

            val base64URLCode: String = sanitizer.getValue("code") ?: throw VenueInvalidFormatException()
            val version: Int = sanitizer.getValue("v")?.toInt() ?: throw VenueInvalidFormatException()
            val time: Long? = sanitizer.getValue("t")?.toLong() // Time is optional, if null, will be set to System.currentTime

            processVenue(robertManager, secureKeystoreDataSource, base64URLCode, version, time)

        } catch (e: Exception) {
            Timber.e(e)
            throw e
        }
    }

    @OptIn(ExperimentalTime::class)
    fun processVenue(
        robertManager: RobertManager,
        secureKeystoreDataSource: SecureKeystoreDataSource,
        base64URLCode: String,
        version: Int,
        unixTimeInSeconds: Long?
    ) {
        try {

            val uuid = extractTlIdFromBase64(base64URLCode.fromBase64URL())
            if (!uuid.isValidUUID()) {
                throw VenueInvalidFormatException()
            }

            val timestamp = unixTimeInSeconds?.times(1000L) ?: System.currentTimeMillis()

            if (isExpired(robertManager, timestamp)) {
                throw VenueExpiredException()
            }

            val ntpTimeStamp: Long = timestamp.unixTimeMsToNtpTimeS()

            val id = "$uuid$ntpTimeStamp"

            val venueQrCode = VenueQrCode(
                id = id,
                ltid = uuid,
                ntpTimestamp = ntpTimeStamp,
                base64URL = base64URLCode,
                version = version
            )
            saveVenue(secureKeystoreDataSource, venueQrCode)
        } catch (e: Exception) {
            Timber.e(e, "Fail to process venue with code: $base64URLCode")
            throw e
        }
    }

    private fun extractTlIdFromBase64(base64String: String): String {
        val bs = copyOfRange(Base64.decode(base64String, Base64.NO_WRAP), 1, 17)
        return extractTlIdFromBase64ByteArray(bs).toString()
    }

    private fun extractTlIdFromBase64ByteArray(bytes: ByteArray): UUID {
        val byteBuffer = ByteBuffer.wrap(bytes)
        val high = byteBuffer.long
        val low = byteBuffer.long
        return UUID(high, low)
    }

    @OptIn(ExperimentalTime::class)
    fun isExpired(robertManager: RobertManager,
        unixTimeInMS: Long): Boolean = unixTimeInMS + gracePeriod(robertManager) <= System.currentTimeMillis()

    private fun saveVenue(keystoreDataSource: SecureKeystoreDataSource, venueQrCode: VenueQrCode) {
        val venuesQrCode = keystoreDataSource.venuesQrCode?.toMutableList() ?: mutableListOf()
        venueQrCode.takeIf {
            venuesQrCode.none {
                venueQrCode.id == it.id
            }
        }?.let {
            venuesQrCode.add(it)
            venueListHasChanged(keystoreDataSource)
            keystoreDataSource.venuesQrCode = venuesQrCode
        }
    }

    fun getVenuesQrCode(
        keystoreDataSource: SecureKeystoreDataSource,
        startNtpTimestamp: Long? = null,
        endNtpTimestamp: Long? = null,
    ): List<VenueQrCode>? = (startNtpTimestamp ?: Long.MIN_VALUE).let { forcedStartNtpTimestamp ->
        (endNtpTimestamp ?: Long.MAX_VALUE).let { forcedEndNtpTimestamp ->
            keystoreDataSource.venuesQrCode?.filter {
                it.ntpTimestamp in forcedStartNtpTimestamp..forcedEndNtpTimestamp
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun clearExpired(
        robertManager: RobertManager,
        keystoreDataSource: SecureKeystoreDataSource,
    ) {
        if (!keystoreDataSource.venuesQrCode?.filter {
                isExpired(robertManager, it.ntpTimestamp.ntpTimeSToUnixTimeMs()) || it.ltid == null // This test is added to handle "old" venues that may have null here due to JSON parsing handling
            }.isNullOrEmpty()) {
            keystoreDataSource.venuesQrCode = keystoreDataSource.venuesQrCode?.filter { venueQrCode ->
                !isExpired(robertManager, venueQrCode.ntpTimestamp.ntpTimeSToUnixTimeMs()) && venueQrCode.ltid != null // This test is added to handle "old" venues that may have null here due to JSON parsing handling
            }
            venueListHasChanged(keystoreDataSource)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun gracePeriod(robertManager: RobertManager) = robertManager.configuration.venuesRetentionPeriod.days.toLongMilliseconds()

    fun removeVenue(keystoreDataSource: SecureKeystoreDataSource, venueId: String) {
        val venuesQrCode = keystoreDataSource.venuesQrCode?.toMutableList() ?: mutableListOf()
        venuesQrCode.firstOrNull { it.id == venueId }?.let {
            venuesQrCode.remove(it)
            venueListHasChanged(keystoreDataSource)
            keystoreDataSource.venuesQrCode = venuesQrCode
        }
    }

    fun clearAllData(preferences: SharedPreferences, keystoreDataSource: SecureKeystoreDataSource) {
        keystoreDataSource.venuesQrCode = null
        preferences.isVenueOnBoardingDone = false
        preferences.venuesFeaturedWasActivatedAtLeastOneTime = false
    }

    private fun venueListHasChanged(keystoreDataSource: SecureKeystoreDataSource) {
        // We reset Clea scoring iteration because we changed the venues
        keystoreDataSource.cleaLastStatusIteration = null
    }
}