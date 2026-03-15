package com.example.mood

import android.content.Context
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.mood.analysis.FaceAnalysisResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Face Analyzer — Instant Capture Mode
 * Captures the current facial expression without requiring a smile.
 * Analyzes: smile probability, eye openness, head euler angles.
 */
class FaceAnalyzer(
    private val context: Context,
    private val onAnalysisComplete: (FaceAnalysisResult) -> Unit,
    private val onFacesDetected: (FaceGraphicInfo) -> Unit
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

    private val captureRequested = AtomicBoolean(false)
    private val analysisDone = AtomicBoolean(false)

    // Continuous face tracking variables
    private var latestFaceResult = FaceAnalysisResult()

    /**
     * Call this to capture the current frame's facial expression.
     * The next analyzed frame will be returned via onAnalysisComplete.
     */
    fun requestCapture() {
        if (!analysisDone.get()) {
            captureRequested.set(true)
            Log.d("FaceAnalyzer", "Capture requested - will analyze next frame")
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
                    val faceGraphicInfo = FaceGraphicInfo(
                        faces = detectedFaces,
                        imageWidth = inputImageWidth,
                        imageHeight = inputImageHeight,
                        isFrontCamera = true
                    )
                    onFacesDetected(faceGraphicInfo)

                    // Update latest face result for real-time display
                    if (detectedFaces.isNotEmpty()) {
                        val face = detectedFaces[0]
                        latestFaceResult = FaceAnalysisResult(
                            smileProbability = face.smilingProbability ?: 0f,
                            leftEyeOpenProbability = face.leftEyeOpenProbability ?: 0.5f,
                            rightEyeOpenProbability = face.rightEyeOpenProbability ?: 0.5f,
                            headEulerAngleX = face.headEulerAngleX,
                            headEulerAngleY = face.headEulerAngleY,
                            headEulerAngleZ = face.headEulerAngleZ,
                            faceDetected = true
                        )
                    } else {
                        latestFaceResult = FaceAnalysisResult(faceDetected = false)
                    }

                    // If capture was requested, deliver the result
                    if (captureRequested.get() && !analysisDone.get()) {
                        captureRequested.set(false)
                        analysisDone.set(true)
                        Log.d("FaceAnalyzer", "Instant capture complete: smile=${latestFaceResult.smileProbability}, " +
                                "eyeOpen=${latestFaceResult.averageEyeOpen}, " +
                                "headX=${latestFaceResult.headEulerAngleX}")
                        onAnalysisComplete(latestFaceResult)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FaceAnalyzer", "Face detection error: $e", e)
                    onFacesDetected(FaceGraphicInfo())
                    if (captureRequested.get() && !analysisDone.get()) {
                        captureRequested.set(false)
                        onAnalysisComplete(FaceAnalysisResult(faceDetected = false))
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            onFacesDetected(FaceGraphicInfo())
            imageProxy.close()
        }
    }

    /** Get the most recent face analysis result without capture */
    fun getLatestResult(): FaceAnalysisResult = latestFaceResult

    fun release() {
        detector.close()
        Log.d("FaceAnalyzer", "Released resources")
    }
}