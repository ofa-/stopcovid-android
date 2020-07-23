/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.Toast
import timber.log.Timber
import java.util.regex.Pattern

fun String.openInChromeTab(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(this)
    val activityInfo: ActivityInfo? = intent.resolveActivityInfo(context.packageManager, intent.flags)
    if (activityInfo?.exported == true) {
        context.startActivity(intent)
    } else {
        Timber.e("No activity to open url")
        Toast.makeText(context, "Unable to open url", Toast.LENGTH_SHORT).show()
    }
}

fun String.isCodeValid(): Boolean {
    return length == 6 || Pattern.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", this)
}