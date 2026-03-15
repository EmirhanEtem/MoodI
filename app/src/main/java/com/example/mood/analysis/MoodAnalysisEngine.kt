package com.example.mood.analysis

import android.util.Log
import com.example.mood.WakeMood
import java.util.Calendar
import java.util.Locale

/**
 * Central mood analysis engine that combines face, voice, text, and behavioral inputs
 * into a multi-dimensional mood score and maps it to one of 12 mood categories.
 */
class MoodAnalysisEngine {

    companion object {
        private const val TAG = "MoodAnalysisEngine"

        // Weights for each analysis source
        private const val FACE_WEIGHT = 0.30f
        private const val VOICE_TONE_WEIGHT = 0.25f
        private const val TEXT_SENTIMENT_WEIGHT = 0.25f
        private const val BEHAVIORAL_WEIGHT = 0.20f
    }

    /**
     * Main analysis function — combines all inputs and returns the best-matching WakeMood
     * along with detailed 5D dimension scores.
     */
    fun analyze(input: MoodAnalysisInput): Pair<WakeMood, MoodDimension> {
        val faceScores = analyzeFace(input.faceResult)
        val voiceToneScores = analyzeVoiceTone(input.voiceToneResult)
        val textScores = analyzeText(input.transcribedText)
        val behavioralScores = analyzeBehavioral(input.snoozeCount, input.hourOfDay)

        Log.d(TAG, "Face scores: $faceScores")
        Log.d(TAG, "Voice tone scores: $voiceToneScores")
        Log.d(TAG, "Text scores: $textScores")
        Log.d(TAG, "Behavioral scores: $behavioralScores")

        // Determine actual weights based on what data is available
        var totalWeight = BEHAVIORAL_WEIGHT // Always available
        var faceW = 0f; var voiceW = 0f; var textW = 0f

        if (input.faceResult.faceDetected) {
            faceW = FACE_WEIGHT; totalWeight += faceW
        }
        if (input.voiceToneResult.isAnalyzed) {
            voiceW = VOICE_TONE_WEIGHT; totalWeight += voiceW
        }
        if (!input.transcribedText.isNullOrBlank()) {
            textW = TEXT_SENTIMENT_WEIGHT; totalWeight += textW
        }

        // Normalize weights
        if (totalWeight > 0) {
            faceW /= totalWeight; voiceW /= totalWeight; textW /= totalWeight
            val behavW = BEHAVIORAL_WEIGHT / totalWeight

            val combined = MoodDimension(
                energy = clamp(faceScores.energy * faceW + voiceToneScores.energy * voiceW + textScores.energy * textW + behavioralScores.energy * behavW),
                stress = clamp(faceScores.stress * faceW + voiceToneScores.stress * voiceW + textScores.stress * textW + behavioralScores.stress * behavW),
                positivity = clamp(faceScores.positivity * faceW + voiceToneScores.positivity * voiceW + textScores.positivity * textW + behavioralScores.positivity * behavW),
                focus = clamp(faceScores.focus * faceW + voiceToneScores.focus * voiceW + textScores.focus * textW + behavioralScores.focus * behavW),
                social = clamp(faceScores.social * faceW + voiceToneScores.social * voiceW + textScores.social * textW + behavioralScores.social * behavW)
            )

            Log.d(TAG, "Combined dimensions: $combined")
            val mood = WakeMood.fromDimensions(combined.energy, combined.stress, combined.positivity, combined.focus, combined.social)
            Log.d(TAG, "Determined mood: ${mood.name} (${mood.description})")
            return Pair(mood, combined)
        }

        return Pair(WakeMood.NEUTRAL, MoodDimension.DEFAULT)
    }

