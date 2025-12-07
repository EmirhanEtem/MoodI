package com.example.mood

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onSmileDetectedAndSaved: () -> Unit, // This callback is still used by FaceAnalyzer to trigger camera close
    onFacesDetected: (FaceGraphicInfo) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val faceAnalyzer = remember(context, onSmileDetectedAndSaved, onFacesDetected) {
        FaceAnalyzer(context, onSmileDetectedAndSaved, onFacesDetected)
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("CameraPreview", "Disposing CameraPreview, shutting down executor and analyzer.")
            cameraExecutor.shutdown()
            faceAnalyzer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
            previewView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            startCamera(ctx, previewView, cameraExecutor, lifecycleOwner, faceAnalyzer)
            previewView
        },
        modifier = modifier
    )
}

private fun startCamera(
    context: Context,
    previewView: PreviewView,
    cameraExecutor: ExecutorService,
    lifecycleOwner: LifecycleOwner,
    analyzer: ImageAnalysis.Analyzer
) {
    Log.d("CameraPreview", "startCamera called.")
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        try {
            Log.d("CameraPreview", "CameraProviderFuture listener invoked.")
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            Log.d("CameraPreview", "Preview use case built.")

            val imageAnalyzerUseCase = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, analyzer)
                }
            Log.d("CameraPreview", "ImageAnalysis use case built and analyzer set.")

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzerUseCase
            )
            Log.d("CameraPreview", "Camera started and bound to lifecycle.")
        } catch (exc: Exception) {
            Log.e("CameraPreview", "Error in CameraProviderFuture listener or binding", exc)
        }
    }, ContextCompat.getMainExecutor(context))
}