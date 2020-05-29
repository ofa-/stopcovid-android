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

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.facebook.testing.screenshot.Screenshot
import com.lunabeestudio.stopcovid.R
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4ClassRunner::class)
class MainActivityTest {

    @Rule
    @JvmField
    var activityTestRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)

    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA,
        android.Manifest.permission.ACCESS_COARSE_LOCATION)

    @Test
    fun mainActivityTest() {
        Screenshot.snapActivity(activityTestRule.activity).setName("proximity").record()
        onView(allOf(withId(R.id.reportFragment), isDisplayed())).perform(click())
        Screenshot.snapActivity(activityTestRule.activity).setName("sick").record()
        onView(allOf(withId(R.id.sharingFragment), isDisplayed())).perform(click())
        Screenshot.snapActivity(activityTestRule.activity).setName("sharing").record()
    }

    @Test
    fun privacyTest() {
        onView(allOf(withId(R.id.recycler_view)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(8, click()))
        Screenshot.snapActivity(activityTestRule.activity).setName("privacy").record()
    }

    @Test
    fun manageDataTest() {
        onView(allOf(withId(R.id.recycler_view)))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(6, click()))
        Screenshot.snapActivity(activityTestRule.activity).setName("manageData").record()
    }

    @Test
    fun enterCodeManualTest() {
        onView(allOf(withId(R.id.reportFragment), isDisplayed())).perform(click())
        onView(withId(R.id.button2)).perform(click())
        Screenshot.snapActivity(activityTestRule.activity).setName("enterCode").record()
        onView(allOf(withId(R.id.editText), isDisplayed()))
            .perform(ViewActions.replaceText("123456"), ViewActions.closeSoftKeyboard())
        onView(withId(R.id.button)).perform(click())
        Screenshot.snapActivity(activityTestRule.activity).setName("symptomsOrigin").record()
        onView(withId(R.id.recycler_view))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(8, click()))
        Screenshot.snapActivity(activityTestRule.activity).setName("sendHistory").record()
        onView(withId(R.id.button)).perform(click())
        Thread.sleep(TimeUnit.SECONDS.toMillis(5L))
        Screenshot.snapActivity(activityTestRule.activity).setName("testedSick").record()
    }

    @Test
    fun scanCodeTest() {
        onView(allOf(withId(R.id.reportFragment), isDisplayed())).perform(click())
        onView(withId(R.id.button1)).perform(click())
        Screenshot.snapActivity(activityTestRule.activity).setName("scanCode").record()
    }

    @Test
    fun aboutTest() {
        onView(allOf(withId(R.id.item_text), isDisplayed())).perform(click())
        Screenshot.snapActivity(activityTestRule.activity).setName("about").record()
    }

    @After
    fun after() {
        activityTestRule.activity.getSharedPreferences("robert_prefs", Context.MODE_PRIVATE).edit {
            clear()
        }
        PreferenceManager.getDefaultSharedPreferences(activityTestRule.activity).edit {
            clear()
        }
    }
}
