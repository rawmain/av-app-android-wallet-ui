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
 * Singleton holder for camera callback data during face matching operation
 * Used to pass data between the SDK and camera activity
 */
object AVCameraCallbackHolder {
    private const val TAG = "AVCameraCallbackHolder"

    var referenceResult: AVProcessResult? = null
        set(value) {
            android.util.Log.d(TAG, "referenceResult set: ${value != null} (embeddingSize: ${value?.embedding?.size ?: 0})")
            field = value
        }

    var decisor: AVDecisor? = null
        set(value) {
            android.util.Log.d(TAG, "decisor set: ${value != null} (samples: ${value?.getSampleCount() ?: 0})")
            field = value
        }

    var onFinalResult: ((AVMatchResult) -> Unit)? = null
        set(value) {
            android.util.Log.d(TAG, "onFinalResult callback set: ${value != null}")
            field = value
        }

    /**
     * Reset all callback data
     */
    fun reset() {
        android.util.Log.d(TAG, "reset: Clearing all callback data")
        referenceResult = null
        decisor = null
        onFinalResult = null
        android.util.Log.d(TAG, "reset: All callback data cleared")
    }

    /**
     * Check if all required data is set
     */
    fun isReady(): Boolean {
        val ready = referenceResult != null && decisor != null && onFinalResult != null
        android.util.Log.d(TAG, "isReady: $ready (ref: ${referenceResult != null}, decisor: ${decisor != null}, callback: ${onFinalResult != null})")
        return ready
    }

    /**
     * Trigger the final result callback
     */
    fun triggerCallback(result: AVMatchResult) {
        android.util.Log.d(TAG, "triggerCallback: Attempting to trigger final callback with result: $result")
        onFinalResult?.let { callback ->
            android.util.Log.d(TAG, "triggerCallback: Callback found, invoking...")
            callback(result)
            android.util.Log.d(TAG, "triggerCallback: Callback invoked successfully")
        } ?: run {
            android.util.Log.e(TAG, "triggerCallback: No callback found! onFinalResult is null")
        }
    }
}