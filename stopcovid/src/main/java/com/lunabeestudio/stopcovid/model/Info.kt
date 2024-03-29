/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

import android.content.Context
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLanguage
import org.json.JSONObject
import java.util.HashMap

/**
 * Abstract class to manage multi language for message and buttonTitle
 */
@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class Info(jsonObject: JSONObject) {

    var isActive: Boolean? = null
    private var _mode: String
    var minRequiredBuildNumber: Int? = null
    var minInfoBuildNumber: Int? = null
    private var messageMap: HashMap<String, String> = hashMapOf()

    fun getMessage(context: Context): String? = getValueForDefaultLanguageOrNull(messageMap, context)
    fun getButtonTitle(context: Context): String? = getValueForDefaultLanguageOrNull(buttonTitleMap, context)
    fun getButtonUrl(context: Context): String? = getValueForDefaultLanguageOrNull(buttonUrlMap, context)

    private var buttonTitleMap: HashMap<String, String> = hashMapOf()
    private var buttonUrlMap: HashMap<String, String> = hashMapOf()

    private fun getValueForDefaultLanguageOrNull(map: HashMap<String, String>, context: Context): String? {
        val result = map[context.getApplicationLanguage()] ?: map["en"]
        return if (result.isNullOrEmpty()) null else result
    }

    private fun mapStrings(jsonObject: JSONObject?, map: HashMap<String, String>) {
        jsonObject?.let {
            for (key in it.keys()) {
                map[key] = it.getString(key)
            }
        }
    }

    val mode: Mode?
        get() = Mode.getMode(_mode)

    init {
        val androidInfoJSONObject = jsonObject.optJSONObject("Android")
        isActive = androidInfoJSONObject.optBoolean("isActive")
        _mode = androidInfoJSONObject.optString("mode")
        minRequiredBuildNumber = androidInfoJSONObject.optInt("minRequiredBuildNumber")
        minInfoBuildNumber = androidInfoJSONObject.optInt("minInfoBuild")
        mapStrings(androidInfoJSONObject.optJSONObject("message"), messageMap)
        mapStrings(androidInfoJSONObject.optJSONObject("buttonTitle"), buttonTitleMap)
        mapStrings(androidInfoJSONObject.optJSONObject("buttonURL"), buttonUrlMap)
    }

    enum class Mode {
        MAINTENANCE, UPGRADE;

        companion object {
            fun getMode(mode: String): Mode? {
                return when (mode) {
                    "maintenance" -> MAINTENANCE
                    "upgrade" -> UPGRADE
                    else -> null
                }
            }
        }
    }
}