/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.activity

import android.content.Intent
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.facebook.testing.screenshot.Screenshot
import com.google.gson.Gson
import com.lunabeestudio.stopcovid.model.Info
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4ClassRunner::class)
class AppMaintenanceActivityTest {

    @Rule
    @JvmField
    var activityTestRule: ActivityTestRule<AppMaintenanceActivity> = object : ActivityTestRule<AppMaintenanceActivity>(
        AppMaintenanceActivity::class.java) {
        override fun getActivityIntent(): Intent {
            val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
            val info = Info(JSONObject("{\n" +
                "    \"iOS\": {\n" +
                "     \"isActive\": false,\n" +
                "     \"mode\": \"maintenance\",\n" +
                "     \"minRequiredBuildNumber\": 1,\n" +
                "     \"message\": {\n" +
                "              \"fr\":\"Votre application StopCovid est actuellement en maintenance.\\nNos équipes sont mobilisées pour que vous puissiez rapidement profiter à nouveau de votre application\"\n" +
                "     }\n" +
                " },\n" +
                "    \"Android\": {\n" +
                "          \"isActive\": true,\n" +
                "          \"mode\": \"maintenance\",\n" +
                "          \"minRequiredBuildNumber\": 1,\n" +
                "          \"message\": {\n" +
                "              \"fr\":\"Votre application StopCovid est actuellement en maintenance.\\nNos équipes sont mobilisées pour que vous puissiez rapidement profiter à nouveau de votre application\"\n" +
                "          }\n" +
                "      }\n" +
                "}"))
            val gson = Gson()
            return Intent(targetContext, AppMaintenanceActivity::class.java).apply {
                putExtra(AppMaintenanceActivity.EXTRA_INFO, gson.toJson(info))
            }
        }
    }

    @Test
    fun maintenanceTest() {
        Screenshot.snapActivity(activityTestRule.activity).setName("maintenance").record()
    }
}
