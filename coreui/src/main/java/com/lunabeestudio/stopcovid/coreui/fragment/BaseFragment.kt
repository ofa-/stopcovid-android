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

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager

abstract class BaseFragment : Fragment() {

    abstract fun refreshScreen()

    val strings: HashMap<String, String>
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
}
