package com.example.mood

import com.google.mlkit.vision.face.Face

data class FaceGraphicInfo(
    val faces: List<Face> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val isFrontCamera: Boolean = true
)