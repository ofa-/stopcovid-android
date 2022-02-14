/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/7 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.usecase

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.domain.model.smartwallet.SmartWalletVacc
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.extension.isRecoveryOrTestPositive
import com.lunabeestudio.stopcovid.extension.recoveryDateOfFirstPositiveTest
import com.lunabeestudio.stopcovid.extension.vaccineDate
import com.lunabeestudio.stopcovid.extension.vaccineDose
import com.lunabeestudio.stopcovid.extension.vaccineMedicinalProduct
import com.lunabeestudio.stopcovid.extension.yearMonthDayUsParser
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.manager.SmartWalletEligibilityManager
import com.lunabeestudio.stopcovid.manager.SmartWalletValidityManager
import com.lunabeestudio.stopcovid.manager.model.ApiSmartWalletEligibilityPivot
import com.lunabeestudio.stopcovid.manager.model.ApiSmartWalletValidityPivot
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.SmartWalletTesting
import com.lunabeestudio.stopcovid.model.SmartWalletTestingCombo
import com.lunabeestudio.stopcovid.repository.WalletRepository
import com.lunabeestudio.stopcovid.utils.ResourcesHelper
import dgca.verifier.app.decoder.model.GreenCertificate
import dgca.verifier.app.decoder.model.RecoveryStatement
import dgca.verifier.app.decoder.model.Vaccination
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.text.DateFormat
import kotlin.test.assertEquals

class ComputeDccValidityEligibilityUseCaseTest {

    private lateinit var validityUseCase: ComputeDccValidityUseCase
    private lateinit var eligibilityUseCase: ComputeDccEligibilityUseCase
    private lateinit var getSmartWalletStateUseCase: GetSmartWalletStateUseCase
    private lateinit var getSmartWalletMapUseCase: GetSmartWalletMapUseCase

    private val robertManager = mockk<RobertManager>(relaxed = true)
    private val blacklistDCCManager = mockk<BlacklistDCCManager>(relaxed = true)
    private val walletRepository = mockk<WalletRepository>(relaxed = true)

    private lateinit var testData: SmartWalletTesting
    private lateinit var comboTestData: SmartWalletTestingCombo

    @Before
    fun init() {

        val gson = Gson()

        testData = gson.fromJson(ResourcesHelper.readTestFileAsString("testing.json"), SmartWalletTesting::class.java)
        comboTestData = gson.fromJson(ResourcesHelper.readTestFileAsString("testing-combo.json"), SmartWalletTestingCombo::class.java)

        val smartWalletExpirationManager = mockk<SmartWalletValidityManager>(relaxed = true).also {
            every { it.smartWalletValidityPivot } returns gson.fromJson<List<ApiSmartWalletValidityPivot>>(
                ResourcesHelper.readTestFileAsString(
                    "validity.json"
                ),
                object : TypeToken<List<ApiSmartWalletValidityPivot>>() {}.type,
            )?.flatMap(ApiSmartWalletValidityPivot::toSmartWalletValidityPivots).orEmpty()
        }

        val smartWalletEligibilityManager = mockk<SmartWalletEligibilityManager>(relaxed = true).also {
            every { it.smartWalletEligibilityPivot } returns gson.fromJson<List<ApiSmartWalletEligibilityPivot>>(
                ResourcesHelper.readTestFileAsString(
                    "eligibility.json"
                ),
                object : TypeToken<List<ApiSmartWalletEligibilityPivot>>() {}.type,
            )?.flatMap(ApiSmartWalletEligibilityPivot::toSmartWalletEligibilityPivots).orEmpty()
        }

        coEvery { blacklistDCCManager.isBlacklisted(any()) } returns false

        validityUseCase = ComputeDccValidityUseCase(robertManager, smartWalletExpirationManager)
        eligibilityUseCase = ComputeDccEligibilityUseCase(robertManager, smartWalletEligibilityManager)
        getSmartWalletStateUseCase = GetSmartWalletStateUseCase(validityUseCase, eligibilityUseCase, robertManager)
        getSmartWalletMapUseCase = GetSmartWalletMapUseCase(
            walletRepository,
            blacklistDCCManager,
            getSmartWalletStateUseCase,
        )

        mockkStatic(GreenCertificate::isRecoveryOrTestPositive)
    }

