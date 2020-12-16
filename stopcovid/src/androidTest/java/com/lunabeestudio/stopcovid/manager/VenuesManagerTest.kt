package com.lunabeestudio.stopcovid.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.domain.model.VenueQrType
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.privateEventQrCode
import com.lunabeestudio.stopcovid.extension.privateEventQrCodeGenerationDate
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import junit.framework.Assert.assertNotNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.security.KeyStore
import java.util.Calendar
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
        sharedPrefs.privateEventQrCode = null
        sharedPrefs.privateEventQrCodeGenerationDate = 0L

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
        sharedPrefs.privateEventQrCode = null
        sharedPrefs.privateEventQrCodeGenerationDate = 0L
        VenuesManager.clearAllData(sharedPrefs, keystoreDataSource)
    }

    @Test
    fun save_and_get() {
        val venue1 = VenueQrCode(
            "idtest1",
            "uuid",
            VenueQrType.STATIC,
            "GA",
            1L,
            2,
            3,
            "playload"
        )
        val venue2 = VenueQrCode(
            "idtest2",
            "uuid",
            VenueQrType.STATIC,
            "GA",
            System.currentTimeMillis(),
            null,
            null,
            "playload"
        )
        val venue3 = VenueQrCode(
            "idtest2",
            "uuid",
            VenueQrType.STATIC,
            "GA",
            1L,
            null,
            null,
            "playload"
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
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() == 2)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(0) == venue1)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(1) == venue2)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource, 0L)?.count() == 2)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource, 2L)?.count() == 1)

        VenuesManager.clearAllData(sharedPrefs, keystoreDataSource)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() ?: 0 == 0)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun clear_expired() {
        val gracePeriod = context.robertManager().venuesRetentionPeriod.days.toLongMilliseconds()
        val venue1 = VenueQrCode(
            "idtest1",
            "uuid",
            VenueQrType.STATIC,
            "GA",
            (System.currentTimeMillis() - gracePeriod - 10.seconds.toLongMilliseconds()).unixTimeMsToNtpTimeS(),
            2,
            3,
            "playload"
        )
        val venue2 = VenueQrCode(
            "idtest2",
            "uuid",
            VenueQrType.STATIC,
            "GA",
            (System.currentTimeMillis() - gracePeriod + 10.seconds.toLongMilliseconds()).unixTimeMsToNtpTimeS(),
            null,
            null,
            "playload"
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

    @Test
    fun venue_url_parsing() {
        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            ""
        )) { "Invalid format" }

        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "test"
        )) { "Invalid format" }

        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://www.google.com"
        )) { "Invalid format" }

        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/0/491ab3/GA/4/400/"
        )) { "Invalid format" }

        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/491ab3ae-ad35-4301-8dd9-414ecf210712/0/GA/4/400/"
        )) { "Invalid format (wrong argument position" }

        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/-1/491ab3ae-ad35-4301-8dd9-414ecf210712/GA/4/400/"
        )) { "Invalid QR code type" }

        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/2/491ab3ae-ad35-4301-8dd9-414ecf210712/GA/4/400/"
        )) { "Invalid QR code type" }

        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/0/491ab3ae-ad35-4301-8dd9-414ecf21071/GA/4/400/"
        )) { "Invalid UUID" }

        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/0/491ab3ae-ad35-4301-8dd9-414ecf210712//4/400/"
        )) { "Invalid venue type not enough character" }

        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/0/491ab3ae-ad35-4301-8dd9-414ecf210712/1234/4/400/"
        )) { "Invalid venue type too many character" }

        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/0/491ab3ae-ad35-4301-8dd9-414ecf210712/1/-1/400/"
        )) { "Invalid venue category should be > 0" }

        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/0/491ab3ae-ad35-4301-8dd9-414ecf210712/1/6/400/"
        )) { "Invalid venue category should be < 6" }

        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/0/491ab3ae-ad35-4301-8dd9-414ecf210712/1/6/-400/"
        )) { "Invalid venue capacity should be > 0" }

        assertNull(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gousv.fr/0/491ab3ae-ad35-4301-8dd9-414ecf210712/l"
        )) { "Invalid root url" }

        assert(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/0/491ab3ae-ad35-4301-8dd9-414ecf210712/l"
        ) == "L")

        val venueQrCode = VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(0)
        assertNotNull("venue QR code should exist", venueQrCode)
        assert(venueQrCode!!.id == "491ab3ae-ad35-4301-8dd9-414ecf210712${venueQrCode.ntpTimestamp}") { "id is wrong" }
        assert(venueQrCode.qrType == VenueQrType.STATIC) { "Qr type is wrong" }
        assert(venueQrCode.uuid == "491ab3ae-ad35-4301-8dd9-414ecf210712") { "UUID is wrong" }
        assert(venueQrCode.venueType == "L") { "Venue type is wrong" }
        assertNotNull("Venue category should be null", venueQrCode.venueCategory)
        assertNotNull("Venue capacity should be null", venueQrCode.venueCapacity)

        assert(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/1/491ab3ae-ad35-4301-8dd9-414ecf210713/GA/4/400/"
        ) == "GA")
        val venue2QrCode = VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(1)
        assertNotNull("venue QR code should exist", venueQrCode)
        assert(venue2QrCode!!.id == "491ab3ae-ad35-4301-8dd9-414ecf210713${venueQrCode.ntpTimestamp}") { "id is wrong" }
        assert(venue2QrCode.qrType == VenueQrType.DYNAMIC) { "Qr type is wrong" }
        assert(venue2QrCode.uuid == "491ab3ae-ad35-4301-8dd9-414ecf210713") { "UUID is wrong" }
        assert(venue2QrCode.venueType == "GA") { "Venue type is wrong" }
        assert(venue2QrCode.venueCategory == 4) { "Venue category is wrong" }
        assert(venue2QrCode.venueCapacity == 400) { "Venue capacity is wrong" }

        assert(VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/0/491ab3ae-ad35-4301-8dd9-414ecf210712/l/"
        ) == "L")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun generate_new_private_event() {
        assertNull(sharedPrefs.privateEventQrCode) { "privateEventQrCode should be null" }
        assert(sharedPrefs.privateEventQrCodeGenerationDate == 0L) { "privateEventQrCodeGenerationDate should be 0L" }

        VenuesManager.generateNewQRCodeIfNeeded(sharedPrefs, context.robertManager(), keystoreDataSource)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() == 1) { "Should have one venue" }
        var eventCode = sharedPrefs.privateEventQrCode
        val venueQrCode = VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(0)
        assertNotNull("venue QR code should exist", venueQrCode)
        assert(venueQrCode?.qrType == VenueQrType.STATIC) { "Qr type is wrong" }
        assert(eventCode?.contains(venueQrCode?.uuid!!) == true) { "UUID is wrong" }
        assert(venueQrCode?.venueType == context.robertManager().privateEventVenueType) { "Venue type is wrong" }
        assertNotNull("Venue category should be null", venueQrCode?.venueCategory)
        assertNotNull("Venue capacity should be null", venueQrCode?.venueCapacity)

        VenuesManager.generateNewQRCodeIfNeeded(sharedPrefs, context.robertManager(), keystoreDataSource)
        assert(eventCode == sharedPrefs.privateEventQrCode) { "uuid shouldn't change" }
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() == 1) { "Should have only one venue" }

        sharedPrefs.privateEventQrCodeGenerationDate = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, get(Calendar.DAY_OF_YEAR) - 1)
        }.timeInMillis
        VenuesManager.generateNewQRCodeIfNeeded(sharedPrefs, context.robertManager(), keystoreDataSource)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() == 2) { "Should have two venues" }
        assert(eventCode != sharedPrefs.privateEventQrCode) { "uuid should have changed" }
        eventCode = sharedPrefs.privateEventQrCode

        sharedPrefs.privateEventQrCodeGenerationDate = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, get(Calendar.DAY_OF_YEAR) + 1)
        }.timeInMillis
        VenuesManager.generateNewQRCodeIfNeeded(sharedPrefs, context.robertManager(), keystoreDataSource)
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() == 3) { "Should have three venues" }
        assert(eventCode != sharedPrefs.privateEventQrCode) { "uuid should have changed" }
    }

    @Test
    fun remove_venue() {
        VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/0/491ab3ae-ad35-4301-8dd9-414ecf210711/l"
        )
        VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/0/491ab3ae-ad35-4301-8dd9-414ecf210712/l"
        )
        VenuesManager.processVenueUrl(
            robertManager = context.robertManager(),
            secureKeystoreDataSource = keystoreDataSource,
            "https://tac.gouv.fr/0/491ab3ae-ad35-4301-8dd9-414ecf210713/l"
        )
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() == 3) { "Should have 3 element" }
        VenuesManager.removeVenue(keystoreDataSource, VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(1)?.id ?: "")
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() == 2) { "Should have 2 element" }
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)
            ?.get(0)?.uuid == "491ab3ae-ad35-4301-8dd9-414ecf210711") { "First element shouldn't be removed" }
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)
            ?.get(1)?.uuid == "491ab3ae-ad35-4301-8dd9-414ecf210713") { "Third element shouldn't be removed" }
        VenuesManager.removeVenue(keystoreDataSource, VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(0)?.id ?: "")
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() == 1) { "Should have 1 element" }
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)
            ?.get(0)?.uuid == "491ab3ae-ad35-4301-8dd9-414ecf210713") { "Second element shouldn't be removed" }
        VenuesManager.removeVenue(keystoreDataSource, VenuesManager.getVenuesQrCode(keystoreDataSource)?.get(0)?.id ?: "")
        assert(VenuesManager.getVenuesQrCode(keystoreDataSource)?.count() == 0) { "Should be empty" }
    }

    private fun assertNull(value: Any?, lazyMessage: () -> Any) {
        assert(value == null, lazyMessage)
    }
}