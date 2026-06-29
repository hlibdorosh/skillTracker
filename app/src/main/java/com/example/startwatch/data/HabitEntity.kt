package com.example.startwatch.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import java.time.Instant

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    val id: String, // UUID generated locally
    val name: String,
    val createdAt: String,
    val totalTimeSpent: Long = 0L,
    val dailyGoalMs: Long = 0L,
    val syncStatus: String = "SYNCED", // SYNCED, PENDING_INSERT, PENDING_UPDATE
    val userId: String? = null // For future Google Auth
)

fun HabitEntity.toDomainModel() = Habit(
    id = this.id,
    name = this.name,
    createdAt = this.createdAt,
    totalTimeSpent = this.totalTimeSpent,
    dailyGoalMs = this.dailyGoalMs
)

fun Habit.toEntity(syncStatus: String = "SYNCED", userId: String? = null) = HabitEntity(
    id = this.id ?: UUID.randomUUID().toString(),
    name = this.name,
    createdAt = this.createdAt ?: Instant.now().toString(),
    totalTimeSpent = this.totalTimeSpent,
    dailyGoalMs = this.dailyGoalMs,
    syncStatus = syncStatus,
    userId = userId
)

data class HabitWithProgress(
    val id: String,
    val name: String,
    val createdAt: String,
    val totalTimeSpent: Long,
    val dailyGoalMs: Long,
    val todayTimeSpent: Long
)
