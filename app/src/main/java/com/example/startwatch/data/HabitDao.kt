package com.example.startwatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabitsFlow(): Flow<List<HabitEntity>>

    @Query("""
        SELECT h.id, h.name, h.createdAt, h.totalTimeSpent, h.dailyGoalMs, h.iconName, h.type,
               COALESCE(SUM(s.durationMs), 0) as todayTimeSpent
        FROM habits h
        LEFT JOIN habit_sessions s ON h.id = s.habitId AND s.dateStarted = :todayDate
        GROUP BY h.id
        ORDER BY h.createdAt DESC
    """)
    fun getHabitsWithTodayProgressFlow(todayDate: String): Flow<List<HabitWithProgress>>

    @Query("SELECT * FROM habits")
    fun getAllHabits(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    fun getHabitById(habitId: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE syncStatus != 'SYNCED'")
    fun getUnsyncedHabits(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHabit(habit: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHabits(habits: List<HabitEntity>): List<Long>

    @Update
    fun updateHabit(habit: HabitEntity): Int

    @Query("UPDATE habits SET totalTimeSpent = totalTimeSpent + :durationMs WHERE id = :habitId")
    fun addTimeSpent(habitId: String, durationMs: Long): Int

    @Query("UPDATE habits SET syncStatus = :status WHERE id = :id")
    fun updateHabitSyncStatus(id: String, status: String): Int

    @Query("UPDATE habits SET name = :newName, dailyGoalMs = :newGoalMs, iconName = :newIconName, type = :newType, syncStatus = :status WHERE id = :id")
    fun updateHabitDetails(id: String, newName: String, newGoalMs: Long, newIconName: String, newType: String, status: String = "PENDING_UPDATE"): Int

    @Query("DELETE FROM habits WHERE id = :id")
    fun deleteHabit(id: String)

    // Sessions
    @Query("SELECT * FROM habit_sessions WHERE habitId = :habitId ORDER BY dateStarted DESC")
    fun getSessionsForHabit(habitId: String): List<HabitSessionEntity>

    @Query("SELECT * FROM habit_sessions WHERE syncStatus != 'SYNCED'")
    fun getUnsyncedSessions(): List<HabitSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: HabitSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSessions(sessions: List<HabitSessionEntity>): List<Long>

    @Query("UPDATE habit_sessions SET syncStatus = :status WHERE id = :id")
    fun updateSessionSyncStatus(id: String, status: String): Int
}
