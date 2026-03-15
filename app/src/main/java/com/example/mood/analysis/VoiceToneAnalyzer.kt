package com.example.mood.analysis

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Voice Tone Analyzer - Extracts audio features from microphone input
 * to estimate energy, stress, and mood without requiring an external ML model.
 *
 * Analyzes:
 * - Amplitude/Energy (RMS) → overall vocal energy
 * - Speaking Rate (from transcription + duration) → excitement vs fatigue
 * - Pitch Variance (autocorrelation) → emotional expressiveness
 * - Pause Ratio (silence detection) → confidence/fatigue indicator
 */
class VoiceToneAnalyzer {

    companion object {
        private const val TAG = "VoiceToneAnalyzer"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val ANALYSIS_DURATION_MS = 5000L // 5 seconds of audio
        private const val SILENCE_THRESHOLD = 500 // Amplitude threshold for silence
    }

    /**
     * Record audio and analyze voice tone features.
     * Call this before/during speech recognition.
     * Returns VoiceToneResult with extracted features and estimated mood dimensions.
     */
    @Suppress("MissingPermission")
    suspend fun analyzeVoiceTone(
        durationMs: Long = ANALYSIS_DURATION_MS,
        transcribedText: String? = null
    ): VoiceToneResult = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(SAMPLE_RATE * 2)

