package com.lunabeestudio.stopcovid.clea

import android.content.Context
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeS
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.venueRepository
import com.lunabeestudio.stopcovid.model.VenueExpiredException
import com.lunabeestudio.stopcovid.model.VenueInvalidFormatException
import com.lunabeestudio.stopcovid.repository.VenueRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.security.KeyStore
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class VenuesManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val venueRepository: VenueRepository by lazy {
        context.venueRepository()
    }

    @Before
    @UiThreadTest
    fun createDataSource() {
        val keystore = KeyStore.getInstance(LocalCryptoManager.ANDROID_KEY_STORE_PROVIDER)
        keystore.load(null)
        keystore.deleteEntry("aes_local_protection")
        keystore.deleteEntry("rsa_wrap_local_protection")
        context.getSharedPreferences(SecureKeystoreDataSource.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(LocalCryptoManager.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        runBlocking {
            venueRepository.clearAllData()
        }
    }

    @After
    @UiThreadTest
    fun clear() {
        val keystore = KeyStore.getInstance(LocalCryptoManager.ANDROID_KEY_STORE_PROVIDER)
        keystore.load(null)
        keystore.deleteEntry("aes_local_protection")
        keystore.deleteEntry("rsa_wrap_local_protection")
        context.getSharedPreferences(SecureKeystoreDataSource.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(LocalCryptoManager.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        runBlocking {
            venueRepository.clearAllData()
        }
    }

    @Test
    @UiThreadTest
    fun save_and_get() {
        val venue1 = VenueQrCode(
            id = "idtest1",
            ltid = "uuid",
            ntpTimestamp = 1L,
            base64URL = "GA",
            version = 0
        )
        val venue2 = VenueQrCode(
            id = "idtest2",
            ltid = "uuid",
            ntpTimestamp = System.currentTimeMillis().unixTimeMsToNtpTimeS(),
            base64URL = "GA",
            version = 0
        )
        val venue3 = VenueQrCode(
            id = "idtest3",
            ltid = "uuid",
            base64URL = "GA",
            ntpTimestamp = 2L,
            version = 0
        )
        val venue4 = VenueQrCode(
            id = "idtest4",
            ltid = "uuid",
            base64URL = "GA",
            ntpTimestamp = Long.MAX_VALUE,
            version = 0
        )
        runBlocking {
            venueRepository.clearAllData()
            assert(venueRepository.getVenuesQrCode().count() == 0)
            venueRepository.saveVenue(venue1)
            venueRepository.saveVenue(venue2)
            venueRepository.saveVenue(venue3)
            venueRepository.saveVenue(venue4)
            assert(
                venueRepository.getVenuesQrCode(
                    endNtpTimestamp = System.currentTimeMillis().unixTimeMsToNtpTimeS(),
                ).count() == 3
            )
            assert(venueRepository.getVenuesQrCode()[0] == venue1)
            assert(venueRepository.getVenuesQrCode()[1] == venue2)
            assert(
                venueRepository.getVenuesQrCode(
                    startNtpTimestamp = 0L,
                    endNtpTimestamp = System.currentTimeMillis().unixTimeMsToNtpTimeS(),
                ).count() == 3
            )
            assert(
                venueRepository.getVenuesQrCode(
                    startNtpTimestamp = 2L,
                    endNtpTimestamp = System.currentTimeMillis().unixTimeMsToNtpTimeS(),
                ).count() == 2
            )
            assert(venueRepository.getVenuesQrCode().count() == 4)
            assert(venueRepository.getVenuesQrCode(2L).count() == 3)

            venueRepository.clearAllData()
            assert(venueRepository.getVenuesQrCode().count() == 0)
        }
    }

    @Test
    @UiThreadTest
    fun clear_expired() {
        val gracePeriod = context.robertManager().configuration.venuesRetentionPeriod.days.inWholeMilliseconds
        val venue1 = VenueQrCode(
            id = "idtest1",
            ltid = "uuid",
            base64URL = "GA",
            ntpTimestamp = (System.currentTimeMillis() - gracePeriod - 10.seconds.inWholeMilliseconds).unixTimeMsToNtpTimeS(),
            version = 0
        )
        val venue2 = VenueQrCode(
            id = "idtest2",
            ltid = "uuid",
            base64URL = "GA",
            ntpTimestamp = (System.currentTimeMillis() - gracePeriod + 10.seconds.inWholeMilliseconds).unixTimeMsToNtpTimeS(),
            version = 0
        )
        runBlocking {
            venueRepository.clearAllData()
            assert(venueRepository.getVenuesQrCode().count() == 0)
            venueRepository.saveVenue(venue1)
            venueRepository.saveVenue(venue2)
            assert(venueRepository.getVenuesQrCode().count() == 2)
            venueRepository.clearExpired(context.robertManager())
            assert(venueRepository.getVenuesQrCode().count() == 1)
            assert(venueRepository.getVenuesQrCode()[0] == venue2)

            venueRepository.clearAllData()
            assert(venueRepository.getVenuesQrCode().count() == 0)
        }
    }

    private val base64: Array<String> = arrayOf(
        "AKIK6oBFzttNk2qufUM59D51sL9HuLJ2OXbS1Jbi68RutMcXO1jW399Vw7486Sktdljx7YXhA6l9kPN7wRaouYv1Qf//" +
            "zy25pbnCD+ICSU8TwupeYTPG5LMXb4jCjBZkyEml8eUOPONtZ7aumJA=",
        "AGUKYcvMWTLqqUAEMbqcq0saGh3vPKTcfPSTCfsQcodWkmUie/y47eeZ3rcgj3Sl9xcnagy9mSQkCGCHltHbLBbAn1m9" +
            "5o4MYxYndNCdGz0FzODBl/0CE/OKtPDrS+wI5wt7cZEbQqd1bhJ8HH4dYKdL0Ibh2ipNzzIIVRTOU3LZwX3Yla5+" +
            "k7AdP26fHQJGUh1BJz3UEJgePesl4/WIGZg/d2tCdO2SBH2yYVSkEw==",
        "AMXb4D9ATjo7oNqzIOrOPfOlYMEGrz1K97YPL5vZ8Js6jwzS8IxbUN6L/qO9d/xsnx1lKiqXLmK5AtGQi1Kn68iA2Ahu" +
            "75cPPc9dBIlRcnaTFk46L9VQzKsuGJz5sspIdEVrf2PnAdhTsHN7CZyhocrHMgCtGDtKKQ75dYEXYWGbNpzWGr5S" +
            "7I124pjXxAMZD/i9wvO0RVuwF5cVG6mijM5MNMDXrUTrxh6XDaOKDQ==",
        "ABfHCL7QijS4iFvF7BRj+yFZdr7bIjdvFHV6DGMVZUFiiAJWgvqVQRMoeil/R/9fZJ5YjNDhkzuqZueJ96eaaX4Jh+iU" +
            "y7KjTAyMquEKk9zhpVEl0BmWp0dtoDaPq4CxHSO+5RBzPihTgDYClxIzOCIOFbAlLIQpz+jS0s4NijlnhNDyqYMn" +
            "dYTM+yBrJAOoWSFtTlImQG378aXzmAIO9XRvBGB9iGnvjW7el2YHZQ=="
    )
    private val ltid: Array<String> = arrayOf(
        "a20aea80-45ce-db4d-936a-ae7d4339f43e",
        "650a61cb-cc59-32ea-a940-0431ba9cab4b",
        "c5dbe03f-404e-3a3b-a0da-b320eace3df3",
        "17c708be-d08a-34b8-885b-c5ec1463fb21"
    )
    private val timeStampsNTP: Array<Long> = arrayOf(
        System.currentTimeMillis().unixTimeMsToNtpTimeS(),
        (System.currentTimeMillis() - 1000L).unixTimeMsToNtpTimeS(),
        (System.currentTimeMillis() - 2000L).unixTimeMsToNtpTimeS(),
        (System.currentTimeMillis() - 3000L).unixTimeMsToNtpTimeS()
    )

    private val base64URLs = base64.map { it.toBase64URL() }
    private var succesfullQrCodes: List<String> = base64.mapIndexed { index, base64 ->
        "https://tac.gouv.fr?v=0&t=${timeStampsNTP[index].ntpTimeSToUnixTimeS()}#${base64.toBase64URL()}"
    }

    private fun populateWithData(usingUrl: Boolean = true) {
        runBlocking {
            base64.forEachIndexed { index, s ->
                if (usingUrl) {
                    try {
                        venueRepository.processVenueUrl(
                            robertManager = context.robertManager(),
                            succesfullQrCodes[index]
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error with URL ${succesfullQrCodes[index]} !!")
                    }
                } else {
                    try {
                        venueRepository.processVenue(
                            robertManager = context.robertManager(),
                            base64URLs[index],
                            0,
                            timeStampsNTP[index].ntpTimeSToUnixTimeS()
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error with Code $s !!")
                    }
                }
            }
        }
    }

    @Test
    @UiThreadTest
    fun venue_expired_test() {
        val gracePeriod = context.robertManager().configuration.venuesRetentionPeriod.days.toLong(DurationUnit.SECONDS)

        assertThrows<VenueExpiredException> {
            runBlocking {
                venueRepository.processVenue(
                    robertManager = context.robertManager(),
                    base64URLCode = base64URLs[0],
                    version = 0,
                    unixTimeInSeconds = System.currentTimeMillis() / 1000L - gracePeriod - 10
                )
            }
        }
    }

    @Test
    @UiThreadTest
    fun venue_url_parsing() {

        val failingQrCodes = arrayOf(
            "",
            "test",
            "https://www.google.com",
            "https://tac.gouv.fr/0/491ab3/GA/4/400/",
            "https://tac.gouv.fr/491ab3ae-ad35-4301-8dd9-414ecf210712/0/GA/4/400/"
        )

        failingQrCodes.forEach {
            assertThrows<VenueInvalidFormatException> {
                runBlocking {
                    venueRepository.processVenueUrl(
                        robertManager = context.robertManager(),
                        it
                    )
                }
            }
        }

        populateWithData()

        val venueQrCodes = runBlocking {
            venueRepository.getVenuesQrCode()
        }
        assert(venueQrCodes.size == succesfullQrCodes.size) {
            "Only ${venueQrCodes.size} venues created, expecting ${succesfullQrCodes.size}"
        }
        venueQrCodes.forEachIndexed { index, venueQrCode ->
            assertNotNull("venue QR code should exist", venueQrCode)
            assert(venueQrCode.ltid == ltid[index]) { "UUID is wrong" }
            assert(venueQrCode.base64URL == base64URLs[index])
            assert(venueQrCode.ntpTimestamp == timeStampsNTP[index])
        }
    }

    @Test
    @UiThreadTest
    fun remove_venue() {

        populateWithData()

        runBlocking {
            assert(venueRepository.getVenuesQrCode().size == succesfullQrCodes.size) {
                "Only ${
                venueRepository.getVenuesQrCode().size
                } venues created, excpecting ${succesfullQrCodes.size}"
            }
            venueRepository.deleteVenue(venueRepository.getVenuesQrCode()[1].id)
            assert(venueRepository.getVenuesQrCode().size == succesfullQrCodes.size - 1) {
                "Should have ${succesfullQrCodes.size - 1} element"
            }
            assert(venueRepository.getVenuesQrCode()[0].ltid == ltid[0]) {
                "First element shouldn't be removed"
            }
            assert(venueRepository.getVenuesQrCode()[1].ltid == ltid[2]) {
                "Third element shouldn't be removed"
            }
            venueRepository.deleteVenue(venueRepository.getVenuesQrCode()[0].id)
            assert(venueRepository.getVenuesQrCode().size == succesfullQrCodes.size - 2) {
                "Should have ${succesfullQrCodes.size - 2} element"
            }
            assert(venueRepository.getVenuesQrCode()[0].ltid == ltid[2]) {
                "Second element shouldn't be removed"
            }
            venueRepository.deleteVenue(venueRepository.getVenuesQrCode()[0].id)
            venueRepository.deleteVenue(venueRepository.getVenuesQrCode()[0].id)
            assert(venueRepository.getVenuesQrCode().isEmpty()) {
                "Should be empty"
            }
        }
    }

    private inline fun <reified T : Exception> assertThrows(runnable: () -> Any?) {
        try {
            runnable.invoke()
        } catch (e: Throwable) {
            if (e is T) {
                return
            }
            Assert.fail(
                "expected ${T::class.qualifiedName} but caught " +
                    "${e::class.qualifiedName} instead"
            )
        }
        Assert.fail("expected ${T::class.qualifiedName}")
    }
}

private fun String.toBase64URL(): String = this.apply {
    val replaceCharacters = arrayOf(
        arrayOf("+", "-"),
        arrayOf("/", "_")
    )
    var result = this
    replaceCharacters.forEach {
        result = result.replace(it[0], it[1])
    }
    return result
}
