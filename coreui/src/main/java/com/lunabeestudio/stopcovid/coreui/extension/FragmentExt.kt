/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.extension

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import timber.log.Timber

fun Fragment.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.fromParts("package", requireActivity().packageName, null)
    startActivity(intent)
}

/**
 * Find a [NavController] given a [Fragment]
 *
 * Calling this on a Fragment that is not a [NavHostFragment] or within a [NavHostFragment]
 * will return null.
 */
fun Fragment.findNavControllerOrNull(): NavController? =
    try {
        NavHostFragment.findNavController(this)
    } catch (e: IllegalStateException) {
        Timber.e(e, "Failed to find the NavController")
        null
    }

fun Fragment.viewLifecycleOwnerOrNull(): LifecycleOwner? =
    try {
        viewLifecycleOwner
    } catch (e: IllegalStateException) {
        Timber.e(e, "Failed to get lifecycle owner")
        null
    }

val Fragment.appCompatActivity: AppCompatActivity?
    get() = activity as? AppCompatActivity