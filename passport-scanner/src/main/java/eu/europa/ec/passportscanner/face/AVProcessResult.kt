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
 * Result of face processing operation containing liveness and embedding data
 */
data class AVProcessResult(
    val livenessChecked: Boolean,
    val isLive: Boolean,
    val faceDetected: Boolean,
    val embeddingExtracted: Boolean,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AVProcessResult

        if (livenessChecked != other.livenessChecked) return false
        if (isLive != other.isLive) return false
        if (faceDetected != other.faceDetected) return false
        if (embeddingExtracted != other.embeddingExtracted) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = livenessChecked.hashCode()
        result = 31 * result + isLive.hashCode()
        result = 31 * result + faceDetected.hashCode()
        result = 31 * result + embeddingExtracted.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}