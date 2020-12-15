package com.lunabeestudio.stopcovid.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.model.IsolationRecommendationStateEnum
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.security.KeyStore
import java.util.concurrent.TimeUnit

class IsolationManagerTest {

    private val context: StopCovid = ApplicationProvider.getApplicationContext() as StopCovid
    private val isolationManager: IsolationManager = context.isolationManager
    val secureKeystoreDataSource: SecureKeystoreDataSource by lazy {
        context.secureKeystoreDataSource
    }

    @Before
    fun createDataSource() {
        val keystore = KeyStore.getInstance("AndroidKeyStore")
        keystore.load(null)
        keystore.deleteEntry("aes_local_protection")
        keystore.deleteEntry("rsa_wrap_local_protection")
        context.getSharedPreferences("robert_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("crypto_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        isolationManager.resetData()
    }

    @After
    fun clear() {
        val keystore = KeyStore.getInstance("AndroidKeyStore")
        keystore.load(null)
        keystore.deleteEntry("aes_local_protection")
        keystore.deleteEntry("rsa_wrap_local_protection")
        context.getSharedPreferences("robert_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("crypto_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        isolationManager.resetData()
    }

    @Test
    fun initCase() {
        assert(isolationManager.currentFormStateValue == null) {
            "current form state should be null"
        }
        assert(isolationManager.currentRecommendationState == IsolationRecommendationStateEnum.INITIAL_CASE_SAFE) {
            "current recommendation state should be initial safe"
        }
        assert(isolationManager.currentIsolationEndDate == null) {
            "current form state should be null"
        }
    }

    @Test
    fun initCaseWithWarning() {
        notifyWarning(7)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.INITIAL_CASE_AT_RISK_OR_SICK)
        cancelNotifyWarning()
    }

    @Test
    fun initCaseWithAtRisk() {
        notifyRisk()
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.INITIAL_CASE_AT_RISK_OR_SICK)
        cancelNotifyRisk()
    }

