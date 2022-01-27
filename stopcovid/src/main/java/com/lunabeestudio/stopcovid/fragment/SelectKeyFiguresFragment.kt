package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.descriptionStringKey
import com.lunabeestudio.stopcovid.extension.labelStringKey
import com.lunabeestudio.stopcovid.fastitem.SelectionItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.selectionItem
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigureCategory
import com.mikepenz.fastadapter.GenericItem

class SelectKeyFiguresFragment : MainFragment() {

    private val args: SelectKeyFiguresFragmentArgs by navArgs()

    private var labelKey1: String? = null
    private var labelKey2: String? = null

    override fun getTitleKey(): String = "keyfigures.comparison.keyfiguresList.screen.title${args.figureNumber}"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        labelKey1 = args.labelKeyFigure1
        labelKey2 = args.labelKeyFigure2
    }

    override suspend fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()
        getKeyFiguresList(items)
        return items
    }

    fun getKeyFiguresList(items: ArrayList<GenericItem>) {
        keyFiguresManager.figures.value?.peekContent()?.let { keyFiguresResult ->
            keyFiguresResult.data?.let { keyFigures ->
                items += spaceItem {
                    spaceRes = R.dimen.spacing_medium
                    identifier = items.count().toLong()
                }
                items += bigTitleItem {
                    text = strings["keyFiguresController.category.vaccine"]
                    identifier = "keyFiguresController.category.vaccine".hashCode().toLong()
                }
                categorySelectionItem(items, keyFigures.filter { it.category == KeyFigureCategory.VACCINE })
                items += spaceItem {
                    spaceRes = R.dimen.spacing_medium
                    identifier = items.count().toLong()
                }
                items += bigTitleItem {
                    text = strings["keyFiguresController.category.health"]
                    identifier = "keyFiguresController.category.health".hashCode().toLong()
                }
                categorySelectionItem(items, keyFigures.filter { it.category == KeyFigureCategory.HEALTH })
                items += spaceItem {
                    spaceRes = R.dimen.spacing_medium
                    identifier = items.count().toLong()
                }
                items += bigTitleItem {
                    text = strings["keyFiguresController.category.app"]
                    identifier = "keyFiguresController.category.app".hashCode().toLong()
                }
                categorySelectionItem(items, keyFigures.filter { it.category == KeyFigureCategory.APP })
            }
        }
    }

    fun categorySelectionItem(items: ArrayList<GenericItem>, keyFigures: List<KeyFigure>) {
        keyFigures.mapTo(items) {
            selectionItem {
                title = strings[it.labelStringKey]
                identifier = it.labelStringKey.hashCode().toLong()
                caption = strings[it.descriptionStringKey]
                identifier = title.hashCode().toLong()
                maxLineCaption = 3
                if (args.figureNumber == 1 && labelKey2 != it.labelKey) {
                    onClick = {
                        labelKey1 = it.labelKey
                        backToChoiceFragment()
                    }
                } else if (args.figureNumber == 2 && labelKey1 != it.labelKey) {
                    onClick = {
                        labelKey2 = it.labelKey
                        backToChoiceFragment()
                    }
                }

                when (it.labelKey) {
                    labelKey1 -> applyColorSelectItem(this, 1)
                    labelKey2 -> applyColorSelectItem(this, 2)
                }
            }
        }
    }

    fun applyColorSelectItem(selectionItem: SelectionItem, labelKeyNumber: Int) {
        selectionItem.apply {
            showSelection = true
            iconSelectionOn = if (labelKeyNumber == 1) R.drawable.ic_one else R.drawable.ic_two
            if (args.figureNumber != labelKeyNumber) {
                context?.let {
                    val color = ContextCompat.getColor(it, R.color.color_gray)
                    iconTint = color
                    textColor = color
                }
            }
        }
    }

    fun backToChoiceFragment() {
        setFragmentResult(
            RESULT_LISTENER_KEY,
            bundleOf(
                RESULT_LISTENER_BUNDLE_KEY1 to labelKey1,
                RESULT_LISTENER_BUNDLE_KEY2 to labelKey2
            )
        )
        findNavControllerOrNull()?.popBackStack()
    }

    companion object {
        const val RESULT_LISTENER_KEY: String = "resultKeyFigurePicker"
        const val RESULT_LISTENER_BUNDLE_KEY1: String = "resultKeyFigure1"
        const val RESULT_LISTENER_BUNDLE_KEY2: String = "resultKeyFigure2"
    }
}