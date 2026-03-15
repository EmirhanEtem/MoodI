package com.example.mood.analysis

/**
 * Represents the 5-dimensional mood analysis result.
 * Each dimension is scored from 0f to 100f.
 */
data class MoodDimension(
    val energy: Float = 50f,    // Physical and mental energy level
    val stress: Float = 35f,    // Stress and tension level
    val positivity: Float = 55f, // Overall positive/negative outlook
    val focus: Float = 55f,     // Concentration and clarity
    val social: Float = 50f     // Desire for social interaction
) {
    fun toList(): List<Float> = listOf(energy, stress, positivity, focus, social)

    companion object {
        val DIMENSION_LABELS = listOf("Enerji", "Stres", "Pozitiflik", "Odak", "Sosyallik")
        val DEFAULT = MoodDimension()
    }
}

/**
 * Result from face analysis
 */
data class FaceAnalysisResult(
    val smileProbability: Float = 0f,
    val leftEyeOpenProbability: Float = 0.5f,
    val rightEyeOpenProbability: Float = 0.5f,
    val headEulerAngleX: Float = 0f, // Pitch (nodding)
    val headEulerAngleY: Float = 0f, // Yaw (turning)
    val headEulerAngleZ: Float = 0f, // Roll (tilting)
    val faceDetected: Boolean = false
) {
    val averageEyeOpen: Float get() = (leftEyeOpenProbability + rightEyeOpenProbability) / 2f
}

/**
 * Result from voice tone analysis
 */
data class VoiceToneResult(
    val averageAmplitude: Float = 0f,     // Normalized 0-1
    val peakAmplitude: Float = 0f,        // Normalized 0-1
    val speakingRateWPM: Float = 0f,      // Words per minute
    val pauseRatio: Float = 0f,           // 0-1 ratio of silence
    val pitchVariance: Float = 0f,        // Variance in pitch
    val estimatedEnergy: Float = 50f,     // Derived 0-100
    val estimatedStress: Float = 35f,     // Derived 0-100
    val estimatedPositivity: Float = 55f, // Derived 0-100
    val isAnalyzed: Boolean = false
)

/**
 * Combined analysis input from all sources
 */
data class MoodAnalysisInput(
    val faceResult: FaceAnalysisResult = FaceAnalysisResult(),
    val voiceToneResult: VoiceToneResult = VoiceToneResult(),
    val transcribedText: String? = null,
    val snoozeCount: Int = 0,
    val hourOfDay: Int = 8
)
