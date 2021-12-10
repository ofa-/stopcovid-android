package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.domain.model.Configuration

fun Configuration.getDefaultFigureLabel1(): String? {
    return keyFiguresCombination?.get(0)?.keyFigureLabel1?.getLabelKeyFigureFromConfig()
}

fun Configuration.getDefaultFigureLabel2(): String? {
    return keyFiguresCombination?.get(0)?.keyFigureLabel2?.getLabelKeyFigureFromConfig()
}