    @Test
    fun computeDccExpirationUseCase_run_testing_data() {
        every { robertManager.configuration.smartWalletVacc } returns SmartWalletVacc(
            ar = testData.config.vaccineProduct.ar,
            ja = testData.config.vaccineProduct.ja,
            az = testData.config.vaccineProduct.az,
        )

        val testTotal = testData.sections.sumOf { it.tests.size }
        var testCount = 1

        testData.sections.forEach { (section, tests) ->
            println(section)

            tests.forEach { test ->
                println("• ${testCount++}/$testTotal - ${test.desc}")

                when (test.input.type) {
                    "v" -> {
                        test.input.products.forEach { product ->
                            test.input.doses.forEach { dose ->
                                println("\tRun vaccination with $product & $dose")
                                executeTest(test, product, null, dose, "params = $product & $dose")
                            }
                        }
                    }
                    "r" -> {
                        test.input.prefixes.forEach { prefix ->
                            println("\tRun recovery with $prefix")
                            executeTest(test, null, prefix, null, "params = $prefix")
                        }
                    }
                    "p" -> {
                        test.input.prefixes.forEach { prefix ->
                            println("\tRun positive test with $prefix")
                            executeTest(test, null, prefix, null, "params = $prefix")
                        }
                    }
                    else -> throw(Exception("Unknown type ${test.input.type}"))
                }
            }
        }
    }

