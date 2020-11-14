/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.view.Gravity
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.lunabeestudio.stopcovid.BuildConfig
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class AboutFragment : MainFragment() {

    override fun getTitleKey(): String = "aboutController.title"

    companion object {
        const val releasesUri = "https://github.com/ofa-/stopcovid-android/releases"
        const val downloadUri = "$releasesUri/latest/download/stopcovid-release.apk"
    }

    private fun isLatest() : Boolean {
        "$releasesUri/latest".run {
            return (OkHttpClient.Builder().build()
                .newCall(Request.Builder().url(this).build())
                .execute()
                .body?.string() ?: "")
                .contains("commit/" + BuildConfig.BUILD_ID)
        }
    }

    private fun toast(text: String?) {
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
                    toast(strings["aboutController.toast.alreadyUpToDate"])
                } else {
                    toast(strings["aboutController.toast.downloadingLatest"])
                    Downloader(requireContext())
                        .fetch()
                }
            }.onFailure {
                toast(strings["aboutController.toast.errorFetchingUpdates"])
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
                releasesUri.openInExternalBrowser(requireContext())
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
            iconRes = R.drawable.ic_web
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

    class Downloader(
        private val context: Context,
        private var uri: Uri = Uri.parse(downloadUri)
    ) {

        private val downloadManager = context.getSystemService(
            Context.DOWNLOAD_SERVICE
        ) as DownloadManager

        private var file = destinationFile(uri)

        fun fetch() {
            autoDeleteFile()
            receiver.register()
            Request(uri)
                .setDescription("💕 🐱  🐭 💋")
                .setDestinationUri(file.toUri())
                .setNotificationVisibility(
                    DownloadManager.Request
                        .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .also {
                    receiver.id =
                        downloadManager.enqueue(it)
                }
        }

        private fun onComplete() {
            val uri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                file
            )
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .also {
                    context.startActivity(it)
                }
        }

        private val receiver = object : BroadcastReceiver() {

            fun register() {
                context.registerReceiver(
                    this,
                    IntentFilter(ACTION_DOWNLOAD_COMPLETE)
                )
            }

            fun unregister() {
                context.unregisterReceiver(this)
            }

            var id = -2L
            override fun onReceive(context: Context?, intent: Intent?) {
                if (id != intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1))
                    return
                unregister()
                if (! downloadOk())
                    return
                onComplete()
            }
        }

        private fun downloadOk(): Boolean {
            downloadManager.query(Query().setFilterById(receiver.id)).run {
                return moveToFirst() && getInt(
                    getColumnIndex(COLUMN_STATUS)
                ) == STATUS_SUCCESSFUL
            }
        }

        private fun destinationFile(uri: Uri): File {
            return File(
                context
                    .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                uri.lastPathSegment.toString()
            )
        }

        fun autoDeleteFile(): Boolean {
            return file.let { it.exists() && it.delete() }
        }
    }
}