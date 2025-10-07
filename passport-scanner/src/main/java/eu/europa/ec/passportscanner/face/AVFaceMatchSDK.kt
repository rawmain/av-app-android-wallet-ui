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
 * Interface for Age Verification Face Matching SDK
 * Provides face liveness detection and matching capabilities for passport verification
 */
interface AVFaceMatchSDK {

    /**
     * Initialize the SDK with configuration
     * @param configJson JSON configuration string containing model paths and settings
     * @param onProgress Optional callback for initialization progress (percentage: 0-100, message)
     * @return true if initialization was successful, false otherwise
     */
    suspend fun init(configJson: String, onProgress: ((Int, String) -> Unit)? = null): Boolean

    /**
     * Capture live face and match against reference image
     * @param referenceImagePath Path to the reference image from passport
     * @param onResult Callback with the matching result
     */
    fun captureAndMatch(referenceImagePath: String, onResult: (AVMatchResult) -> Unit)

    /**
     * Reset the SDK state
     */
    fun reset()
}

/**
 * Result of face matching operation
 */
data class AVMatchResult(
    val processed: Boolean,
    val referenceIsValid: Boolean,
    val capturedIsLive: Boolean,
    val isSameSubject: Boolean,
    val capturedPath: String?
)