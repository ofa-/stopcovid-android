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

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.findParentFragmentByType
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLanguage
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.getKeyFigureForPostalCode
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.itemForFigure
import com.lunabeestudio.stopcovid.extension.labelStringKey
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.showErrorSnackBar
import com.lunabeestudio.stopcovid.fastitem.KeyFigureCardItem
import com.lunabeestudio.stopcovid.fastitem.explanationActionCardItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.DepartmentKeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigureCategory
import com.lunabeestudio.stopcovid.model.KeyFiguresNotAvailableException
import com.lunabeestudio.stopcovid.model.TacResult
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class KeyFiguresFragment : MainFragment() {

    private var category: KeyFigureCategory = KeyFigureCategory.UNKNOWN

    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
    private val dateFormat by lazy {
        SimpleDateFormat(UiConstants.DAY_MONTH_DATE_PATTERN, Locale(requireContext().getApplicationLanguage()))
    }

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = (arguments?.getSerializable(CATEGORY_ARG_KEY) as? KeyFigureCategory) ?: category
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        keyFiguresManager.figures.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        binding?.recyclerView?.let {
            findParentFragmentByType<KeyFiguresPagerFragment>()?.bindFabToRecyclerView(it)
        }
    }

    override fun refreshScreen() {
        super.refreshScreen()
        (activity as? MainActivity)?.binding?.tabLayout?.isVisible = true
    }

    override suspend fun getItems(): List<GenericItem> {
        val items = mutableListOf<GenericItem>()
        addFetchErrorItemIfNeeded(items)
        addExplanationItemIfNeeded(items)
        addKeyFiguresItems(items)
        return items
    }

    private fun addFetchErrorItemIfNeeded(items: MutableList<GenericItem>) {
        keyFiguresManager.figures.value?.peekContent()?.let { keyFiguresResult ->
            val error = (keyFiguresResult as? TacResult.Failure)?.throwable
            if (error is KeyFiguresNotAvailableException) {
                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.count().toLong()
                }

                items += explanationActionCardItem {
                    explanation = error.getString(strings)
                    bottomText = strings["keyFiguresController.fetchError.button"]
                    onClick = {
                        viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
                            (activity as? MainActivity)?.showProgress(true)
                            keyFiguresManager.onAppForeground(requireContext())
                            vaccinationCenterManager.postalCodeDidUpdate(
                                requireContext(),
                                sharedPrefs,
                                sharedPrefs.chosenPostalCode,
                            )
                            (activity as? MainActivity)?.showProgress(false)
                            refreshScreen()
                        }
                    }
                    identifier = "keyFiguresController.fetchError.button".hashCode().toLong()
                }
            }
        }
    }

    private fun addExplanationItemIfNeeded(items: MutableList<GenericItem>) {
        val explanations = strings["keyFiguresController.explanations.${category.stringCode}"]
        if (!explanations.isNullOrEmpty()) {
            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.count().toLong()
            }

            items += captionItem {
                text = explanations
                identifier = text.hashCode().toLong()
            }
            items += linkItem {
                text = strings["keyFiguresController.section.health.button"]
                onClickListener = View.OnClickListener {
                    findNavControllerOrNull()
                        ?.safeNavigate(KeyFiguresPagerFragmentDirections.actionKeyFiguresPagerFragmentToMoreKeyFigureFragment())
                }
                identifier = text.hashCode().toLong()
            }
        }
    }

    private fun addKeyFiguresItems(items: MutableList<GenericItem>) {
        keyFiguresManager.figures.value?.peekContent()?.let { keyFiguresResult ->
            if (keyFiguresResult.data?.isNotEmpty() == true) {
                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.count().toLong()
                }

                keyFiguresResult.data
                    ?.filter { it.category == category }
                    ?.mapNotNullTo(items) {
                        val departmentKeyFigure = if (keyFiguresResult is TacResult.Success) {
                            it.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)
                        } else {
                            it.getKeyFigureForPostalCode(null)
                        }
                        itemForFigure(it, departmentKeyFigure)
                    }

                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.count().toLong()
                }
            }
        }
    }

    private fun itemForFigure(figure: KeyFigure, departmentKeyFigure: DepartmentKeyFigure?): KeyFigureCardItem? {
        return figure.itemForFigure(
            context = requireContext(),
            sharedPrefs = sharedPrefs,
            departmentKeyFigure = departmentKeyFigure,
            numberFormat = numberFormat,
            dateFormat = dateFormat,
            strings = strings,
        ) {
            shareContentDescription = stringsFormat("accessibility.hint.keyFigure.share.withLabel", strings[figure.labelStringKey])
            onShareCard = { binding ->
                viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
                    val uri = ShareManager.getShareCaptureUri(binding, "$label")
                    withContext(Dispatchers.Main) {
                        val shareString = if (rightLocation == null) {
                            stringsFormat("keyFigure.sharing.national", label, leftValue)
                        } else {
                            stringsFormat("keyFigure.sharing.department", label, leftLocation, leftValue, label, rightValue)
                        }
                        ShareManager.shareImageAndText(requireContext(), uri, shareString) {
                            strings["common.error.unknown"]?.let { showErrorSnackBar(it) }
                        }
                    }
                }
            }
            onClickListener = View.OnClickListener {
                analyticsManager.reportAppEvent(AppEventName.e9, null)
                parentFragment?.findNavControllerOrNull()?.safeNavigate(
                    KeyFiguresPagerFragmentDirections.actionKeyFiguresPagerFragmentToKeyFigureDetailsFragment(
                        figure.labelKey
                    )
                )
            }
            descriptionMaxLines = 2
        }
    }

    override fun getTitleKey(): String = "keyFiguresController.title"

    companion object {
        const val CATEGORY_ARG_KEY: String = "CATEGORY_ARG_KEY"

        fun newInstance(category: KeyFigureCategory): KeyFiguresFragment {
            val fragment = KeyFiguresFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(CATEGORY_ARG_KEY, category)
            }
            return fragment
        }
    }
}
