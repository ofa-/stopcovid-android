/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.view.Gravity
import android.widget.Toast
import com.lunabeestudio.stopcovid.BuildConfig
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.openInChromeTab
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class AboutFragment : MainFragment() {

    override fun getTitleKey(): String = "aboutController.title"

    private val releasesUri = "https://github.com/ofa-/stopcovid-android/releases"
    private val releaseApk = "stopcovid-release.apk"

    private fun isLatest() : Boolean {
        "$releasesUri/latest".run {
            return (OkHttpClient.Builder().build()
                .newCall(Request.Builder().url(this).build())
                .execute()
                .body?.string() ?: "")
                .contains("commit/" + BuildConfig.BUILD_ID)
        }
    }

    private fun toast(text: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).also {
                it.setGravity(Gravity.CENTER, 0, 0)
            }.show()
        }
    }

    private fun checkNewVersion() {
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                if (isLatest()) {
                    toast("💕  🐭   already latest   🐱  💋")
                } else {
                    toast("⏬ 🐱   downloading latest   🐭 ⏬")
                    delay(2000)
                    "$releasesUri/latest/download/$releaseApk"
                        .openInChromeTab(requireContext())
                }
            }.onFailure {
                toast("🐰  error fetching updates  🎃")
            }
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.home
            identifier = items.count().toLong()
        }
        items += titleItem {
            text = strings["app.name"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = stringsFormat("aboutController.appVersion", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString())
                .plus("\n\n(" + BuildConfig.BUILD_ID + ")")
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
            ripple = true
            onLongClick = {
                releasesUri.openInChromeTab(requireContext())
            }
            onClick = {
                checkNewVersion()
            }
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["aboutController.mainMessage.title"]
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = strings["aboutController.mainMessage.subtitle"]
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += dividerItem {
            identifier = items.count().toLong()
        }
        items += linkItem {
            iconRes = R.drawable.ic_about
            text = strings["aboutController.webpage"]
            url = strings["aboutController.webpageUrl"]
            identifier = items.size.toLong()
        }
        items += dividerItem {
            identifier = items.count().toLong()
        }
        items += linkItem {
            iconRes = R.drawable.ic_faq
            text = strings["aboutController.faq"]
            url = strings["aboutController.faqUrl"]
            identifier = items.size.toLong()
        }
        items += dividerItem {
            identifier = items.count().toLong()
        }
        items += linkItem {
            iconRes = R.drawable.ic_feedback
            text = strings["aboutController.opinion"]
            url = strings["aboutController.opinionUrl"]
            identifier = items.size.toLong()
        }

        return items
    }
}