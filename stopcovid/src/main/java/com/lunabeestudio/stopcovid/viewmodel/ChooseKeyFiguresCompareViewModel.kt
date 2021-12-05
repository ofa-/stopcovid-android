package com.lunabeestudio.stopcovid.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.extension.getDefaultFigureLabel1
import com.lunabeestudio.stopcovid.extension.getDefaultFigureLabel2
import com.lunabeestudio.stopcovid.extension.keyFigureCompare1
import com.lunabeestudio.stopcovid.extension.keyFigureCompare2

class ChooseKeyFiguresCompareViewModel(
    val strings: LocalizedStrings,
    val sharedPreferences: SharedPreferences,
    val configuration: Configuration
) : ViewModel() {

    var labelKey1: String? = sharedPreferences.keyFigureCompare1 ?: configuration.getDefaultFigureLabel1()
    var labelKey2: String? = sharedPreferences.keyFigureCompare2 ?: configuration.getDefaultFigureLabel2()

    val isBothKeyFigureSelected: Boolean
        get() = labelKey1 != null && labelKey2 != null

    init {
        verifyIfKeyFigureExist(1, labelKey1)
        verifyIfKeyFigureExist(2, labelKey2)
    }

    fun verifyIfKeyFigureExist(keyFigure: Int, labelKey: String?) {
        val keyString = strings["$labelKey.shortLabel"]
        if (keyString == null) {
            if (keyFigure == 1) {
                labelKey1 = null
            } else {
                labelKey2 = null
            }
        }
    }

    fun getLabelActionChoiceKey(keyFigure: Int, labelKey: String?): String? {
        return if (labelKey == null) {
            strings["keyfigures.comparison.keyfiguresList.screen.title$keyFigure"]
        } else {
            strings["$labelKey.shortLabel"]
        }
    }
}

class ChooseKeyFiguresCompareViewModelFactory(
    val strings: LocalizedStrings,
    val sharedPreferences: SharedPreferences,
    val configuration: Configuration
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChooseKeyFiguresCompareViewModel(
            strings,
            sharedPreferences,
            configuration
        ) as T
    }
}