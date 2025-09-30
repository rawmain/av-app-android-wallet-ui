/*
 * Copyright (C) 2020 Newlogic Pte. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package eu.europa.ec.passportscanner.mrz

import android.util.Log
import eu.europa.ec.passportscanner.SmartScannerActivity
import eu.europa.ec.passportscanner.parser.MrzParser
import eu.europa.ec.passportscanner.parser.MrzRecord
import eu.europa.ec.passportscanner.parser.records.MrtdTd1

import java.net.URLEncoder


object MRZCleaner {
    private var previousMRZString:String? = null

    fun clean(mrz: String): String {
        if (mrz.isBlank()) {
            throw IllegalArgumentException("Empty MRZ string.")
        }
//        Log.v(SmartScannerActivity.TAG, "got MRZ: $mrz")

        var result = mrz
            .replace(Regex("^[^PIACV]*"), "") // Remove everything before P, I, A or C
            .replace(Regex("[ \\t\\r]+"), "") // Remove any white space
            .replace(Regex("\\n+"), "\n") // Remove extra new lines
            .replace("«", "<")
            .replace("<c<", "<<<")
            .replace("<e<", "<<<")
            .replace("<E<", "<<<") // Good idea? Maybe not.
            .replace("<K<", "<<<") // Good idea? Maybe not.
            .replace("<S<", "<<<") // Good idea? Maybe not.
            .replace("<C<", "<<<") // Good idea? Maybe not.
            .replace("<¢<", "<<<")
            .replace("<(<", "<<<")
            .replace("<{<", "<<<")
            .replace("<[<", "<<<")
            .replace(Regex("^P[KC]"), "P<")
            .replace(Regex("[^A-Z0-9<\\n]"), "") // Remove any other char
            .trim()

        result = reconstructTd3LinesIfNeeded(result)

        if (result.contains("<") && (
                    result.startsWith("P") ||
                    result.startsWith("I") ||
                    result.startsWith("A") ||
                    result.startsWith("C") ||
                    result.startsWith("V"))
        ) {
            when (result.filter{ it == '\n' }.count()) {
                1 -> {
                    if (result.length > 89) {
                        result = result.slice(IntRange(0, 88))
                    }
                }
                2 -> {
                    if (result.length > 92) {
                        result = result.slice(IntRange(0, 91))
                    }
                }
                else -> throw IllegalArgumentException("Invalid MRZ string. Wrong number of lines.")
            }
        } else {
            Log.d(SmartScannerActivity.TAG, "Error = [${URLEncoder.encode(result, "UTF-8").replace("%3C", "<").replace("%0A", "↩")}]")
            throw IllegalArgumentException("Invalid MRZ string. No '<' or 'P', 'I', 'A', 'C', 'V' detected.")
        }

        return result
    }

    private fun reconstructTd3LinesIfNeeded(mrzText: String): String {
        if (!mrzText.startsWith("P<")) {
            return mrzText
        }

        val lines = mrzText.split('\n').filter { it.isNotEmpty() }


        if (lines.size <= 2) {
            return mrzText
        }


        val allChars = lines.joinToString("")
        return if (allChars.length >= 88) {
            Log.d(SmartScannerActivity.TAG, "fixing lines ${lines.size} for TD3")
            allChars.substring(0, 44) + "\n" + allChars.substring(44, 88)
        } else {
            mrzText
        }
    }

    private fun String.replaceNumbertoChar(): String {
        return this
            .replace('0', 'O')
            .replace("1", "I")
            .replace("8", "B")
            .replace("5", "S")
            .replace("2", "Z")
            .replace("3", "J")
    }

    fun parseAndClean(mrz: String): MrzRecord {
        val record = MrzParser.parse(mrz)

        Log.d(SmartScannerActivity.TAG, "Previous Scan: $previousMRZString")
        if (record.validDateOfBirth && record.validDocumentNumber && record.validExpirationDate || record.validComposite) {
            record.givenNames = record.givenNames?.replaceNumbertoChar()
            record.surname = record.surname.replaceNumbertoChar()
            record.issuingCountry = record.issuingCountry.replaceNumbertoChar()
            record.nationality = record.nationality.replaceNumbertoChar()
            return record
        } else {
            Log.d(SmartScannerActivity.TAG, "Still accept scanning.")
            Log.d(SmartScannerActivity.TAG, "Previous Scan: $previousMRZString")
            if (mrz != previousMRZString) {
                previousMRZString = mrz
            }
            throw IllegalArgumentException("Invalid check digits.")
        }
    }

    fun parseAndCleanMrtdTd1(mrz: String): MrtdTd1 {
        val record = MrzParser.parseToMrtdTd1(mrz)

        Log.d(SmartScannerActivity.TAG, "Previous Scan: $previousMRZString")
        if (record.validDateOfBirth && record.validDocumentNumber && record.validExpirationDate || record.validComposite) {
            record.givenNames = record.givenNames?.replaceNumbertoChar()
            record.surname = record.surname.replaceNumbertoChar()
            record.issuingCountry = record.issuingCountry.replaceNumbertoChar()
            record.nationality = record.nationality.replaceNumbertoChar()
            return record
        } else {
            Log.d(SmartScannerActivity.TAG, "Still accept scanning.")
            if (mrz != previousMRZString) {
                previousMRZString = mrz
            }
            throw IllegalArgumentException("Invalid check digits.")
        }
    }
}
