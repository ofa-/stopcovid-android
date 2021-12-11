/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/22/03 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.repository

import android.content.SharedPreferences
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.util.Base64
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeMs
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.framework.extension.fromBase64URL
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.repository.RobertVenueRepository
import com.lunabeestudio.stopcovid.extension.isValidUUID
import com.lunabeestudio.stopcovid.extension.isVenueOnBoardingDone
import com.lunabeestudio.stopcovid.extension.venuesFeaturedWasActivatedAtLeastOneTime
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.extension.privateEventQrCode
import com.lunabeestudio.stopcovid.extension.privateEventQrCodeGenerationDate
import com.lunabeestudio.stopcovid.manager.DeeplinkManager
import com.lunabeestudio.stopcovid.model.VenueExpiredException
import com.lunabeestudio.stopcovid.model.VenueInvalidFormatException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.Arrays.copyOfRange
import java.util.Calendar
import java.util.UUID
import kotlin.time.Duration.Companion.days

class VenueRepository(
    private val localKeystoreDataSource: LocalKeystoreDataSource,
    private val sharedPreferences: SharedPreferences,
) : RobertVenueRepository {
    val venuesQrCodeFlow: Flow<List<VenueQrCode>>
        get() = localKeystoreDataSource.venuesQrCodeFlow

    suspend fun processVenueUrl(
        robertManager: RobertManager,
        stringUrl: String,
    ) {
        try {
            val sanitizer = UrlQuerySanitizer()
            sanitizer.registerParameters(arrayOf(DeeplinkManager.DEEPLINK_CODE_PARAMETER, "v", "t")) {
                it // Do nothing since there are plenty of non legal characters in this value
            }
            sanitizer.parseUrl(DeeplinkManager.transformFragmentToCodeParam(Uri.parse(stringUrl)).toString())

            val base64URLCode: String = sanitizer.getValue(DeeplinkManager.DEEPLINK_CODE_PARAMETER) ?: throw VenueInvalidFormatException()
            val version: Int = sanitizer.getValue("v")?.toInt() ?: throw VenueInvalidFormatException()
            val time: Long? = sanitizer.getValue("t")?.toLong() // Time is optional, if null, will be set to System.currentTime

            processVenue(robertManager, base64URLCode, version, time)
        } catch (e: Exception) {
            Timber.e(e)
            throw e
        }
    }

    suspend fun processVenue(
        robertManager: RobertManager,
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
            saveVenue(venueQrCode)
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

    fun isExpired(
        robertManager: RobertManager,
        unixTimeInMS: Long
    ): Boolean = unixTimeInMS + gracePeriod(robertManager) <= System.currentTimeMillis()

    internal suspend fun saveVenue(venueQrCode: VenueQrCode) {
        localKeystoreDataSource.insertAllVenuesQrCode(venueQrCode)
        venueListHasChanged()
    }

    override suspend fun getVenuesQrCode(
        startNtpTimestamp: Long?,
        endNtpTimestamp: Long?,
    ): List<VenueQrCode> = (startNtpTimestamp ?: Long.MIN_VALUE).let { forcedStartNtpTimestamp ->
        (endNtpTimestamp ?: Long.MAX_VALUE).let { forcedEndNtpTimestamp ->
            localKeystoreDataSource.venuesQrCode().data?.filter {
                it.ntpTimestamp in forcedStartNtpTimestamp..forcedEndNtpTimestamp
            }.orEmpty()
        }
    }

    suspend fun clearExpired(
        robertManager: RobertManager,
    ) {
        val expiredVenuesQrCode = localKeystoreDataSource.venuesQrCode().data?.filter {
            @Suppress("SENSELESS_COMPARISON")
            isExpired(
                robertManager,
                it.ntpTimestamp.ntpTimeSToUnixTimeMs()
            ) || it.ltid == null // This test is added to handle "old" venues that may have null here due to JSON parsing handling
        }.orEmpty()
        if (expiredVenuesQrCode.isNotEmpty()) {
            expiredVenuesQrCode.forEach {
                localKeystoreDataSource.deleteVenueQrCode(it.id)
            }
            venueListHasChanged()
        }
    }

    private fun gracePeriod(robertManager: RobertManager) =
        robertManager.configuration.venuesRetentionPeriod.days.inWholeMilliseconds

    suspend fun deleteVenue(venueId: String) {
        localKeystoreDataSource.deleteVenueQrCode(venueId)
        venueListHasChanged()
    }

    fun deleteDeprecatedVenues() {
        localKeystoreDataSource.deleteDeprecatedVenuesQrCode()
    }

    override suspend fun clearAllData() {
        localKeystoreDataSource.deleteAllVenuesQrCode()
        sharedPreferences.isVenueOnBoardingDone = false
        sharedPreferences.venuesFeaturedWasActivatedAtLeastOneTime = false
    }

    private fun venueListHasChanged() {
        // We reset Clea scoring iteration because we changed the venues
        localKeystoreDataSource.cleaLastStatusIteration = null
    }

    fun generateNewQRCodeIfNeeded(
        sharedPreferences: SharedPreferences,
        robertManager: RobertManager,
    ) {
        val nowCalendar = Calendar.getInstance()
        val lastGenerationCalendar = Calendar.getInstance().apply {
            timeInMillis = sharedPreferences.privateEventQrCodeGenerationDate
        }
        if (nowCalendar.get(Calendar.DAY_OF_YEAR) != lastGenerationCalendar.get(Calendar.DAY_OF_YEAR)
            || nowCalendar.get(Calendar.YEAR) != lastGenerationCalendar.get(Calendar.YEAR)) {
            val venueUrl = "${Constants.Url.VENUE_ROOT_URL}?code=${UUID.randomUUID()}&v=0"
            sharedPreferences.privateEventQrCode = venueUrl
            runBlocking {
            processVenueUrl(robertManager, venueUrl)
            }
            sharedPreferences.privateEventQrCodeGenerationDate = System.currentTimeMillis()
        }
    }

    suspend fun forceRefreshVenues() {
        localKeystoreDataSource.forceRefreshVenues()
    }

    suspend fun deleteLostVenues() {
        localKeystoreDataSource.deleteLostVenues()
    }
}
