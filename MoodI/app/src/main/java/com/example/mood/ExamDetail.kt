package com.example.mood

import java.util.UUID

data class ExamDetail(
    val id: String = UUID.randomUUID().toString(),
    val courseName: String,
    val examDate: String
)