    private fun analyzeFace(face: FaceAnalysisResult): MoodDimension {
        if (!face.faceDetected) return MoodDimension.DEFAULT

        val smile = face.smileProbability
        val eyeOpen = face.averageEyeOpen

        // Energy: eyes open + some smile = alert and energetic
        val energy = clamp((eyeOpen * 60f) + (smile * 40f))

        // Stress: low eye open + head tilt could indicate tension
        val headTension = (kotlin.math.abs(face.headEulerAngleX) + kotlin.math.abs(face.headEulerAngleZ)) / 60f
        val stress = clamp((1f - smile) * 40f + headTension * 30f + (1f - eyeOpen) * 30f)

        // Positivity: strong correlation with smile
        val positivity = clamp(smile * 80f + eyeOpen * 20f)

        // Focus: eyes open + forward facing
        val forwardFacing = 1f - (kotlin.math.abs(face.headEulerAngleY) / 45f).coerceIn(0f, 1f)
        val focus = clamp(eyeOpen * 50f + forwardFacing * 50f)

        // Social: smile + eye contact (forward looking)
        val social = clamp(smile * 60f + forwardFacing * 25f + eyeOpen * 15f)

        return MoodDimension(energy, stress, positivity, focus, social)
    }

    private fun analyzeVoiceTone(voiceTone: VoiceToneResult): MoodDimension {
        if (!voiceTone.isAnalyzed) return MoodDimension.DEFAULT

        return MoodDimension(
            energy = voiceTone.estimatedEnergy,
            stress = voiceTone.estimatedStress,
            positivity = voiceTone.estimatedPositivity,
            focus = clamp(60f - voiceTone.pauseRatio * 30f + (1f - voiceTone.pitchVariance.coerceIn(0f, 1f)) * 30f),
            social = clamp(voiceTone.estimatedEnergy * 0.5f + voiceTone.estimatedPositivity * 0.3f + (1f - voiceTone.pauseRatio) * 20f)
        )
    }

