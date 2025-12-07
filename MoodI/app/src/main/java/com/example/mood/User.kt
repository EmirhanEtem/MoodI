package com.example.mood

data class User(
    val username: String,
    val hashedPassword: String,
    val profession: String? = null,
    val hasUpcomingExams: Boolean? = null,
    val studentExams: List<ExamDetail> = emptyList(),
    val developerHasProjectDeadline: Boolean? = null,
    val developerProjectDetails: String? = null,
    val doctorHasUpcomingShift: Boolean? = null,
    val doctorShiftDetails: String? = null,
    val teacherHasUrgentTask: Boolean? = null,
    val teacherUrgentTaskDetails: String? = null,
    val artistHasDeadlineOrEvent: Boolean? = null,
    val artistEventDetails: String? = null,
    // Spotify token'ları
    val spotifyAccessToken: String? = null,
    val spotifyRefreshToken: String? = null,
    val spotifyTokenExpiresAt: Long? = null
)