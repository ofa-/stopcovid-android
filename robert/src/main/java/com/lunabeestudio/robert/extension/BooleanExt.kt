package com.lunabeestudio.robert.extension

import com.lunabeestudio.robert.model.AtRiskStatus

fun Boolean?.toAtRiskStatus(): AtRiskStatus = when (this) {
    true -> AtRiskStatus.AT_RISK
    false -> AtRiskStatus.NOT_AT_RISK
    null -> AtRiskStatus.UNKNOWN
}