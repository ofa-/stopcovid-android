package com.lunabeestudio.stopcovid.model

import com.google.gson.annotations.SerializedName

data class RisksUILevel(
    val contactDateFormat: ContactDateFormat? = ContactDateFormat.UNKNOWN,
    val riskLevel: Float,
    val description: String,
    val labels: RisksUILevelLabels,
    val color: RisksUILevelColor,
    val sections: List<RisksUILevelSection>

)

data class RisksUILevelLabels(
    val homeTitle: String,
    val homeSub: String,
    val detailTitle: String,
    val detailSubtitle: String,
    val widgetShort: String,
    val widgetLong: String,
    val notifTitle: String?,
    val notifBody: String?
)

data class RisksUILevelColor(
    val from: String,
    val to: String
)

data class RisksUILevelSection(
    val section: String,
    val description: String,
    val link: RisksUILevelSectionLink?
)

data class RisksUILevelSectionLink(
    val label: String,
    val action: String,
    val type: LinkType
)

enum class ContactDateFormat {
    @SerializedName("date")
    DATE,

    @SerializedName("range")
    RANGE,

    UNKNOWN
}

enum class LinkType {
    @SerializedName("web")
    WEB,

    @SerializedName("ctrl")
    CONTROLLER
}
