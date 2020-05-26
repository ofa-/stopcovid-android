/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.extension

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager

/**
 *  Forcibly show the keyboard in a activity, it will be not be closed until the user explicitly do so.
 *
 *  @receiver The activity where the keyboard will be shown.
 */
fun Activity.showSoftKeyBoard() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY)
}

/**
 *  Hide the keyboard if it was already shown.
 *  ## To hide the keyboard you need to have a focusable view in your layout
 *
 *  @receiver The activity where the keyboard will be hidden.
 */
fun Activity.hideSoftKeyBoard() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    var view = currentFocus
    //If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
        view = (findViewById(android.R.id.content) as? ViewGroup)?.rootView
    }
    imm.hideSoftInputFromWindow(view?.windowToken, 0)
}