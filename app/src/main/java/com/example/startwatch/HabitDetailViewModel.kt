package com.example.startwatch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.startwatch.data.AppDatabase
import com.example.startwatch.data.HabitRepository
import com.example.startwatch.data.HabitSessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = HabitRepository(database.habitDao())

    private val _sessions = MutableStateFlow<List<HabitSessionEntity>>(emptyList())
    val sessions: StateFlow<List<HabitSessionEntity>> = _sessions

    // Map of Date String (yyyy-MM-dd) to Pair<TotalMs, GoalMs>
    private val _dailyTotals = MutableStateFlow<Map<String, Pair<Long, Long>>>(emptyMap())
    val dailyTotals: StateFlow<Map<String, Pair<Long, Long>>> = _dailyTotals

    fun loadSessions(habitId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Actually repository needs a way to get sessions
                // We will add getSessions(habitId) to repository next
                val data = database.habitDao().getSessionsForHabit(habitId)
                _sessions.value = data
                
                // Calculate daily totals and historical goals
                val totals = mutableMapOf<String, Pair<Long, Long>>()
                for (session in data) {
                    val currentPair = totals.getOrDefault(session.dateStarted, Pair(0L, session.goalMs))
                    val newTotal = currentPair.first + session.durationMs
                    totals[session.dateStarted] = Pair(newTotal, session.goalMs) // Keep latest session's goal as day's goal
                }
                _dailyTotals.value = totals
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveSession(habitId: String, durationMs: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())
                repository.addSessionAndUpdateTotal(habitId, durationMs, today)
                loadSessions(habitId) // Reload to update stats
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.deleteHabit(habitId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
