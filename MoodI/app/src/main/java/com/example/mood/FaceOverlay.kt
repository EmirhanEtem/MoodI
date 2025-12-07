package com.example.mood

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark // Ensure this import for explicit typing if needed

@Composable
fun FaceOverlay(
    faceGraphicInfo: FaceGraphicInfo,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val viewWidth = size.width
        val viewHeight = size.height

        if (faceGraphicInfo.faces.isEmpty() || faceGraphicInfo.imageWidth == 0 || faceGraphicInfo.imageHeight == 0) {
            return@Canvas
        }

        val imageAspectRatio = faceGraphicInfo.imageWidth.toFloat() / faceGraphicInfo.imageHeight.toFloat()
        val viewAspectRatio = viewWidth / viewHeight

        val scaleFactor: Float
        val postScaleWidthOffset: Float
        val postScaleHeightOffset: Float

        if (imageAspectRatio > viewAspectRatio) {
            scaleFactor = viewWidth / faceGraphicInfo.imageWidth.toFloat()
            postScaleWidthOffset = 0f
            val scaledImageHeight = faceGraphicInfo.imageHeight.toFloat() * scaleFactor
            postScaleHeightOffset = (viewHeight - scaledImageHeight) / 2f
        } else {
            scaleFactor = viewHeight / faceGraphicInfo.imageHeight.toFloat()
            postScaleHeightOffset = 0f
            val scaledImageWidth = faceGraphicInfo.imageWidth.toFloat() * scaleFactor
            postScaleWidthOffset = (viewWidth - scaledImageWidth) / 2f
        }

        val allContourTypes = listOf(
            FaceContour.FACE, FaceContour.LEFT_EYEBROW_TOP, FaceContour.LEFT_EYEBROW_BOTTOM,
            FaceContour.RIGHT_EYEBROW_TOP, FaceContour.RIGHT_EYEBROW_BOTTOM, FaceContour.LEFT_EYE,
            FaceContour.RIGHT_EYE, FaceContour.UPPER_LIP_TOP, FaceContour.UPPER_LIP_BOTTOM,
            FaceContour.LOWER_LIP_TOP, FaceContour.LOWER_LIP_BOTTOM, FaceContour.NOSE_BRIDGE,
            FaceContour.NOSE_BOTTOM, FaceContour.LEFT_CHEEK, FaceContour.RIGHT_CHEEK
        )

        for (face in faceGraphicInfo.faces) {
            val landmarkPoints = mutableListOf<Offset>()
            face.allLandmarks.forEach { landmark: FaceLandmark -> // Explicit type
                val originalX = landmark.position.x
                val originalY = landmark.position.y
                var translatedX = originalX * scaleFactor
                val translatedY = originalY * scaleFactor
                if (faceGraphicInfo.isFrontCamera) {
                    translatedX = (faceGraphicInfo.imageWidth * scaleFactor) - translatedX
                }
                landmarkPoints.add(Offset(x = translatedX + postScaleWidthOffset, y = translatedY + postScaleHeightOffset))
            }
            if (landmarkPoints.isNotEmpty()) {
                drawPoints(points = landmarkPoints, pointMode = PointMode.Points, color = Color.Green, strokeWidth = 8f)
            }

            allContourTypes.forEach { contourType ->
                face.getContour(contourType)?.points?.let { points ->
                    val contourOffsets = points.map { point: android.graphics.PointF -> // Explicit type
                        var translatedX = point.x * scaleFactor
                        val translatedY = point.y * scaleFactor
                        if (faceGraphicInfo.isFrontCamera) {
                            translatedX = (faceGraphicInfo.imageWidth * scaleFactor) - translatedX
                        }
                        Offset(x = translatedX + postScaleWidthOffset, y = translatedY + postScaleHeightOffset)
                    }
                    if (contourOffsets.size > 1) {
                        for (i in 0 until contourOffsets.size - 1) {
                            drawLine(color = Color.Cyan, start = contourOffsets[i], end = contourOffsets[i + 1], strokeWidth = 2.dp.toPx())
                        }
                        // Optional: Close certain contours
                        if (contourType == FaceContour.FACE || contourType == FaceContour.LEFT_EYE || contourType == FaceContour.RIGHT_EYE ||
                            contourType == FaceContour.UPPER_LIP_TOP || contourType == FaceContour.UPPER_LIP_BOTTOM ||
                            contourType == FaceContour.LOWER_LIP_TOP || contourType == FaceContour.LOWER_LIP_BOTTOM) {
                            if (contourOffsets.isNotEmpty()) {
                                drawLine(color = Color.Cyan, start = contourOffsets.last(), end = contourOffsets.first(), strokeWidth = 2.dp.toPx())
                            }
                        }
                    }
                }
            }
        }
    }
}