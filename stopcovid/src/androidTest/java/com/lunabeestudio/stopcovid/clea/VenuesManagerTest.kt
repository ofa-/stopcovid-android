package com.lunabeestudio.stopcovid.clea

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeS
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.manager.VenuesManager
import com.lunabeestudio.stopcovid.model.VenueExpiredException
import com.lunabeestudio.stopcovid.model.VenueInvalidFormatException
import junit.framework.Assert.assertNotNull
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.security.KeyStore
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.days
import kotlin.time.seconds

class VenuesManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var keystoreDataSource: SecureKeystoreDataSource

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Before
    fun createDataSource() {
        val keystore = KeyStore.getInstance("AndroidKeyStore")
        keystore.load(null)
        keystore.deleteEntry("aes_local_protection")
        keystore.deleteEntry("rsa_wrap_local_protection")
        context.getSharedPreferences("robert_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("crypto_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        keystoreDataSource = context.secureKeystoreDataSource()
        VenuesManager.clearAllData(sharedPrefs, keystoreDataSource)
    }

    @After
    fun clear() {
        val keystore = KeyStore.getInstance("AndroidKeyStore")
        keystore.load(null)
        keystore.deleteEntry("aes_local_protection")
        keystore.deleteEntry("rsa_wrap_local_protection")
        context.getSharedPreferences("robert_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("crypto_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        VenuesManager.clearAllData(sharedPrefs, keystoreDataSource)
    }

    @Test
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
        VenuesManager.clearAllData(sharedPrefs, keystoreDataSource)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() ?: 0 == 0)

        val saveVenueMethod = VenuesManager.javaClass.getDeclaredMethod(
            "saveVenue",
            SecureKeystoreDataSource::class.java,
            VenueQrCode::class.java
        )
        saveVenueMethod.isAccessible = true
        val parameters = arrayOfNulls<Any>(2)
        parameters[0] = keystoreDataSource
        parameters[1] = venue1
        saveVenueMethod.invoke(VenuesManager, *parameters)
        parameters[1] = venue2
        saveVenueMethod.invoke(VenuesManager, *parameters)
        parameters[1] = venue3
        saveVenueMethod.invoke(VenuesManager, *parameters)
        parameters[1] = venue4
        saveVenueMethod.invoke(VenuesManager, *parameters)
        assert(
            VenuesManager.getVenuesQrCode(
                keystoreDataSource,
                endNtpTimestamp = System.currentTimeMillis().unixTimeMsToNtpTimeS(),
            )?.count() == 3
        )
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(0) == venue1)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(1) == venue2)
        assert(
            VenuesManager.getVenuesQrCode(
                keystoreDataSource,
                startNtpTimestamp = 0L,
                endNtpTimestamp = System.currentTimeMillis().unixTimeMsToNtpTimeS(),
            )?.count() == 3
        )
        assert(
            VenuesManager.getVenuesQrCode(
                keystoreDataSource,
                startNtpTimestamp = 2L,
                endNtpTimestamp = System.currentTimeMillis().unixTimeMsToNtpTimeS(),
            )?.count() == 2
        )
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() == 4)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource, 2L)?.count() == 3)

        VenuesManager.clearAllData(sharedPrefs, keystoreDataSource)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() ?: 0 == 0)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun clear_expired() {
        val gracePeriod = context.robertManager().configuration.venuesRetentionPeriod.days.toLongMilliseconds()
        val venue1 = VenueQrCode(
            id = "idtest1",
            ltid = "uuid",
            base64URL = "GA",
            ntpTimestamp = (System.currentTimeMillis() - gracePeriod - 10.seconds.toLongMilliseconds()).unixTimeMsToNtpTimeS(),
            version = 0
        )
        val venue2 = VenueQrCode(
            id = "idtest2",
            ltid = "uuid",
            base64URL = "GA",
            ntpTimestamp = (System.currentTimeMillis() - gracePeriod + 10.seconds.toLongMilliseconds()).unixTimeMsToNtpTimeS(),
            version = 0
        )
        VenuesManager.clearAllData(sharedPrefs, keystoreDataSource)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() ?: 0 == 0)

        val saveVenueMethod = VenuesManager.javaClass.getDeclaredMethod(
            "saveVenue",
            SecureKeystoreDataSource::class.java,
            VenueQrCode::class.java
        )
        saveVenueMethod.isAccessible = true
        val parameters = arrayOfNulls<Any>(2)
        parameters[0] = keystoreDataSource
        parameters[1] = venue1
        saveVenueMethod.invoke(VenuesManager, *parameters)
        parameters[1] = venue2
        saveVenueMethod.invoke(VenuesManager, *parameters)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() == 2)
        VenuesManager.clearExpired(context.robertManager(), keystoreDataSource)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() == 1)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(0) == venue2)

        VenuesManager.clearAllData(sharedPrefs, keystoreDataSource)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() ?: 0 == 0)
    }

    val base64 = arrayOf(
        "AKIK6oBFzttNk2qufUM59D51sL9HuLJ2OXbS1Jbi68RutMcXO1jW399Vw7486Sktdljx7YXhA6l9kPN7wRaouYv1Qf//zy25pbnCD+ICSU8TwupeYTPG5LMXb4jCjBZkyEml8eUOPONtZ7aumJA=",
        "AGUKYcvMWTLqqUAEMbqcq0saGh3vPKTcfPSTCfsQcodWkmUie/y47eeZ3rcgj3Sl9xcnagy9mSQkCGCHltHbLBbAn1m95o4MYxYndNCdGz0FzODBl/0CE/OKtPDrS+wI5wt7cZEbQqd1bhJ8HH4dYKdL0Ibh2ipNzzIIVRTOU3LZwX3Yla5+k7AdP26fHQJGUh1BJz3UEJgePesl4/WIGZg/d2tCdO2SBH2yYVSkEw==",
        "AMXb4D9ATjo7oNqzIOrOPfOlYMEGrz1K97YPL5vZ8Js6jwzS8IxbUN6L/qO9d/xsnx1lKiqXLmK5AtGQi1Kn68iA2Ahu75cPPc9dBIlRcnaTFk46L9VQzKsuGJz5sspIdEVrf2PnAdhTsHN7CZyhocrHMgCtGDtKKQ75dYEXYWGbNpzWGr5S7I124pjXxAMZD/i9wvO0RVuwF5cVG6mijM5MNMDXrUTrxh6XDaOKDQ==",
        "ABfHCL7QijS4iFvF7BRj+yFZdr7bIjdvFHV6DGMVZUFiiAJWgvqVQRMoeil/R/9fZJ5YjNDhkzuqZueJ96eaaX4Jh+iUy7KjTAyMquEKk9zhpVEl0BmWp0dtoDaPq4CxHSO+5RBzPihTgDYClxIzOCIOFbAlLIQpz+jS0s4NijlnhNDyqYMndYTM+yBrJAOoWSFtTlImQG378aXzmAIO9XRvBGB9iGnvjW7el2YHZQ=="
    )
    val ltid = arrayOf(
        "a20aea80-45ce-db4d-936a-ae7d4339f43e",
        "650a61cb-cc59-32ea-a940-0431ba9cab4b",
        "c5dbe03f-404e-3a3b-a0da-b320eace3df3",
        "17c708be-d08a-34b8-885b-c5ec1463fb21"
    )
    val timeStampsNTP = arrayOf(
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
        base64.forEachIndexed { index, s ->
            if (usingUrl) {
                try {
                    VenuesManager.processVenueUrl(
                        robertManager = context.robertManager(),
                        secureKeystoreDataSource = keystoreDataSource,
                        succesfullQrCodes[index]
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error with URL ${succesfullQrCodes[index]} !!")
                }
            } else {
                try {
                    VenuesManager.processVenue(
                        robertManager = context.robertManager(),
                        secureKeystoreDataSource = keystoreDataSource,
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

    @ExperimentalTime
    @Test
    fun venue_expired_test() {
        val gracePeriod = context.robertManager().configuration.venuesRetentionPeriod.days.toLong(DurationUnit.SECONDS)

        assertThrows<VenueExpiredException> {
            VenuesManager.processVenue(
                robertManager = context.robertManager(),
                secureKeystoreDataSource = keystoreDataSource,
                base64URLCode = base64URLs[0],
                version = 0,
                unixTimeInSeconds = System.currentTimeMillis() / 1000L - gracePeriod - 10
            )
        }
    }

    @Test
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
                VenuesManager.processVenueUrl(
                    robertManager = context.robertManager(),
                    secureKeystoreDataSource = keystoreDataSource,
                    it
                )
            }
        }

        populateWithData()

        val venueQrCodes = VenuesManager.getVenuesQrCode(keystoreDataSource)
        assert(venueQrCodes?.size == succesfullQrCodes.size) { "Only ${venueQrCodes?.size} venues created, expecting ${succesfullQrCodes.size}" }
        venueQrCodes?.forEachIndexed { index, venueQrCode ->
            assertNotNull("venue QR code should exist", venueQrCode)
            assert(venueQrCode.ltid == ltid[index]) { "UUID is wrong" }
            assert(venueQrCode.base64URL == base64URLs[index])
            assert(venueQrCode.ntpTimestamp == timeStampsNTP[index])
        }

    }

    @Test
    fun remove_venue() {

        populateWithData()

        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.size == succesfullQrCodes.size) {
            "Only ${
                VenuesManager.getVenuesQrCode(keystoreDataSource)?.size
            } venues created, excpecting ${succesfullQrCodes.size}"
        }
        VenuesManager.removeVenue(keystoreDataSource, VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(1)?.id ?: "")
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.size == succesfullQrCodes.size - 1) { "Should have ${succesfullQrCodes.size - 1} element" }
        assert(
            VenuesManager.getVenuesQrCode(keystoreDataSource)
                ?.get(0)?.ltid == ltid[0]
        ) { "First element shouldn't be removed" }
        assert(
            VenuesManager.getVenuesQrCode(keystoreDataSource)
                ?.get(1)?.ltid == ltid[2]
        ) { "Third element shouldn't be removed" }
        VenuesManager.removeVenue(keystoreDataSource, VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(0)?.id ?: "")
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.size == succesfullQrCodes.size - 2) { "Should have ${succesfullQrCodes.size - 2} element" }
        assert(
            VenuesManager.getVenuesQrCode(keystoreDataSource)
                ?.get(0)?.ltid == ltid[2]
        ) { "Second element shouldn't be removed" }
        VenuesManager.removeVenue(keystoreDataSource, VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(0)?.id ?: "")
        VenuesManager.removeVenue(keystoreDataSource, VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(0)?.id ?: "")
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.isEmpty() == true) { "Should be empty" }
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
