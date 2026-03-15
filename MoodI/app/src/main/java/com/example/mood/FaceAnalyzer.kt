package com.example.mood

import android.content.Context
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class FaceAnalyzer(
    private val context: Context,
    private val onSmileDetectedAndSaved: () -> Unit, // Triggers camera close and TTS/Save
    private val onFacesDetected: (FaceGraphicInfo) -> Unit // Passes all face data for overlay & smile prob to VM
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
    )

    private var tts: TextToSpeech? = null
    private val smilingAnnouncedThisSession = AtomicBoolean(false)

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("tr", "TR")) // Explicitly Turkish
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("FaceAnalyzer_TTS", "Turkish language not supported or missing data.")
                } else {
                    Log.d("FaceAnalyzer_TTS", "TTS Initialized successfully for Turkish.")
                }
            } else {
                Log.e("FaceAnalyzer_TTS", "TTS Initialization Failed! Status: $status")
            }
        }
    }

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            val inputImageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
            val inputImageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

            detector.process(image)
                .addOnSuccessListener { detectedFaces ->
                    onFacesDetected(
                        FaceGraphicInfo(
                            faces = detectedFaces,
                            imageWidth = inputImageWidth,
                            imageHeight = inputImageHeight,
                            isFrontCamera = true
                        )
                    )

                    if (!smilingAnnouncedThisSession.get()) {
                        for (face in detectedFaces) {
                            val smileProb = face.smilingProbability ?: 0.0f
                            // Log.d("FaceAnalyzer", "Face Tracking ID: ${face.trackingId}, Smile Probability: $smileProb") // Can be noisy

                            if (smileProb > 0.7f) { // Smile threshold
                                Log.d("FaceDetection", "Gülümsüyor! Probability: $smileProb")
                                tts?.speak("Gülümsüyor", TextToSpeech.QUEUE_FLUSH, null, "smile_utterance_id")

                                smilingAnnouncedThisSession.set(true)
                                saveFaceImage(image, context) // Save image on strong smile
                                onSmileDetectedAndSaved()     // Signal to close camera etc.
                                break
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FaceDetection", "Yüz algılama hatası: $e", e)
                    onFacesDetected(FaceGraphicInfo())
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            onFacesDetected(FaceGraphicInfo())
            imageProxy.close()
        }
    }

    private fun saveFaceImage(image: InputImage, context: Context) {
        try {
            val bitmap = image.bitmapInternal ?: run {
                Log.e("FaceDetection", "Bitmap internal is null for saving.")
                return
            }
            val filename = "smile_${System.currentTimeMillis()}.jpg"
            val file = File(context.cacheDir, filename)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
            }
            Log.d("FaceDetection", "Yüz kaydedildi: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("FaceDetection", "Yüz kaydetme hatası", e)
        }
    }

    fun release() {
        detector.close()
        tts?.stop()
        tts?.shutdown()
        tts = null
        Log.d("FaceAnalyzer", "Released resources")
    }
}