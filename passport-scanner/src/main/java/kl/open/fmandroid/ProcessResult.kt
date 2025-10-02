package kl.open.fmandroid

/**
 * Original ProcessResult data class matching the native library expectations
 */
data class ProcessResult(
    val livenessChecked: Boolean,
    val isLive: Boolean,
    val faceDetected: Boolean,
    val embeddingExtracted: Boolean,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProcessResult

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