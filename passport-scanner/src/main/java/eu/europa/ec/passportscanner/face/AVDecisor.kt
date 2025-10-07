/*
 * MIT License
 *
 * Copyright (c) 2025 Keyless Tech
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package eu.europa.ec.passportscanner.face

/**
 * Decision making component for face matching based on multiple samples
 * Aggregates results from multiple frame captures to make final decision
 */
class AVDecisor(private val numSamples: Int = 3) {
    private val results = mutableListOf<Boolean>()
    private val TAG = "AVDecisor"

    /**
     * Add a matching result from a frame
     * @param isMatch whether the frame shows a match
     */
    fun addResult(isMatch: Boolean) {
        results.add(isMatch)
        android.util.Log.d(TAG, "addResult: Added result $isMatch, total samples: ${results.size}/$numSamples")
        android.util.Log.d(TAG, "addResult: Current results: $results")
    }

    /**
     * Check if we have enough samples to make a decision
     */
    fun hasEnoughSamples(): Boolean {
        val enough = results.size >= numSamples
        android.util.Log.d(TAG, "hasEnoughSamples: $enough (${results.size}/$numSamples)")
        return enough
    }

    /**
     * Get the final decision based on majority vote
     * @return true if majority of samples indicate a match
     */
    fun getFinalDecision(): Boolean {
        if (results.isEmpty()) {
            android.util.Log.d(TAG, "getFinalDecision: No results available, returning false")
            return false
        }

        val matchCount = results.count { it }
        val decision = matchCount > results.size / 2
        android.util.Log.d(TAG, "getFinalDecision: $matchCount matches out of ${results.size} samples = $decision")
        return decision
    }

    /**
     * Reset the decisor for a new matching session
     */
    fun reset() {
        android.util.Log.d(TAG, "reset: Clearing ${results.size} previous results")
        results.clear()
    }

    /**
     * Get current sample count
     */
    fun getSampleCount(): Int = results.size
}