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