package com.lunabeestudio.stopcovid.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.preference.PreferenceManager
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.databinding.FragmentRecyclerWithBottomActionBinding
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.keyFigureCompare1
import com.lunabeestudio.stopcovid.extension.keyFigureCompare2
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.viewmodel.ChooseKeyFiguresCompareViewModel
import com.lunabeestudio.stopcovid.viewmodel.ChooseKeyFiguresCompareViewModelFactory
import com.mikepenz.fastadapter.GenericItem

class ChooseKeyFiguresCompareFragment : MainFragment() {
    override val layout: Int = R.layout.fragment_recycler_with_bottom_action
    override fun getTitleKey(): String = "keyfigures.comparison.screen.title"

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    val viewModel: ChooseKeyFiguresCompareViewModel by viewModels {
        ChooseKeyFiguresCompareViewModelFactory(
            strings,
            sharedPreferences,
            injectionContainer.robertManager.configuration
        )
    }

    var bindingBottomAction: FragmentRecyclerWithBottomActionBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindingBottomAction = FragmentRecyclerWithBottomActionBinding.bind(view).apply {
            bottomSheetButton.setOnClickListener {
                sharedPreferences.keyFigureCompare1 = viewModel.labelKey1
                sharedPreferences.keyFigureCompare2 = viewModel.labelKey2
                findNavControllerOrNull()?.safeNavigate(
                    ChooseKeyFiguresCompareFragmentDirections.actionChooseKeyFiguresCompareFragmentToCompareKeyFiguresFragment(),
                    NavOptions.Builder().setPopUpTo(R.id.chooseKeyFiguresCompareFragment, true).setLaunchSingleTop(true).build()
                )
            }
            bottomSheetButtonLight.setOnClickListener {
                findNavControllerOrNull()?.popBackStack()
            }
        }
    }

    override fun refreshScreen() {
        super.refreshScreen()
        bindingBottomAction?.bottomSheetButton?.text = strings["keyfigures.comparison.keyfiguresChoice.validation.button.title"]
        bindingBottomAction?.bottomSheetButtonLight?.text = strings["common.cancel"]
        bindingBottomAction?.bottomSheetButton?.isVisible = viewModel.isBothKeyFigureSelected
        bindingBottomAction?.bottomSheetButtonLight?.isVisible = !viewModel.isBothKeyFigureSelected
    }

    override suspend fun getItems(): List<GenericItem> {
        val items = mutableListOf<GenericItem>()
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
        items += keyFigureChoiceSection()
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
        return items
    }

    private fun keyFigureChoiceSection() = cardWithActionItem {
        mainTitle = strings["keyfigures.comparison.keyfiguresChoice.section.title"]
        mainBody = strings["keyfigures.comparison.keyfiguresChoice.section.subtitle"]
        actions = listOf(
            Action(R.drawable.ic_one, viewModel.getLabelActionChoiceKey(1, viewModel.labelKey1)) {
                launchPickerKeyFigure(1)
            },
            Action(R.drawable.ic_two, viewModel.getLabelActionChoiceKey(2, viewModel.labelKey2)) {
                launchPickerKeyFigure(2)
            }
        )
        identifier = "keyfigures.comparison.keyfiguresChoice.section.title".hashCode().toLong()
    }

    private fun launchPickerKeyFigure(figureNumber: Int) {
        setFragmentResultListener(SelectKeyFiguresFragment.RESULT_LISTENER_KEY) { _, bundle ->
            viewModel.labelKey1 = bundle.getString(SelectKeyFiguresFragment.RESULT_LISTENER_BUNDLE_KEY1)
            viewModel.labelKey2 = bundle.getString(SelectKeyFiguresFragment.RESULT_LISTENER_BUNDLE_KEY2)
            refreshScreen()
        }
        findNavControllerOrNull()?.safeNavigate(
            ChooseKeyFiguresCompareFragmentDirections.actionChooseKeyFiguresCompareFragmentToSelectKeyFiguresFragment(
                figureNumber,
                viewModel.labelKey1,
                viewModel.labelKey2
            )
        )
    }
}