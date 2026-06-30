package com.example.startwatch.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Habit(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("total_time_spent") val totalTimeSpent: Long = 0L,
    @SerialName("daily_goal_ms") val dailyGoalMs: Long = 0L,
    @SerialName("icon_name") val iconName: String = "ic_timer"
)
