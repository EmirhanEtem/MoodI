package com.example.mood

import java.util.UUID

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    var isCompleted: Boolean = false,
    val estimatedTimeMinutes: Int? = null,
    val difficulty: Int = 1,
    val priority: Int = 2,  // 1=high, 2=medium, 3=low
    val sortOrder: Int = 0,
    val scheduledTime: String? = null,
    val tags: List<String> = emptyList(),
    var subTasks: List<String> = emptyList(),
    var isLoadingSubTasks: Boolean = false,
    val detailPromptInstruction: String? = null,
    val spotifyTrackUriForDetail: String? = null,
    val albumCoverUrl: String? = null,
    val durationMs: Int? = null,
    val spotifyTrackWebUrl: String? = null
)