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

/**
 * Run code when the first fragment of type [T] is attached to the [FragmentManager] receiver or run it synchronously if a fragment of type
 * [T] is already attached.
 *
 * @param block The block to execute on fragment attached
 */
inline fun <reified T : Fragment> FragmentManager.doOnFragmentAttached(crossinline block: (FragmentManager, T) -> Unit?) {
    val fragment = fragments.filterIsInstance<T>().firstOrNull()
    if (fragment != null) {
        block(this, fragment)
    } else {
        addFragmentOnAttachListener(object : FragmentOnAttachListener {
            override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
                if (fragment is T) {
                    block(fragmentManager, fragment)
                    removeFragmentOnAttachListener(this)
                }
            }
        })
    }
}