    private fun analyzeText(text: String?): MoodDimension {
        if (text.isNullOrBlank()) return MoodDimension.DEFAULT
        val lower = text.lowercase(Locale("tr", "TR"))

        // Sentiment keyword mapping with multi-dimensional scoring
        data class KeywordEffect(val energy: Float, val stress: Float, val positivity: Float, val focus: Float, val social: Float)

        val keywordMap = mapOf(
            // Very positive
            "harika" to KeywordEffect(85f, 10f, 95f, 70f, 80f),
            "mükemmel" to KeywordEffect(90f, 10f, 95f, 75f, 80f),
            "çok iyi" to KeywordEffect(80f, 15f, 90f, 70f, 75f),
            "süper" to KeywordEffect(85f, 10f, 90f, 70f, 80f),
            "muhteşem" to KeywordEffect(90f, 10f, 95f, 75f, 85f),
            "enerjiğiyim" to KeywordEffect(95f, 15f, 80f, 75f, 70f),
            "enerjik" to KeywordEffect(95f, 15f, 80f, 75f, 70f),
            "mutlu" to KeywordEffect(75f, 10f, 95f, 60f, 85f),
            "neşeli" to KeywordEffect(80f, 10f, 90f, 55f, 90f),
            "heyecanlı" to KeywordEffect(85f, 30f, 85f, 60f, 80f),

            // Moderate positive
            "iyiyim" to KeywordEffect(60f, 25f, 70f, 60f, 60f),
            "fena değil" to KeywordEffect(55f, 30f, 60f, 55f, 55f),
            "idare eder" to KeywordEffect(45f, 35f, 50f, 50f, 45f),
            "normal" to KeywordEffect(50f, 30f, 55f, 55f, 50f),

            // Calm/reflective
            "sakin" to KeywordEffect(45f, 15f, 70f, 65f, 45f),
            "huzurlu" to KeywordEffect(50f, 10f, 80f, 70f, 50f),
            "dingin" to KeywordEffect(40f, 10f, 75f, 70f, 40f),
            "düşünceli" to KeywordEffect(45f, 25f, 55f, 80f, 30f),

            // Creative/inspired
            "ilhamlı" to KeywordEffect(70f, 15f, 85f, 80f, 55f),
            "yaratıcı" to KeywordEffect(70f, 20f, 80f, 75f, 55f),
            "ilham" to KeywordEffect(70f, 15f, 85f, 80f, 55f),

            // Tired/sleepy
            "yorgun" to KeywordEffect(15f, 40f, 35f, 25f, 30f),
            "yorgunum" to KeywordEffect(15f, 40f, 35f, 25f, 30f),
            "uykulu" to KeywordEffect(10f, 25f, 40f, 15f, 30f),
            "uyumak istiyorum" to KeywordEffect(5f, 30f, 30f, 10f, 20f),
            "bitkin" to KeywordEffect(8f, 60f, 20f, 15f, 15f),
            "tükenmiş" to KeywordEffect(5f, 75f, 15f, 10f, 10f),

            // Negative
            "kötü" to KeywordEffect(30f, 55f, 20f, 30f, 25f),
            "kötüyüm" to KeywordEffect(25f, 55f, 20f, 30f, 25f),
            "berbat" to KeywordEffect(20f, 65f, 10f, 20f, 20f),
            "istemiyorum" to KeywordEffect(20f, 50f, 20f, 20f, 15f),

            // Anxious/worried
            "endişeli" to KeywordEffect(55f, 80f, 25f, 35f, 35f),
            "kaygılı" to KeywordEffect(55f, 80f, 25f, 35f, 35f),
            "tedirgin" to KeywordEffect(50f, 70f, 30f, 40f, 35f),

            // Angry/frustrated
            "sinirli" to KeywordEffect(65f, 85f, 15f, 30f, 20f),
            "sinirliyim" to KeywordEffect(65f, 85f, 15f, 30f, 20f),
            "kızgın" to KeywordEffect(70f, 85f, 10f, 25f, 15f),
            "kızgınım" to KeywordEffect(70f, 85f, 10f, 25f, 15f),
            "stresliyim" to KeywordEffect(55f, 90f, 20f, 30f, 25f),
            "stresli" to KeywordEffect(55f, 90f, 20f, 30f, 25f),
            "gergin" to KeywordEffect(55f, 80f, 20f, 30f, 20f),
            "gerginim" to KeywordEffect(55f, 80f, 20f, 30f, 20f),

            // Sad
            "üzgün" to KeywordEffect(20f, 50f, 10f, 25f, 25f),
            "üzgünüm" to KeywordEffect(20f, 50f, 10f, 25f, 25f),
            "mutsuz" to KeywordEffect(25f, 50f, 10f, 25f, 25f),
            "depresif" to KeywordEffect(10f, 60f, 5f, 10f, 10f),
            "umutsuz" to KeywordEffect(10f, 65f, 5f, 10f, 10f)
        )

        var matchCount = 0
        var totalEnergy = 0f; var totalStress = 0f; var totalPositivity = 0f
        var totalFocus = 0f; var totalSocial = 0f

        for ((keyword, effect) in keywordMap) {
            if (lower.contains(keyword)) {
                matchCount++
                totalEnergy += effect.energy
                totalStress += effect.stress
                totalPositivity += effect.positivity
                totalFocus += effect.focus
                totalSocial += effect.social
            }
        }

        return if (matchCount > 0) {
            MoodDimension(
                energy = totalEnergy / matchCount,
                stress = totalStress / matchCount,
                positivity = totalPositivity / matchCount,
                focus = totalFocus / matchCount,
                social = totalSocial / matchCount
            )
        } else {
            MoodDimension.DEFAULT
        }
    }

    private fun analyzeBehavioral(snoozeCount: Int, hourOfDay: Int): MoodDimension {
        // Snooze impacts
        val snoozeEnergy = clamp(80f - snoozeCount * 20f)
        val snoozeStress = clamp(20f + snoozeCount * 12f)
        val snoozeFocus = clamp(70f - snoozeCount * 15f)

        // Time of day impacts
        val timeEnergy = when (hourOfDay) {
            in 5..8 -> 55f  // Early morning
            in 9..11 -> 75f // Late morning peak
            in 12..13 -> 60f // Post lunch dip
            in 14..16 -> 65f // Afternoon
            in 17..19 -> 55f // Evening
            in 20..22 -> 40f // Night
            else -> 25f      // Late night
        }

        return MoodDimension(
            energy = clamp((snoozeEnergy + timeEnergy) / 2f),
            stress = snoozeStress,
            positivity = clamp(65f - snoozeCount * 10f),
            focus = clamp((snoozeFocus + timeEnergy * 0.5f) / 1.5f),
            social = clamp(55f - snoozeCount * 5f)
        )
    }

    private fun clamp(value: Float): Float = value.coerceIn(0f, 100f)
}