    private fun executeTest(
        test: SmartWalletTesting.Section.Test,
        product: String?,
        prefix: String?,
        dose: SmartWalletTesting.Section.Test.Input.Dose?,
        errorText: String,
    ) {

        val dcc = mockk<EuropeanCertificate>(relaxed = true)

        every { dcc.greenCertificate.dateOfBirth } returns test.input.dob
        every { dcc.greenCertificate.vaccinations } returns listOf(
            mockk<Vaccination>(relaxed = true).also { vaccination ->
                every { vaccination.dateOfVaccination } returns test.input.doi
                if (product != null) {
                    every { vaccination.medicinalProduct } returns product
                }
                if (dose != null) {
                    every { vaccination.doseNumber } returns dose.c
                    every { vaccination.totalSeriesOfDoses } returns dose.t
                }
            }
        )
        every { dcc.greenCertificate.recoveryStatements } returns listOf(
            mockk<RecoveryStatement>(relaxed = true).also { recoveryStatement ->
                every { recoveryStatement.dateOfFirstPositiveTest } returns test.input.doi
                if (prefix != null) {
                    every { recoveryStatement.certificateIssuer } returns prefix
                }
            }
        )
        every { dcc.greenCertificate.tests } returns listOf(
            mockk<dgca.verifier.app.decoder.model.Test>(relaxed = true).also { positiveTest ->
                every { positiveTest.dateTimeOfCollection } returns test.input.doi
                if (prefix != null) {
                    every { positiveTest.testingCentre } returns prefix
                }
            }
        )
        every { dcc.type } returns when (test.input.type) {
            "v" -> WalletCertificateType.VACCINATION_EUROPE
            "r" -> WalletCertificateType.RECOVERY_EUROPE
            "p" -> WalletCertificateType.SANITARY_EUROPE
            else -> throw(Exception("Unknown type ${test.input.type}"))
        }

        val yearMonthDayUsParser = yearMonthDayUsParser()
        val actualExp = validityUseCase(dcc, yearMonthDayUsParser.parse(test.input.today)!!)
        val actualElg = eligibilityUseCase(dcc, yearMonthDayUsParser.parse(test.input.today)!!)

        val expectedExpStart = test.output.start?.let(yearMonthDayUsParser::parse)
        val expectedExpEnd = test.output.exp?.let(yearMonthDayUsParser::parse)
        val expectedElg = test.output.elg?.let(yearMonthDayUsParser::parse)

        assertEquals(expectedExpStart, actualExp?.start, "validity start -> $errorText")
        assertEquals(expectedExpEnd, actualExp?.end, "validity end -> $errorText")
        assertEquals(expectedElg, actualElg, "eligibility -> $errorText")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getSmartWalletCertificateUseCase_run_testing_combo_data() {
        val dateFormat = yearMonthDayUsParser()

        every { robertManager.configuration.smartWalletVacc } returns SmartWalletVacc(
            ar = comboTestData.config.vaccineProduct.ar,
            ja = comboTestData.config.vaccineProduct.ja,
            az = comboTestData.config.vaccineProduct.az,
        )

        val testTotal = comboTestData.sections.sumOf { it.tests.size }
        var testCount = 1

        comboTestData.sections.forEach { (section, tests) ->
            println(section)

            tests.forEach { test ->
                val certificateMatrix = test.inputs.map { input ->
                    when (input.type) {
                        "v" -> {
                            input.products.flatMap { product ->
                                input.doses.map { dose ->
                                    mockk<EuropeanCertificate>(relaxed = true).also { dcc ->
                                        every { dcc.greenCertificate.dateOfBirth } returns test.dob
                                        every { dcc.type } returns WalletCertificateType.VACCINATION_EUROPE
                                        every { dcc.expirationTime } returns Long.MAX_VALUE
                                        every { dcc.greenCertificate.vaccinations } returns listOf(
                                            mockk<Vaccination>(relaxed = true).also { vaccination ->
                                                every { vaccination.dateOfVaccination } returns input.doi
                                                every { vaccination.medicinalProduct } returns product
                                                every { vaccination.doseNumber } returns dose.c
                                                every { vaccination.totalSeriesOfDoses } returns dose.t
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        "r" -> {
                            input.prefixes.map { prefix ->
                                mockk<EuropeanCertificate>(relaxed = true).also { dcc ->
                                    every { dcc.greenCertificate.dateOfBirth } returns test.dob
                                    every { dcc.type } returns WalletCertificateType.RECOVERY_EUROPE
                                    every { dcc.expirationTime } returns Long.MAX_VALUE
                                    every { dcc.greenCertificate.isRecoveryOrTestPositive } returns true
                                    every { dcc.greenCertificate.recoveryStatements } returns listOf(
                                        mockk<RecoveryStatement>(relaxed = true).also { recoveryStatement ->
                                            every { recoveryStatement.dateOfFirstPositiveTest } returns input.doi
                                            every { recoveryStatement.certificateIssuer } returns prefix
                                        }
                                    )
                                }
                            }
                        }
                        "p" -> {
                            input.prefixes.map { prefix ->
                                mockk<EuropeanCertificate>(relaxed = true).also { dcc ->
                                    every { dcc.greenCertificate.dateOfBirth } returns test.dob
                                    every { dcc.type } returns WalletCertificateType.SANITARY_EUROPE
                                    every { dcc.expirationTime } returns Long.MAX_VALUE
                                    every { dcc.greenCertificate.isRecoveryOrTestPositive } returns true
                                    every { dcc.greenCertificate.tests } returns listOf(
                                        mockk<dgca.verifier.app.decoder.model.Test>(relaxed = true).also { positiveTest ->
                                            every { positiveTest.dateTimeOfCollection } returns input.doi
                                            every { positiveTest.testingCentre } returns prefix
                                        }
                                    )
                                }
                            }
                        }
                        else -> throw(Exception("Unknown type ${input.type}"))
                    }.onEach {
                        every { it.id } returns input.id.toString()
                    }
                }

                val testData = certificateMatrix.combine()

                println("• ${testCount++}/$testTotal - ${test.desc} - ${testData.size} combinations")

                val nowDate = dateFormat.parse(test.today)!!
                testData.forEach { dcc ->
                    println("\tRun with\n\t\t- ${dcc.joinToString("\n\t\t- ") { it.print(dateFormat) }}")
                    every { walletRepository.walletCertificateFlow } returns MutableStateFlow(TacResult.Success(dcc))

                    val flow = getSmartWalletMapUseCase(nowDate)
                    runTest {
                        val electedDcc = flow.first().values.firstOrNull()
                        val actualElg = electedDcc?.let { eligibilityUseCase(it, nowDate) }
                        val actualId = electedDcc?.id?.toInt()

                        val expectedElg = test.output.elg?.let(dateFormat::parse)
                        val expectedId = test.output.id

                        println("\t-> Elected ${electedDcc?.print(dateFormat)}\n")

                        assertEquals(expectedElg, actualElg)
                        assertEquals(expectedId, actualId)
                    }
                }
            }
        }
    }

    private fun EuropeanCertificate.print(dateFormat: DateFormat): String {
        val sb = StringBuilder()
        sb.append("#$id : ")
        sb.append(type)
        sb.append(" - ")
        sb.append("${(greenCertificate.recoveryDateOfFirstPositiveTest ?: greenCertificate.vaccineDate)?.let(dateFormat::format)}")
        sb.append(" - ")
        when (type) {
            WalletCertificateType.VACCINATION_EUROPE -> {
                sb.append("product(${greenCertificate.vaccineMedicinalProduct})")
                sb.append(" - ")
                sb.append("dose${greenCertificate.vaccineDose}")
            }
            WalletCertificateType.SANITARY_EUROPE -> {
                sb.append("testCentre(${greenCertificate.tests?.firstOrNull()?.testingCentre.orEmpty()})")
            }
            WalletCertificateType.RECOVERY_EUROPE -> {
                sb.append("certificateIssuer(${greenCertificate.recoveryStatements?.firstOrNull()?.certificateIssuer.orEmpty()})")
            }
        }
        return sb.toString()
    }

    // https://stackoverflow.com/a/56178470/10935947
    private fun <T> List<List<T>>.combine(
        partial: List<T> = listOf(),
    ): List<List<T>> {
        if (isEmpty()) {
            // recursive base case: lists is now empty, so partial
            // is complete, so return it in an enclosing array
            return listOf(partial)
        } else {
            // make lists mutable so that we can remove the first sub-array
            val mutableLists = toMutableList()
            // remove the first sub-array from lists which is now shorter
            val first = mutableLists.removeFirst()
            // create an array to hold all of the combinations
            val result = mutableListOf<List<T>>()
            // take each element from the first sub-array, append it to
            // the partial result, and call combine to continue the
            // process. Take the results returned from combine and append
            // those to the result array.
            for (n in first) {
                result += mutableLists.combine(partial + listOf(n))
            }
            // Return the results
            return result
        }
    }
}