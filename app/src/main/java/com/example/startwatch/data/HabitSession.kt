package com.example.startwatch.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HabitSession(
    @SerialName("id") val id: String? = null,
    @SerialName("habit_id") val habitId: String,
    @SerialName("date_started") val dateStarted: String,
    @SerialName("duration_ms") val durationMs: Long
)
