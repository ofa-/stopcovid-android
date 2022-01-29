/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/21 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener

fun FragmentManager.onFirstFragmentAttached(block: (FragmentManager, Fragment) -> Unit?) {
    val fragment = fragments.firstOrNull()
    if (fragment != null) {
        block(this, fragment)
    } else {
        addFragmentOnAttachListener(object : FragmentOnAttachListener {
            override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
                block(fragmentManager, fragment)
                removeFragmentOnAttachListener(this)
            }
        })
    }
}