package eu.europa.ec.passportscanner.face

/**
 * Interface for Age Verification Face Matching SDK
 * Provides face liveness detection and matching capabilities for passport verification
 */
interface AVFaceMatchSDK {

    /**
     * Initialize the SDK with configuration
     * @param configJson JSON configuration string containing model paths and settings
     * @return true if initialization was successful, false otherwise
     */
    fun init(configJson: String): Boolean

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