/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/06/04 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.tools

class CSVReader(private val fileName: String,
    private val delimiter: String = ";") {

    fun readLines(valuesAction: (List<String>) -> Unit) {

        CSVReader::class.java.classLoader!!
            .getResourceAsStream(fileName)
            .bufferedReader(charset = Charsets.UTF_8)
            .useLines { lines ->
                lines.filterIndexed { index, _ -> index != 0 }
                    .map { line -> line.split(delimiter) }
                    .forEach { valuesAction(it) }
            }
    }

}