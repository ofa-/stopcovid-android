/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.registerToAppBarLayoutForLiftOnScroll
import com.lunabeestudio.stopcovid.coreui.fragment.FastAdapterFragment
import com.lunabeestudio.stopcovid.databinding.ActivityMainBinding

abstract class MainFragment : FastAdapterFragment() {

    abstract fun getTitleKey(): String

    protected fun getActivityBinding(): ActivityMainBinding = (activity as MainActivity).binding

    override fun getAppBarLayout(): AppBarLayout? = getActivityBinding().appBarLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.recyclerView?.registerToAppBarLayoutForLiftOnScroll(getActivityBinding().appBarLayout)
    }

    override fun refreshScreen() {
        super.refreshScreen()
        (activity as AppCompatActivity).supportActionBar?.title = strings[getTitleKey()]
    }

    protected fun showSnackBar(message: String) {
        (activity as? MainActivity)?.showSnackBar(message)
    }

    protected fun showErrorSnackBar(message: String) {
        (activity as? MainActivity)?.showErrorSnackBar(message)
    }
}