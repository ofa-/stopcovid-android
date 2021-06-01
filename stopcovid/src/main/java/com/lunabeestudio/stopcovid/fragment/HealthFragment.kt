/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/29 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeMs
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.fixFormatter
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.coreui.model.CardTheme
import com.lunabeestudio.stopcovid.extension.getGradientBackground
import com.lunabeestudio.stopcovid.extension.getRelativeDateTimeString
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.hideRiskStatus
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.healthCardItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.manager.RisksLevelManager
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.ContactDateFormat
import com.lunabeestudio.stopcovid.model.LinkType
import com.lunabeestudio.stopcovid.model.RisksUILevelSectionLink
import com.lunabeestudio.stopcovid.viewmodel.HealthViewModel
import com.lunabeestudio.stopcovid.viewmodel.HealthViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.IllegalFormatException
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

class HealthFragment : TimeMainFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val viewModel: HealthViewModel by viewModels { HealthViewModelFactory(robertManager) }

    private val fromDateFormat: DateFormat = SimpleDateFormat("dd LLL", Locale.getDefault())
    private val toDateFormat: DateFormat = SimpleDateFormat("dd LLL yyyy", Locale.getDefault())
    private val longDateFormat: DateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG)

    override fun getTitleKey(): String = "myHealthController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModelObserver()
    }

    private fun initViewModelObserver() {
        viewModel.loadingInProgress.observe(viewLifecycleOwner) { loadingInProgress ->
            (activity as? MainActivity)?.showProgress(loadingInProgress)
        }
        viewModel.eraseNotificationSuccess.observe(viewLifecycleOwner) {
            refreshScreen()
        }
        viewModel.covidException.observe(viewLifecycleOwner) { covidException ->
            showErrorSnackBar(covidException.getString(strings))
        }
        robertManager.liveAtRiskStatus.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        robertManager.liveUpdatingRiskStatus.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    override fun getItems(): List<GenericItem> {
        return RisksLevelManager.getCurrentLevel(robertManager.atRiskStatus?.riskLevel)?.let {
            registeredItems()
        } ?: notRegisteredItems()
    }

    private fun notRegisteredItems(): ArrayList<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.diagnosis
            identifier = items.count().toLong()
        }

        items += titleItem {
            text = strings["myHealthController.notRegistered.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["myHealthController.notRegistered.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }

        return items
    }

    @OptIn(ExperimentalTime::class)
    private fun registeredItems(): ArrayList<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.diagnosis
            identifier = items.count().toLong()
        }

        RisksLevelManager.getCurrentLevel(robertManager.atRiskStatus?.riskLevel)?.let {
            if (!sharedPreferences.hideRiskStatus) {
                items += healthCardItem(R.layout.item_health_card) {
                    header = getStatusLastUpdateToDisplay(requireContext(), robertManager.atRiskLastRefresh, it.riskLevel)

                    title = strings[it.labels.detailTitle]
                    caption = strings[it.labels.detailSubtitle]
                    gradientBackground = it.getGradientBackground()
                    identifier = items.count().toLong()

                    dateValue()?.let { date ->
                        dateLabel = strings["myHealthStateHeaderCell.exposureDate.title"]
                        dateValue = date
                    }
                    
                    statusUpdateAction = refreshStatusActions(robertManager.liveUpdatingRiskStatus.value?.peekContent())
                }
                items += spaceItem {
                    spaceRes = R.dimen.spacing_medium
                    identifier = items.count().toLong()
                }
            }
            robertManager.configuration.ameliUrl?.takeIf { url -> url.isNotBlank() }?.let { ameliUrl ->
                robertManager.declarationToken?.takeIf { token -> token.isNotBlank() }?.let { declarationToken ->
                    items += cardWithActionItem(CardTheme.Primary) {
                        cardTitle = strings["myHealthController.workStopping.title"]
                        cardTitleIcon = R.drawable.ic_recommendation
                        mainBody = strings["myHealthController.workStopping.message"]
                        actions = listOf(
                            Action(
                                label = strings["myHealthController.workStopping.link"],
                                onClickListener = {
                                    context?.let { context ->
                                        ameliWithDeclarationUrl(ameliUrl, declarationToken)?.openInExternalBrowser(
                                            context,
                                            showToast = true
                                        )
                                    }
                                }
                            ),
                            Action(
                                label = strings["myHealthController.workStopping.share"],
                                onClickListener = {
                                    context?.let { context ->
                                        ShareManager.shareImageAndText(
                                            context,
                                            null,
                                            ameliWithDeclarationUrl(ameliUrl, declarationToken)
                                        ) {
                                            strings["common.error.unknown"]?.let { errorString -> showErrorSnackBar(errorString) }
                                        }
                                    }
                                }
                            )
                        )
                        identifier = "workStopping".hashCode().toLong()
                    }

                    items += spaceItem {
                        spaceRes = R.dimen.spacing_medium
                        identifier = items.count().toLong()
                    }
                }
            }
            it.sections.forEach {
                items += cardWithActionItem {
                    mainTitle = strings[it.section]
                    mainBody = strings[it.description]

                    it.link?.let { link ->
                        actions = listOf(
                            Action(
                                icon = null,
                                label = strings[link.label],
                                showBadge = false,
                                onClickListener = actionForLink(link)
                            )
                        )
                    }
                    identifier = items.count().toLong()
                }

                items += spaceItem {
                    spaceRes = R.dimen.spacing_medium
                    identifier = items.count().toLong()
                }
            }
        }

        return items
    }

    private fun ameliWithDeclarationUrl(ameliUrl: String, declarationToken: String): String? {
        return try {
            String.format(ameliUrl.fixFormatter(), declarationToken)
        } catch (e: IllegalFormatException) {
            Timber.e(e)
            null
        }
    }

    private fun actionForLink(link: RisksUILevelSectionLink): View.OnClickListener {
        return when (link.type) {
            LinkType.WEB -> View.OnClickListener {
                strings[link.action]?.openInExternalBrowser(it.context, true)
            }
            LinkType.CONTROLLER -> View.OnClickListener {
                findNavControllerOrNull()?.safeNavigate(HealthFragmentDirections.actionHealthFragmentToGestureFragment())
            }
        }
    }

    private fun dateValue(): String? {
        return robertManager.atRiskStatus?.ntpLastContactS?.ntpTimeSToUnixTimeMs()?.let { lastContactDate ->
            when (RisksLevelManager.getCurrentLevel(robertManager.atRiskStatus?.riskLevel)?.contactDateFormat) {
                ContactDateFormat.DATE -> longDateFormat.format(Date(lastContactDate))
                ContactDateFormat.RANGE -> {
                    RisksLevelManager.getLastContactDateFrom(robertManager.atRiskStatus?.riskLevel, lastContactDate)?.let { dateFrom ->
                        RisksLevelManager.getLastContactDateTo(robertManager.atRiskStatus?.riskLevel, lastContactDate)?.let { dateTo ->
                            stringsFormat(
                                "myHealthStateHeaderCell.exposureDate.range",
                                fromDateFormat.format(Date(dateFrom)),
                                toDateFormat.format(Date(dateTo))
                            )
                        }
                    }
                }
                else -> null
            }
        }
    }

    override fun timeRefresh() {
        refreshScreen()
    }
}
