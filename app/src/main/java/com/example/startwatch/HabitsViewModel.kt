package com.example.startwatch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.startwatch.data.AppDatabase
import com.example.startwatch.data.HabitEntity
import com.example.startwatch.data.HabitWithProgress
import com.example.startwatch.data.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HabitsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = HabitRepository(database.habitDao())

    private val _habits = MutableStateFlow<List<HabitWithProgress>>(emptyList())
    val habits: StateFlow<List<HabitWithProgress>> = _habits
    
    private var allHabits = listOf<HabitWithProgress>()

    init {
        // Collect local database changes instantly
        viewModelScope.launch {
            repository.getHabitsFlow().collectLatest { entities ->
                allHabits = entities
                _habits.value = entities
            }
        }
        
        // Trigger a background sync on startup
        viewModelScope.launch {
            repository.syncPendingHabits()
        }
    }

    fun addHabit(name: String, dailyGoalMs: Long, iconName: String) {
        viewModelScope.launch {
            try {
                repository.addHabit(name, dailyGoalMs, iconName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun editHabit(habitId: String, name: String, dailyGoalMs: Long, iconName: String) {
        viewModelScope.launch {
            try {
                repository.editHabit(habitId, name, dailyGoalMs, iconName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            try {
                repository.deleteHabit(habitId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchHabits(query: String) {
        if (query.isBlank()) {
            _habits.value = allHabits
        } else {
            _habits.value = allHabits.filter { 
                it.name.contains(query, ignoreCase = true) 
            }
        }
    }
}
