/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.fragment

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.fragment.app.Fragment
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.days
import kotlin.time.milliseconds
import kotlin.time.minutes

abstract class BaseFragment : Fragment() {

    abstract fun refreshScreen()

    protected val strings: HashMap<String, String>
        get() = StringsManager.strings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        StringsManager.liveStrings.observe(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    protected fun stringsFormat(key: String, vararg args: Any?): String? {
        return strings.stringsFormat(key, *args)
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
            )
                .toString()
                .fixQuoteInString()
            else -> DateUtils.getRelativeDateTimeString(
                context,
                this.toLongMilliseconds(),
                DateUtils.DAY_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH,
            )
                .toString()
                .fixQuoteInString()
        }
    }

    @OptIn(ExperimentalTime::class)
    protected fun Duration.getRelativeDateString(): String? {
        return DateUtils.getRelativeTimeSpanString(
            this.toLongMilliseconds(),
            System.currentTimeMillis(),
            DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH)
            .toString()
    }

    // Dirty hack to fix strange display of quotes on some Android devices
    private fun String.fixQuoteInString(): String = replace("'", "")
}
