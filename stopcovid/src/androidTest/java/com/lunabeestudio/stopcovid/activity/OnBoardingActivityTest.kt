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

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.facebook.testing.screenshot.Screenshot
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4ClassRunner::class)
class OnBoardingActivityTest {

    @Rule
    @JvmField
    var activityTestRule: ActivityTestRule<OnBoardingActivity> = ActivityTestRule(OnBoardingActivity::class.java)

    @Rule
    @JvmField
    var mainActivityTestRule: IntentsTestRule<MainActivity> = IntentsTestRule(MainActivity::class.java)

    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_COARSE_LOCATION)

    @Test
    fun onBoardingActivityTest() {
        Screenshot.snapActivity(activityTestRule.activity).setName("onBoarding_page1").record()
        onView(allOf(withId(R.id.bottomSheetButton), isDisplayed())).perform(click())
        Screenshot.snapActivity(activityTestRule.activity).setName("onBoarding_page2").record()
        onView(allOf(withId(R.id.bottomSheetButton), isDisplayed())).perform(click())
        Screenshot.snapActivity(activityTestRule.activity).setName("onBoarding_page3").record()
        onView(allOf(withId(R.id.bottomSheetButton), isDisplayed())).perform(click())
        Screenshot.snapActivity(activityTestRule.activity).setName("onBoarding_page4").record()
        onView(allOf(withId(R.id.bottomSheetButton), isDisplayed())).perform(click())
        onView(withId(android.R.id.button1)).perform(click())
        Screenshot.snapActivity(activityTestRule.activity).setName("onBoarding_page5").record()
        onView(allOf(withId(R.id.bottomSheetButton), isDisplayed())).perform(click())
        Screenshot.snapActivity(activityTestRule.activity).setName("onBoarding_page6").record()
        onView(allOf(withId(R.id.bottomSheetButton), isDisplayed())).perform(click())
        Screenshot.snapActivity(activityTestRule.activity).setName("onBoarding_page7").record()
        onView(allOf(withId(R.id.bottomSheetButton), isDisplayed())).perform(click())
        intended(hasComponent(MainActivity::class.java.name))
    }

    @After
    fun after() {
        PreferenceManager.getDefaultSharedPreferences(activityTestRule.activity).edit {
            remove(Constants.SharedPrefs.ON_BOARDING_DONE)
        }
    }
}
