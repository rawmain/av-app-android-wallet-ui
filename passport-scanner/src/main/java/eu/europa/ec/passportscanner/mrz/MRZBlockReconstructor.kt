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

import android.graphics.Rect
import eu.europa.ec.businesslogic.controller.log.LogController
import kotlin.math.abs

/**
 * Utility to reconstruct MRZ lines from OCR text blocks that may be split horizontally.
 *
 * Common OCR issue: TD3 passports often get detected as 4 blocks (top-left, top-right,
 * bottom-left, bottom-right) instead of 2 lines, with "<" filler characters missed between blocks.
 */
object MRZBlockReconstructor {

    private const val TAG = "MRZBlockReconstructor"
    private const val TD3_LINE_LENGTH = 44
    private const val MRZ_FILLER_CHAR = '<'

    /**
     * Represents a text block with its position
     */
    data class TextBlock(
        val text: String,
        val boundingBox: Rect
    )

    /**
     * Reconstructs MRZ text from blocks by grouping them into lines and filling gaps.
     *
     * @param blocks List of text blocks with bounding boxes from OCR
     * @param logController Logger for debugging
     * @return Reconstructed MRZ text with proper line breaks
     */
    fun reconstruct(blocks: List<TextBlock>, logController: LogController): String {
        if (blocks.isEmpty()) {
            return ""
        }

        // Group blocks into lines based on vertical position
        val lines = groupBlocksIntoLines(blocks, logController)

        // Check if any line starts with "P" to detect TD3 passport format
        val isTd3Passport = lines.any { lineBlocks ->
            lineBlocks.firstOrNull()?.text?.startsWith("P") == true
        }

        // Reconstruct each line
        val reconstructedLines = lines.mapIndexed { index, lineBlocks ->
            val isSecondLine = isTd3Passport && lines.size == 2 && index == 1
            reconstructLine(lineBlocks, isTd3Passport, isSecondLine, logController)
        }

        // Validate TD3 format: second line must end with a digit
        if (isTd3Passport && reconstructedLines.size == 2) {
            val secondLine = reconstructedLines[1]
            val lastChar = secondLine.lastOrNull()
            if (lastChar == null || !lastChar.isDigit()) {
                logController.w(TAG) {
                    "TD3 validation warning: Second line should end with digit, ends with '${lastChar ?: "empty"}'"
                }
            }
        }

        return reconstructedLines.joinToString("\n")
    }

    /**
     * Groups text blocks into lines based on their vertical position (Y-coordinate).
     * Blocks are considered on the same line if their Y-centers are within a threshold.
     */
    private fun groupBlocksIntoLines(
        blocks: List<TextBlock>,
        logController: LogController
    ): List<List<TextBlock>> {
        if (blocks.isEmpty()) return emptyList()

        // Calculate vertical center for each block
        val blocksWithCenter = blocks.map { block ->
            val centerY = (block.boundingBox.top + block.boundingBox.bottom) / 2
            block to centerY
        }

        // Sort by vertical position (top to bottom)
        val sortedBlocks = blocksWithCenter.sortedBy { it.second }

        // Group blocks with similar Y positions
        val lines = mutableListOf<MutableList<TextBlock>>()
        var currentLine = mutableListOf<TextBlock>()
        var currentCenterY = sortedBlocks.first().second

        // Threshold: blocks within 30% of average block height are considered same line
        val avgBlockHeight = blocks.map { it.boundingBox.height() }.average()
        val threshold = (avgBlockHeight * 0.3).toInt()

        for ((block, centerY) in sortedBlocks) {
            if (abs(centerY - currentCenterY) <= threshold) {
                // Same line
                currentLine.add(block)
            } else {
                // New line
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = mutableListOf(block)
                currentCenterY = centerY
            }
        }

        // Add last line
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        // Sort blocks within each line by X position (left to right)
        lines.forEach { line ->
            line.sortBy { it.boundingBox.left }
        }

        return lines
    }

    /**
     * Reconstructs a single MRZ line from horizontally split blocks.
     * Fills gaps between blocks with "<" characters if TD3 format is detected.
     *
     * @param lineBlocks Blocks that belong to this line
     * @param isTd3Passport Whether TD3 passport format was detected
     * @param isSecondLine Whether this is the second line of a TD3 passport (must end with digit)
     * @param logController Logger for debugging
     */
    private fun reconstructLine(
        lineBlocks: List<TextBlock>,
        isTd3Passport: Boolean,
        isSecondLine: Boolean,
        logController: LogController
    ): String {
        if (lineBlocks.isEmpty()) return ""

        // For single block in TD3 format, we might still need to pad to 44 chars
        if (lineBlocks.size == 1) {
            val text = lineBlocks.first().text
            if (isTd3Passport && text.length < TD3_LINE_LENGTH) {
                return text + MRZ_FILLER_CHAR.toString().repeat(TD3_LINE_LENGTH - text.length)
            }
            return text
        }

        if (!isTd3Passport) {
            // Not TD3, just concatenate
            return lineBlocks.joinToString("") { it.text }
        }

        // Calculate current text length
        val currentTextLength = lineBlocks.sumOf { it.text.length }

        // If we already have 44+ characters, just concatenate
        if (currentTextLength >= TD3_LINE_LENGTH) {
            return lineBlocks.joinToString("") { it.text }
        }

        // Calculate fillers needed between blocks
        val totalFillersNeeded = TD3_LINE_LENGTH - currentTextLength
        val gaps = lineBlocks.size - 1

        // Estimate gap widths based on bounding box positions
        val gapWidths = mutableListOf<Int>()
        for (i in 0 until gaps) {
            val currentBlock = lineBlocks[i]
            val nextBlock = lineBlocks[i + 1]
            val gapWidth = nextBlock.boundingBox.left - currentBlock.boundingBox.right
            gapWidths.add(gapWidth)
        }

        val totalGapWidth = gapWidths.sum()

        // Distribute fillers proportionally to gap widths
        val fillersPerGap = gapWidths.map { gapWidth ->
            if (totalGapWidth == 0) {
                totalFillersNeeded / gaps
            } else {
                ((gapWidth.toFloat() / totalGapWidth) * totalFillersNeeded).toInt()
            }
        }.toMutableList()

        // Adjust for rounding errors
        val fillersDistributed = fillersPerGap.sum()
        if (fillersDistributed < totalFillersNeeded) {
            fillersPerGap[fillersPerGap.lastIndex] += (totalFillersNeeded - fillersDistributed)
        }

        // Build the reconstructed line
        val result = buildString {
            lineBlocks.forEachIndexed { index, block ->
                append(block.text)
                if (index < gaps) {
                    val fillersCount = fillersPerGap[index]
                    append(MRZ_FILLER_CHAR.toString().repeat(fillersCount))
                }
            }
        }

        // For TD3 second line, verify it ends with a digit
        if (isSecondLine && result.isNotEmpty()) {
            val lastChar = result.last()
            if (!lastChar.isDigit()) {
                logController.w(TAG) {
                    "TD3 second line should end with digit but ends with '$lastChar'"
                }
            }
        }

        return result
    }
}
