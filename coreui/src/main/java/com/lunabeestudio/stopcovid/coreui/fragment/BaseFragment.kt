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

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import timber.log.Timber
import java.util.IllegalFormatException

abstract class BaseFragment : Fragment() {

    abstract fun refreshScreen()

    protected var strings: HashMap<String, String> = StringsManager.getStrings()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        StringsManager.strings.observe(viewLifecycleOwner, Observer { strings: HashMap<String, String> ->
            this.strings = strings
            refreshScreen()
        })
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
}