    @Test
    fun initCaseWithReport() {
        report(1, true, 2)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.INITIAL_CASE_AT_RISK_OR_SICK)
        cancelReport()
    }

    @Test
    fun healthyCase() {
        isolationManager.updateState(IsolationFormStateEnum.ALL_GOOD)
        assert(isolationManager.currentFormStateValue == IsolationFormStateEnum.ALL_GOOD) {
            "current form state should be all good"
        }
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.ALL_GOOD)
        assert(isolationManager.currentIsolationEndDate == null) {
            "current form state should be null"
        }
    }

    @Test
    fun symptomsCase() {
        isolationManager.updateState(IsolationFormStateEnum.SYMPTOMS)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.SYMPTOMS)
        isolationManager.setNegativeTest()
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.SYMPTOMS_TESTED)
    }

    @Test
    fun contactCaseUnknownIndexTested() {
        contactCase()
        isolationManager.setIsKnownIndexAtHome(false)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.CONTACT_CASE_UNKNOWN_INDEX)
        isolationManager.setNegativeTest()
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.CONTACT_CASE_POST_ISOLATION_PERIOD)
    }

    @Test
    fun contactCaseKnownIndexTested() {
        contactCase()
        isolationManager.setIsKnownIndexAtHome(true)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.CONTACT_CASE_KNOWN_INDEX_NOT_TESTED)
        isolationManager.setNegativeTest()
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.INDETERMINATE)
        isolationManager.setKnowsIndexSymptomsEndDate(false)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.CONTACT_CASE_KNOWN_INDEX_TESTED_UNKNOWN_DATE)
        isolationManager.setKnowsIndexSymptomsEndDate(true)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.INDETERMINATE)
        setIndexEndSymptomDate()
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.CONTACT_CASE_KNOWN_INDEX_TESTED_KNOWN_DATE)
        setIndexEndSymptomDate(8)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.CONTACT_CASE_POST_ISOLATION_PERIOD)
        setIndexEndSymptomDate(8 + 8)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.INITIAL_CASE_SAFE)
    }

    @Test
    fun positiveCaseNoSymptoms() {
        positiveCase()
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.INDETERMINATE)
        setPositiveTestingDate()
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.INDETERMINATE)
        isolationManager.setIsHavingSymptoms(false)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.POSITIVE_CASE_NO_SYMPTOMS)
        setPositiveTestingDate(8)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.POSITIVE_CASE_POST_ISOLATION_PERIOD)
        setPositiveTestingDate(8 + 8)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.INITIAL_CASE_SAFE)
    }

    @Test
    fun positiveCaseWithSymptoms() {
        positiveCase()
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.INDETERMINATE)
        setPositiveTestingDate()
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.INDETERMINATE)
        isolationManager.setIsHavingSymptoms(true)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.INDETERMINATE)
        setSymptomsStartDate()
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.POSITIVE_CASE_SYMPTOMS_DURING_ISOLATION)
        setSymptomsStartDate(8)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.POSITIVE_CASE_SYMPTOMS_AFTER_ISOLATION)
        isolationManager.setStillHavingFever(true)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.POSITIVE_CASE_SYMPTOMS_AFTER_ISOLATION_STILL_HAVING_FEVER)
        isolationManager.setStillHavingFever(false)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.POSITIVE_CASE_POST_ISOLATION_PERIOD)
        setSymptomsStartDate(8 + 8)
        assertState(isolationManager.currentRecommendationState,
            IsolationRecommendationStateEnum.INITIAL_CASE_SAFE)
    }

    private fun assertState(has: IsolationRecommendationStateEnum, shouldHave: IsolationRecommendationStateEnum) {
        assert(shouldHave == has) {
            "current form state should be ${shouldHave.key} != ${has.key}"
        }
    }

    private fun contactCase() {
        isolationManager.updateState(IsolationFormStateEnum.CONTACT)
        assert(isolationManager.currentFormStateValue == IsolationFormStateEnum.CONTACT) {
            "current form state should be CONTACT"
        }
    }

    private fun positiveCase() {
        isolationManager.updateState(IsolationFormStateEnum.POSITIVE)
        assert(isolationManager.currentFormStateValue == IsolationFormStateEnum.POSITIVE) {
            "current form state should be POSITIVE"
        }
    }

    private fun setSymptomsStartDate(nDaysAgo: Int? = null) {
        isolationManager.updateSymptomsStartDate(daysAgo(nDaysAgo))
    }

    private fun setPositiveTestingDate(nDaysAgo: Int? = null) {
        isolationManager.updatePositiveTestingDate(daysAgo(nDaysAgo))
    }

    private fun setIndexEndSymptomDate(nDaysAgo: Int? = null) {
        isolationManager.updateIndexSymptomsEndDate(daysAgo(nDaysAgo))
    }

    private fun notifyWarning(nDaysAgo: Int? = null) {
        secureKeystoreDataSource.isWarningAtRisk = true
        secureKeystoreDataSource.lastWarningReceivedDate = daysAgo(nDaysAgo)
    }

    private fun cancelNotifyWarning() {
        secureKeystoreDataSource.lastWarningReceivedDate = null
    }

    private fun notifyRisk(nDaysAgo: Int? = null) {
        secureKeystoreDataSource.lastRiskReceivedDate = daysAgo(nDaysAgo)
    }

    private fun cancelNotifyRisk() {
        secureKeystoreDataSource.lastRiskReceivedDate = null
    }

    private fun report(nDaysAgo: Int?, withSymptoms: Boolean = false, symptomsStartedDaysAgo: Int? = null) {
        secureKeystoreDataSource.isSick = true
        secureKeystoreDataSource.reportPositiveTestDate = daysAgo(nDaysAgo)

        if (withSymptoms) {
            secureKeystoreDataSource.reportSymptomsStartDate = symptomsStartedDaysAgo?.let {
                secureKeystoreDataSource.reportPositiveTestDate?.minus(TimeUnit.DAYS.toMillis(it.toLong()))
            } ?: secureKeystoreDataSource.reportPositiveTestDate
        }
    }

    private fun cancelReport() {
        secureKeystoreDataSource.isSick = false
        secureKeystoreDataSource.reportPositiveTestDate = null
        secureKeystoreDataSource.reportSymptomsStartDate = null
    }

    private fun daysAgo(nDaysAgo: Int?): Long {
        return nDaysAgo?.let {
            System.currentTimeMillis()
                .minus(TimeUnit.DAYS.toMillis(it.toLong()))
        } ?: System.currentTimeMillis()
    }

}