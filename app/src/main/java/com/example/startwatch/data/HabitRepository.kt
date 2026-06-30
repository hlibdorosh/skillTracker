package com.example.startwatch.data

import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class HabitRepository(private val habitDao: HabitDao) {
    private val client = SupabaseManager.client

    // Returns a Flow that the UI can observe. It instantly emits local database changes!
    fun getHabitsFlow(): Flow<List<HabitWithProgress>> {
        val todayStr = java.time.LocalDate.now().toString()
        return habitDao.getHabitsWithTodayProgressFlow(todayStr)
    }

    suspend fun addHabit(name: String, dailyGoalMs: Long, iconName: String, type: String = "TIME") = withContext(Dispatchers.IO) {
        val habit = HabitEntity(
            id = UUID.randomUUID().toString(),
            userId = "local_user",
            name = name,
            createdAt = java.time.Instant.now().toString(),
            dailyGoalMs = dailyGoalMs,
            iconName = iconName,
            type = type,
            syncStatus = "PENDING_INSERT"
        )
        habitDao.insertHabit(habit)
        syncPendingHabits()
    }

    suspend fun addSessionAndUpdateTotal(habitId: String, durationMs: Long, dateStarted: String) = withContext(Dispatchers.IO) {
        // Fetch current habit goal to save as historical record
        val habit = habitDao.getHabitById(habitId)
        val currentGoalMs = habit?.dailyGoalMs ?: 0L

        val session = HabitSessionEntity(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            durationMs = durationMs,
            dateStarted = dateStarted,
            goalMs = currentGoalMs,
            syncStatus = "PENDING_INSERT"
        )
        habitDao.insertSession(session)
        habitDao.addTimeSpent(habitId, durationMs)

        // Try to sync
        syncPendingHabits()
    }

    suspend fun editHabit(habitId: String, newName: String, newGoalMs: Long, newIconName: String, newType: String = "TIME") = withContext(Dispatchers.IO) {
        habitDao.updateHabitDetails(habitId, newName, newGoalMs, newIconName, newType)
        // Try to push the update directly
        try {
            client.postgrest["habits"].update({
                set("name", newName)
                set("daily_goal_ms", newGoalMs)
                set("icon_name", newIconName)
                set("type", newType)
            }) {
                filter { eq("id", habitId) }
            }
            habitDao.updateHabitSyncStatus(habitId, "SYNCED")
        } catch (e: Exception) {
            Log.e("HabitRepository", "Failed to sync editHabit: ${e.message}")
        }
    }

    suspend fun deleteHabit(habitId: String) = withContext(Dispatchers.IO) {
        habitDao.deleteHabit(habitId)
        try {
            client.postgrest["habits"].delete {
                filter {
                    eq("id", habitId)
                }
            }
        } catch (e: Exception) {
            Log.e("HabitRepository", "Failed to delete remote habit: ${e.message}")
        }
    }

    // A basic SyncManager function to push pending data and pull fresh data
    suspend fun syncPendingHabits() = withContext(Dispatchers.IO) {
        try {
            // PUSH
            val pendingHabits = habitDao.getUnsyncedHabits()
            for (habit in pendingHabits) {
                if (habit.syncStatus == "PENDING_INSERT") {
                    // Send to Supabase (using domain model to match exact json schema)
                    val domainModel = habit.toDomainModel()
                    client.postgrest["habits"].insert(domainModel)
                    
                    // Mark as SYNCED locally
                    habitDao.updateHabitSyncStatus(habit.id, "SYNCED")
                }
            }
            
            // PULL
            val remoteHabits = client.postgrest["habits"].select().decodeList<Habit>()
            val remoteEntities = remoteHabits.map { it.toEntity(syncStatus = "SYNCED") }
            habitDao.insertHabits(remoteEntities)
            
        } catch (e: Exception) {
            Log.e("HabitRepository", "Sync failed. Will retry later. Error: ${e.message}")
        }
    }
}
