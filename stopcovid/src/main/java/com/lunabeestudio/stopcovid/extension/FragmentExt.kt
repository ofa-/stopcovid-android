/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/26/8 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.createViewModelLazy
import androidx.lifecycle.ViewModelStore
import androidx.navigation.fragment.findNavController
import com.lunabeestudio.stopcovid.InjectionContainer
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.findParentFragmentByType
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory

val Fragment.injectionContainer: InjectionContainer
    get() = requireActivity().injectionContainer

@MainThread
inline fun <reified T : Fragment> Fragment.navGraphWalletViewModels(
    noinline factoryProducer: (() -> WalletViewModelFactory)
): Lazy<WalletViewModel> {
    val backStackEntry by lazy {
        findParentFragmentByType<T>()!!.findNavController().getBackStackEntry(R.id.nav_wallet)
    }
    val storeProducer: () -> ViewModelStore = {
        backStackEntry.viewModelStore
    }
    return createViewModelLazy(
        WalletViewModel::class, storeProducer,
        {
            factoryProducer()
        }
    )
}

fun BaseFragment.showErrorSnackBar(message: String) {
    (activity as? MainActivity)?.showErrorSnackBar(message)
}