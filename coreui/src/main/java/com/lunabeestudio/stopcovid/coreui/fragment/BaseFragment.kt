/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.fragment

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.fragment.app.Fragment
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import timber.log.Timber
import java.util.IllegalFormatException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.days
import kotlin.time.milliseconds
import kotlin.time.minutes

abstract class BaseFragment : Fragment() {

    abstract fun refreshScreen()

    protected var strings = StringsManager.strings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        StringsManager.liveStrings.observe(viewLifecycleOwner) { strings ->
            if (this.strings != strings) {
                this.strings = strings
                refreshScreen()
            }
        }
    }

    protected fun stringsFormat(key: String, vararg args: Any?): String? {
        return strings[key]?.let {
            try {
                String.format(it, *args)
            } catch (e: IllegalFormatException) {
                Timber.e(e)
                it
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    protected fun Duration.getRelativeDateTimeString(context: Context): String? {
        val now = System.currentTimeMillis().milliseconds

        return when {
            now - this <= 1.minutes -> strings["common.justNow"]
            now - this <= 1.days -> DateUtils.getRelativeTimeSpanString(
                    this.coerceAtMost(now - 1.minutes).toLongMilliseconds(),
                    now.toLongMilliseconds(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH,
            ).toString()
            else -> DateUtils.getRelativeDateTimeString(
                    context,
                    this.toLongMilliseconds(),
                    DateUtils.DAY_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH,
            ).toString()
        }
    }
}