        var audioRecord: AudioRecord? = null
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                return@withContext VoiceToneResult()
            }

            val totalSamples = (SAMPLE_RATE * durationMs / 1000).toInt()
            val audioData = ShortArray(totalSamples)
            var samplesRead = 0

            audioRecord.startRecording()
            Log.d(TAG, "Started recording for voice tone analysis")

            val startTime = System.currentTimeMillis()
            while (samplesRead < totalSamples && System.currentTimeMillis() - startTime < durationMs + 1000) {
                val remaining = totalSamples - samplesRead
                val toRead = minOf(remaining, bufferSize / 2)
                val read = audioRecord.read(audioData, samplesRead, toRead)
                if (read > 0) samplesRead += read
                else break
            }

            audioRecord.stop()
            Log.d(TAG, "Recorded $samplesRead samples")

            if (samplesRead < SAMPLE_RATE) {
                Log.w(TAG, "Too few samples recorded: $samplesRead")
                return@withContext VoiceToneResult()
            }

            val actualData = audioData.copyOf(samplesRead)
            analyzeAudioFeatures(actualData, durationMs, transcribedText)

        } catch (e: Exception) {
            Log.e(TAG, "Voice tone analysis error: ${e.message}", e)
            VoiceToneResult()
        } finally {
            try {
                audioRecord?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing AudioRecord", e)
            }
        }
    }

    private fun analyzeAudioFeatures(
        samples: ShortArray,
        durationMs: Long,
        transcribedText: String?
    ): VoiceToneResult {
        // 1. Calculate RMS (Root Mean Square) - Average energy
        var sumSquares = 0.0
        var peak = 0
        for (sample in samples) {
            val abs = abs(sample.toInt())
            sumSquares += sample.toDouble() * sample.toDouble()
            if (abs > peak) peak = abs
        }
        val rms = sqrt(sumSquares / samples.size).toFloat()
        val normalizedRMS = (rms / 16000f).coerceIn(0f, 1f)
        val normalizedPeak = (peak / 32767f).coerceIn(0f, 1f)

        // 2. Calculate pause ratio (frames below silence threshold)
        val frameSize = SAMPLE_RATE / 50 // 20ms frames
        var silentFrames = 0
        var totalFrames = 0
        for (i in 0 until samples.size - frameSize step frameSize) {
            totalFrames++
            var frameEnergy = 0.0
            for (j in i until i + frameSize) {
                frameEnergy += abs(samples[j].toInt())
            }
            frameEnergy /= frameSize
            if (frameEnergy < SILENCE_THRESHOLD) silentFrames++
        }
        val pauseRatio = if (totalFrames > 0) silentFrames.toFloat() / totalFrames else 0.5f

        // 3. Estimate speaking rate from transcription
        val wordCount = transcribedText?.split("\\s+".toRegex())?.filter { it.isNotBlank() }?.size ?: 0
        val durationMinutes = durationMs / 60000f
        val speakingRateWPM = if (durationMinutes > 0) wordCount / durationMinutes else 0f

        // 4. Pitch variance estimation using zero-crossing rate
        var zeroCrossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] > 0 && samples[i - 1] <= 0) || (samples[i] <= 0 && samples[i - 1] > 0)) {
                zeroCrossings++
            }
        }
        val zeroCrossingRate = zeroCrossings.toFloat() / samples.size * SAMPLE_RATE
        // Calculate variance of short-term ZCR
        val segmentSize = SAMPLE_RATE / 4 // 250ms segments
        val segmentZCRs = mutableListOf<Float>()
        for (i in 0 until samples.size - segmentSize step segmentSize) {
            var segZC = 0
            for (j in i + 1 until i + segmentSize) {
                if ((samples[j] > 0 && samples[j - 1] <= 0) || (samples[j] <= 0 && samples[j - 1] > 0)) {
                    segZC++
                }
            }
            segmentZCRs.add(segZC.toFloat() / segmentSize * SAMPLE_RATE)
        }
        val meanZCR = if (segmentZCRs.isNotEmpty()) segmentZCRs.average().toFloat() else zeroCrossingRate
        val pitchVariance = if (segmentZCRs.size > 1) {
            val variance = segmentZCRs.map { (it - meanZCR) * (it - meanZCR) }.average().toFloat()
            (sqrt(variance.toDouble()) / (meanZCR + 1f)).toFloat().coerceIn(0f, 2f)
        } else 0f

        Log.d(TAG, "Audio features - RMS: $normalizedRMS, Peak: $normalizedPeak, " +
                "PauseRatio: $pauseRatio, SpeakingRate: $speakingRateWPM WPM, " +
                "PitchVariance: $pitchVariance, ZCR: $zeroCrossingRate")

        // 5. Map features to mood dimensions
        val estimatedEnergy = mapToEnergy(normalizedRMS, normalizedPeak, speakingRateWPM, pauseRatio)
        val estimatedStress = mapToStress(normalizedPeak, pitchVariance, speakingRateWPM)
        val estimatedPositivity = mapToPositivity(pitchVariance, normalizedRMS, pauseRatio)

        return VoiceToneResult(
            averageAmplitude = normalizedRMS,
            peakAmplitude = normalizedPeak,
            speakingRateWPM = speakingRateWPM,
            pauseRatio = pauseRatio,
            pitchVariance = pitchVariance,
            estimatedEnergy = estimatedEnergy,
            estimatedStress = estimatedStress,
            estimatedPositivity = estimatedPositivity,
            isAnalyzed = true
        )
    }

    private fun mapToEnergy(rms: Float, peak: Float, wpm: Float, pauseRatio: Float): Float {
        // High RMS + high peak + fast speaking + low pauses = high energy
        val volumeScore = (rms * 50f + peak * 30f)
        val speedScore = (wpm / 200f * 40f).coerceIn(0f, 40f) // 200 WPM = max
        val continuityScore = ((1f - pauseRatio) * 30f)
        return (volumeScore + speedScore + continuityScore).coerceIn(5f, 100f)
    }

    private fun mapToStress(peak: Float, pitchVariance: Float, wpm: Float): Float {
        // High peak amplitude + high pitch variance + fast speaking = stress
        val loudnessStress = peak * 35f
        val variabilityStress = pitchVariance * 30f
        val speedStress = (wpm / 200f * 25f).coerceIn(0f, 25f)
        return (loudnessStress + variabilityStress + speedStress + 10f).coerceIn(5f, 95f)
    }

    private fun mapToPositivity(pitchVariance: Float, rms: Float, pauseRatio: Float): Float {
        // Moderate pitch variance + good volume + low pauses = positivity
        val expressiveness = if (pitchVariance in 0.1f..0.8f) 40f else 20f
        val confidence = rms * 30f + (1f - pauseRatio) * 20f
        return (expressiveness + confidence + 10f).coerceIn(5f, 95f)
    }
}
