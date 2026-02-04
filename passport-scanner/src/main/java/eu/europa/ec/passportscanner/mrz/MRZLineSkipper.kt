/*
 * Copyright (c) 2026 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.passportscanner.mrz

import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.passportscanner.parser.MrzRecord

/**
 * Utility class to try different combinations of MRZ lines when multiple lines are detected.
 * This helps handle cases where OCR detects extra lines that aren't part of the actual MRZ.
 */
class MRZLineSkipper(
    val logController: LogController,
    private val parseCallback: (String, LogController) -> MrzRecord
) {

    companion object {
        private const val TAG = "MRZLineSkipper"
    }

    /**
     * Try to parse MRZ by attempting different line combinations.
     * When 3+ lines are detected, tries windows of 2 and 3 lines.
     *
     * @param cleanedMrz The cleaned MRZ string from MRZCleaner
     * @return Valid MrzRecord if found, null otherwise
     */
    fun tryParse(cleanedMrz: String): MrzRecord? {
        val lines = cleanedMrz.split('\n').filter { it.isNotEmpty() }

        logController.d(TAG) { "Processing ${lines.size} lines" }

        if (lines.size < 3) {
            return tryParseLines(lines)
        }

        // Try window of 3 lines - TD1 format
        logController.d(TAG) { "Trying windows of 3 lines" }
        checkWindow(3, lines, null)?.let { return it }

        // Try window of 2 lines - TD3/Passport format - first line must start with 'P'
        logController.d(TAG) { "Trying windows of 2 lines (must start with 'P')" }
        checkWindow(2, lines, 'P')?.let { return it }


        logController.d(TAG) { "No valid combination found" }
        return null
    }

    /**
     * Try different windows of lines, keeping order.
     * For numLines=2, lines=[1,2,3]: tries [1,2], [1,3], [2,3]
     * For numLines=3, lines=[1,2,3,4]: tries [1,2,3], [1,2,4], [1,3,4], [2,3,4]
     *
     * @param numLines Number of lines in each window (2 or 3)
     * @param linesArray Array of all detected lines
     * @param startChar Expected first character of the first line (null to skip check)
     * @return Valid MrzRecord if found, null otherwise
     */
    private fun checkWindow(
        numLines: Int,
        linesArray: List<String>,
        startChar: Char?
    ): MrzRecord? {
        if (numLines > linesArray.size) {
            return null
        }

        // Generate all combinations of indices
        val combinations = generateCombinations(linesArray.size, numLines)

        logController.d(TAG) {
            "Checking ${combinations.size} combinations of $numLines lines from ${linesArray.size} total"
        }

        for ((index, combo) in combinations.withIndex()) {
            val selectedLines = combo.map { linesArray[it] }

            // Check if first line starts with expected character
            if (startChar != null) {
                val firstLine = selectedLines.firstOrNull()
                if ( firstLine.isNullOrEmpty() || firstLine[0] != startChar) {
                    logController.d(TAG) {
                        "Skipping combination ${index + 1}/${combinations.size}: lines ${combo.map { it + 1 }} - " +
                        "first line doesn't start with '$startChar' (starts with '${firstLine?.firstOrNull() ?: "empty"}')"
                    }
                    continue
                }
            }

            logController.d(TAG) {
                "Trying combination ${index + 1}/${combinations.size}: lines ${combo.map { it + 1 }}"
            }

            tryParseLines(selectedLines)?.let { record ->
                logController.d(TAG) {
                    "SUCCESS! Valid MRZ found with lines ${combo.map { it + 1 }}"
                }
                return record
            }
        }

        return null
    }

    /**
     * Generate all combinations of k elements from n elements (C(n,k))
     * Returns list of index combinations, preserving order.
     *
     * Example: generateCombinations(3, 2) returns [[0,1], [0,2], [1,2]]
     * Example: generateCombinations(4, 3) returns [[0,1,2], [0,1,3], [0,2,3], [1,2,3]]
     */
    private fun generateCombinations(n: Int, k: Int): List<List<Int>> {
        if (k > n || k <= 0) return emptyList()
        if (k == n) return listOf((0 until n).toList())

        val result = mutableListOf<List<Int>>()
        val combination = IntArray(k)

        fun backtrack(start: Int, depth: Int) {
            if (depth == k) {
                result.add(combination.toList())
                return
            }

            for (i in start until n) {
                combination[depth] = i
                backtrack(i + 1, depth + 1)
            }
        }

        backtrack(0, 0)
        return result
    }

    /**
     * Attempt to parse a list of lines as an MRZ record.
     *
     * @param lines Lines to combine and parse
     * @return Valid MrzRecord if successful, null otherwise
     */
    private fun tryParseLines(lines: List<String>): MrzRecord? {
        if (lines.isEmpty()) return null

        val mrzString = lines.joinToString("\n")

        return try {
            val record = parseCallback(mrzString, logController)

            // Check if the record has valid checksums
            if (record.validDateOfBirth &&
                record.validDocumentNumber &&
                record.validExpirationDate ||
                record.validComposite) {
                record
            } else {
                logController.d(TAG) {
                    "Parse succeeded but checksums invalid: " +
                    "DOB=${record.validDateOfBirth}, " +
                    "DocNum=${record.validDocumentNumber}, " +
                    "Exp=${record.validExpirationDate}, " +
                    "Composite=${record.validComposite}"
                }
                null
            }
        } catch (e: Exception) {
            logController.d(TAG) { "Parse failed: ${e.message}" }
            null
        }
    }
}
