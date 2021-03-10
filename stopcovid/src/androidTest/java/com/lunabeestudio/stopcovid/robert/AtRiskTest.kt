/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/02/03 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.robert

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.model.AtRiskStatus
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.support.robert.SupportRobertManager
import org.junit.After
import org.junit.Before
import org.junit.Test

class AtRiskTest {

    private fun Context.robertManager(): SupportRobertManager = (applicationContext as RobertApplication).robertManager as SupportRobertManager

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        context.robertManager().eraseRemoteAlert()
    }

    @After
    fun tearDown() {
        context.robertManager().eraseRemoteAlert()
    }

    @Test
    fun failure_don_t_change_status() {
        assert(context.robertManager().atRiskStatus == null)
        context.robertManager().processStatusResults(
            RobertResultData.Failure(),
            RobertResultData.Failure()
        )
        assert(context.robertManager().atRiskStatus == null)
        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Failure()
        )
        assert(context.robertManager().atRiskStatus == null)
        context.robertManager().processStatusResults(
            RobertResultData.Failure(),
            RobertResultData.Success(AtRiskStatus(0f, null, null))
        )
        assert(context.robertManager().atRiskStatus == null)
    }

    @Test
    fun simple_status_case() {
        assert(context.robertManager().atRiskStatus == null)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(0f, null, null))
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 0f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == null)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == null)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(1f, null, 1L)),
            RobertResultData.Success(AtRiskStatus(0f, null, null))
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 1f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == null)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 1L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(1f, 0L, 2L)),
            RobertResultData.Success(AtRiskStatus(0f, null, null))
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 1f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 0L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 2L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(4f, -1L, 3L)),
            RobertResultData.Success(AtRiskStatus(0f, null, null))
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 1f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 0L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 2L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(1f, 0L, 1L)),
            RobertResultData.Success(AtRiskStatus(0f, null, null))
        )
        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(4f, 2L, 4L)),
            RobertResultData.Success(AtRiskStatus(0f, null, null))
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 4f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 2L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 4L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(1f, 3L, 5L)),
            RobertResultData.Success(AtRiskStatus(0f, null, null))
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 1f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 3L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 5L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(0f, null, null))
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 0f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == null)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == null)
    }

    @Test
    fun simple_warning_status_case() {
        assert(context.robertManager().atRiskStatus == null)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 0f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == null)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == null)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(1f, null, 1L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 1f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == null)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 1L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(1f, 0L, 2L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 1f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 0L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 2L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(4f, -1L, 3L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 1f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 0L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 2L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(1f, 0L, 1L)),
        )
        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(4f, 2L, 4L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 4f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 2L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 4L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(1f, 3L, 5L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 1f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 3L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 5L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 0f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == null)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == null)
    }

    @Test
    fun mixed_status_scenario_1() {
        assert(context.robertManager().atRiskStatus == null)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 0f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == null)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == null)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(3f, 1L, 1L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 3f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 1L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 1L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(4f, 2L, 2L)),
            RobertResultData.Success(AtRiskStatus(3f, 1L, 1L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 4f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 2L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 2L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(4f, 2L, 2L)),
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 4f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 2L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 2L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(1f, 3L, 3L)),
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 1f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 3L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 3L)
    }

    @Test
    fun mixed_status_scenario_2() {
        assert(context.robertManager().atRiskStatus == null)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 0f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == null)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == null)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(4f, 1L, 1L)),
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 4f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 1L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 1L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(4f, 1L, 1L)),
            RobertResultData.Success(AtRiskStatus(3f, 2L, 2L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 4f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 1L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 1L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(1f, 3L, 3L)),
            RobertResultData.Success(AtRiskStatus(3f, 2L, 2L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 3f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 2L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 2L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(1f, 3L, 3L)),
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 1f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 3L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 3L)
    }

    @Test
    fun mixed_status_scenario_3() {
        assert(context.robertManager().atRiskStatus == null)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 0f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == null)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == null)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(3f, 1L, 1L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 3f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 1L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 1L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(4f, 2L, 2L)),
            RobertResultData.Success(AtRiskStatus(3f, 1L, 1L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 4f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 2L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 2L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(1f, 3L, 3L)),
            RobertResultData.Success(AtRiskStatus(3f, 1L, 1L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 3f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 1L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 1L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(1f, 3L, 3L)),
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 1f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 3L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 3L)
    }

    @Test
    fun mixed_status_scenario_4() {
        assert(context.robertManager().atRiskStatus == null)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(4f, 1L, 1L)),
            RobertResultData.Success(AtRiskStatus(1f, 2L, 2L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 4f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 1L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 1L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(4f, 1L, 1L)),
            RobertResultData.Success(AtRiskStatus(1f, 2L, 2L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 4f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 1L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 1L)
    }

    @Test
    fun mixed_status_scenario_5() {
        assert(context.robertManager().atRiskStatus == null)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(0f, null, null)),
            RobertResultData.Success(AtRiskStatus(2f, 2L, 1L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 2f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 2L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 1L)

        context.robertManager().processStatusResults(
            RobertResultData.Success(AtRiskStatus(4f, 1L, 2L)),
            RobertResultData.Success(AtRiskStatus(2f, 2L, 1L)),
        )
        assert(context.robertManager().atRiskStatus?.riskLevel == 4f)
        assert(context.robertManager().atRiskStatus?.ntpLastRiskScoringS == 1L)
        assert(context.robertManager().atRiskStatus?.ntpLastContactS == 2L)
    }
}