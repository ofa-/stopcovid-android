/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/12/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

enum class IsolationRecommendationStateEnum(val key: String) {

    INITIAL_CASE_SAFE("initialCaseSafe"),
    INITIAL_CASE_AT_RISK_OR_SICK("initialCaseAtRiskOrSick"),

    ALL_GOOD("allGood"),

    SYMPTOMS("symptoms"),
    SYMPTOMS_TESTED("symptomsTested"),

    CONTACT_CASE_UNKNOWN_INDEX("contactCaseUnknownIndex"),
    CONTACT_CASE_KNOWN_INDEX_NOT_TESTED("contactCaseKnownIndexNotTested"),
    CONTACT_CASE_KNOWN_INDEX_TESTED_KNOWN_DATE("contactCaseKnownIndexTestedKnownDate"),
    CONTACT_CASE_KNOWN_INDEX_TESTED_UNKNOWN_DATE("contactCaseKnownIndexTestedUnknownDate"),
    CONTACT_CASE_POST_ISOLATION_PERIOD("contactCasePostIsolationPeriod"),

    POSITIVE_CASE_NO_SYMPTOMS("positiveCaseNoSymptoms"),
    POSITIVE_CASE_SYMPTOMS_DURING_ISOLATION("positiveCaseSymptomsDuringIsolation"),
    POSITIVE_CASE_SYMPTOMS_AFTER_ISOLATION("positiveCaseSymptomsAfterIsolation"),
    POSITIVE_CASE_SYMPTOMS_AFTER_ISOLATION_STILL_HAVING_FEVER("positiveCaseSymptomsAfterIsolationStillHavingFever"),
    POSITIVE_CASE_POST_ISOLATION_PERIOD("positiveCasePostIsolationPeriod"),

    INDETERMINATE("indeterminate");

    fun getTitleStringKey(): String = "isolation.recommendation.$key.title"
    fun getBodyStringKey(): String = "isolation.recommendation.$key.body"
}
