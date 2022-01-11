/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import com.google.android.material.appbar.AppBarLayout
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.framework.manager.DebugManager
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.registerToAppBarLayoutForLiftOnScroll
import com.lunabeestudio.stopcovid.coreui.fragment.FastAdapterFragment
import com.lunabeestudio.stopcovid.coreui.manager.CalibrationManager
import com.lunabeestudio.stopcovid.coreui.manager.ConfigManager
import com.lunabeestudio.stopcovid.databinding.ActivityMainBinding
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.manager.FormManager
import com.lunabeestudio.stopcovid.manager.InfoCenterManager
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.LinksManager
import com.lunabeestudio.stopcovid.manager.MoreKeyFiguresManager
import com.lunabeestudio.stopcovid.manager.PrivacyManager
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.manager.Blacklist2DDOCManager
import com.lunabeestudio.stopcovid.manager.RisksLevelManager
import com.lunabeestudio.stopcovid.manager.VaccinationCenterManager
import com.lunabeestudio.stopcovid.repository.AttestationRepository
import com.lunabeestudio.stopcovid.repository.VenueRepository
import com.lunabeestudio.stopcovid.repository.WalletRepository
import com.lunabeestudio.stopcovid.utils.lazyFast

abstract class MainFragment : FastAdapterFragment() {
    abstract fun getTitleKey(): String

    protected fun getActivityBinding(): ActivityMainBinding? = (activity as? MainActivity)?.binding

    override fun getAppBarLayout(): AppBarLayout? = getActivityBinding()?.appBarLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if ((activity as? MainActivity)?.binding?.tabLayout?.isVisible == true) {
            postponeEnterTransition()
            (activity as? MainActivity)?.binding?.appBarLayout?.doOnNextLayout {
                startPostponedEnterTransition()
            }
            (activity as? MainActivity)?.binding?.tabLayout?.isVisible = false
        }
        getActivityBinding()?.appBarLayout?.let { appBarLayout ->
            binding?.recyclerView?.registerToAppBarLayoutForLiftOnScroll(appBarLayout)
        }
    }

    override fun refreshScreen() {
        setTitle()
        super.refreshScreen()
    }

    protected open fun setTitle() {
        appCompatActivity?.supportActionBar?.title = strings[getTitleKey()]
    }

    protected fun showSnackBar(message: String) {
        (activity as? MainActivity)?.showSnackBar(message)
    }

    protected val keyFiguresManager: KeyFiguresManager by lazyFast {
        injectionContainer.keyFiguresManager
    }
    protected val vaccinationCenterManager: VaccinationCenterManager by lazyFast {
        injectionContainer.vaccinationCenterManager
    }
    protected val risksLevelManager: RisksLevelManager by lazyFast {
        injectionContainer.risksLevelManager
    }
    protected val infoCenterManager: InfoCenterManager by lazyFast {
        injectionContainer.infoCenterManager
    }
    protected val linksManager: LinksManager by lazyFast {
        injectionContainer.linksManager
    }
    protected val moreKeyFiguresManager: MoreKeyFiguresManager by lazyFast {
        injectionContainer.moreKeyFiguresManager
    }
    protected val formManager: FormManager by lazyFast {
        injectionContainer.formManager
    }
    protected val privacyManager: PrivacyManager by lazyFast {
        injectionContainer.privacyManager
    }
    protected val blacklistDCCManager: BlacklistDCCManager by lazyFast {
        injectionContainer.blacklistDCCManager
    }
    protected val blacklist2DDOCManager: Blacklist2DDOCManager by lazyFast {
        injectionContainer.blacklist2DDOCManager
    }
    protected val analyticsManager: AnalyticsManager by lazyFast {
        injectionContainer.analyticsManager
    }
    protected val attestationRepository: AttestationRepository by lazyFast {
        injectionContainer.attestationRepository
    }
    protected val venueRepository: VenueRepository by lazyFast {
        injectionContainer.venueRepository
    }
    protected val walletRepository: WalletRepository by lazyFast {
        injectionContainer.walletRepository
    }
    protected val debugManager: DebugManager by lazyFast {
        injectionContainer.debugManager
    }
    protected val configManager: ConfigManager by lazyFast {
        injectionContainer.configManager
    }
    protected val calibrationManager: CalibrationManager by lazyFast {
        injectionContainer.calibrationManager
    }
}