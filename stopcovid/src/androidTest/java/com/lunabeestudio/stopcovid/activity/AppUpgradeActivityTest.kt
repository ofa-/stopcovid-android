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
class AppUpgradeActivityTest {

    @Rule
    @JvmField
    var activityTestRule: ActivityTestRule<AppMaintenanceActivity> = object : ActivityTestRule<AppMaintenanceActivity>(
        AppMaintenanceActivity::class.java) {
        override fun getActivityIntent(): Intent {
            val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
            val info = Info(JSONObject("{\n" +
                "    \"iOS\": {\n" +
                "        \"isActive\": true,\n" +
                "        \"mode\": \"upgrade\",\n" +
                "        \"minRequiredBuildNumber\": 949,\n" +
                "        \"buttonTitle\": {\n" +
                "            \"fr\": \"Télécharger la nouvelle version\"\n" +
                "        },\n" +
                "        \"buttonURL\": {\n" +
                "            \"fr\": \"https://beta.itunes.apple.com/v1/app/1489730268\"\n" +
                "        },\n" +
                "        \"message\": {\n" +
                "            \"fr\":\"Cher client, votre application a été mise à jour. Nous vous invitons à télécharger la nouvelle version directement sur TestFlight.\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"Android\": {\n" +
                "        \"isActive\": true,\n" +
                "        \"mode\": \"upgrade\",\n" +
                "        \"minRequiredBuildNumber\": 3,\n" +
                "        \"buttonTitle\": {\n" +
                "            \"fr\": \"Télécharger la nouvelle version\"\n" +
                "        },\n" +
                "        \"buttonURL\": {\n" +
                "            \"fr\": \"https://play.google.com/store/apps/details?id=com.stopcovid.android\"\n" +
                "        },\n" +
                "        \"message\": {\n" +
                "            \"fr\":\"Cher client, votre application a été mise à jour. Nous vous invitons à télécharger la nouvelle version directement sur le Play Store.\"\n" +
                "        }\n" +
                "    }\n" +
                "}"))
            val gson = Gson()
            return Intent(targetContext, AppMaintenanceActivity::class.java).apply {
                putExtra(AppMaintenanceActivity.EXTRA_INFO, gson.toJson(info))
            }
        }
    }

    @Test
    fun upgradeTest() {
        Screenshot.snapActivity(activityTestRule.activity).setName("upgrade").record()
    }
}
