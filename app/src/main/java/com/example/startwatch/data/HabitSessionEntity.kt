package com.example.startwatch.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import java.time.Instant

@Entity(tableName = "habit_sessions")
data class HabitSessionEntity(
    @PrimaryKey
    val id: String,
    val habitId: String,
    val durationMs: Long,
    val dateStarted: String,
    val syncStatus: String = "SYNCED",
    val userId: String? = null
)

fun HabitSessionEntity.toDomainModel() = HabitSession(
    id = this.id,
    habitId = this.habitId,
    durationMs = this.durationMs,
    dateStarted = this.dateStarted
)

fun HabitSession.toEntity(syncStatus: String = "SYNCED", userId: String? = null) = HabitSessionEntity(
    id = this.id ?: UUID.randomUUID().toString(),
    habitId = this.habitId,
    durationMs = this.durationMs,
    dateStarted = this.dateStarted,
    syncStatus = syncStatus,
    userId = userId
)
