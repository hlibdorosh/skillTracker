package com.example.startwatch

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.startwatch.databinding.FragmentSecondBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HabitDetailViewModel by viewModels()
    
    private var habitId: String = ""
    private var habitName: String = ""
    private var currentDailyGoalMs = 0L
    
    private var isRunning = false
    private var startTime = 0L
    private var todayTotalMs = 0L

    private lateinit var calendarAdapter: CalendarAdapter
    private var currentCalendarMonth = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        habitId = arguments?.getString("habitId") ?: return
        habitName = arguments?.getString("habitName") ?: "Habit Details"
        currentDailyGoalMs = arguments?.getLong("dailyGoalMs", 0L) ?: 0L
        
        binding.textHabitTitle.text = habitName
        updateDailyGoalText()

        viewModel.loadSessions(habitId)

        // Setup Stopwatch
        binding.buttonStartStop.setOnClickListener {
            if (isRunning) {
                showStopConfirmationDialog()
            } else {
                startTimer()
            }
        }

        // Setup Custom Calendar
        setupCustomCalendar()

        // Intercept Back Button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isRunning) {
                    showStopConfirmationDialog(isFromBackButton = true)
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Observe Data
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dailyTotals.collect { totalsMap ->
                    val totalMs = totalsMap.values.sumOf { it.first }
                    binding.textTotalStats.text = "Total time spent: ${formatTime(totalMs)}"
                    
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val todayStr = dateFormat.format(Date())
                    todayTotalMs = totalsMap[todayStr]?.first ?: 0L
                    
                    if (!isRunning) {
                        binding.stopwatch.text = formatTimeMs(todayTotalMs)
                    }
                    
                    refreshCalendarData()
                }
            }
        }
    }

    private fun setupCustomCalendar() {
        calendarAdapter = CalendarAdapter { selectedDate ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDateStr = dateFormat.format(selectedDate)
            val pair = viewModel.dailyTotals.value[selectedDateStr]
            val dailyTotalMs = pair?.first ?: 0L
            val sessionGoalMs = pair?.second ?: 0L
            val effectiveGoalMs = if (sessionGoalMs > 0) sessionGoalMs else currentDailyGoalMs
            val checkmark = if (effectiveGoalMs > 0 && dailyTotalMs >= effectiveGoalMs) " ✅" else ""
            binding.textDailyStats.text = "Time spent: ${formatTime(dailyTotalMs)}$checkmark"
        }
        binding.calendarRecyclerView.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.calendarRecyclerView.addItemDecoration(CalendarStreakDecoration(calendarAdapter))
        binding.calendarRecyclerView.adapter = calendarAdapter

        binding.buttonPrevMonth.setOnClickListener {
            currentCalendarMonth.add(Calendar.MONTH, -1)
            updateCalendarUI()
        }

        binding.buttonNextMonth.setOnClickListener {
            currentCalendarMonth.add(Calendar.MONTH, 1)
            updateCalendarUI()
        }

        updateCalendarUI()
    }

    private fun updateCalendarUI() {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.textMonthYear.text = monthFormat.format(currentCalendarMonth.time)
        refreshCalendarData()
    }

    private fun refreshCalendarData() {
        val dates = generateDatesForMonth(currentCalendarMonth)
        calendarAdapter.updateData(dates, viewModel.dailyTotals.value, currentDailyGoalMs)
    }

    private fun generateDatesForMonth(monthCalendar: Calendar): List<Date?> {
        val dates = mutableListOf<Date?>()
        
        val calendar = monthCalendar.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // Convert to offset where Monday is 0
        val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
        
        for (i in 0 until offset) {
            dates.add(null)
        }
        
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..maxDays) {
            dates.add(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        while (dates.size % 7 != 0) {
            dates.add(null)
        }
        
        return dates
    }
    
    private fun startTimer() {
        isRunning = true
        startTime = SystemClock.elapsedRealtime()
        
        binding.buttonStartStop.text = "Stop"
        // Change button color to red
        binding.buttonStartStop.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#B3261E"))
        
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive && isRunning) {
                val elapsedMs = SystemClock.elapsedRealtime() - startTime
                val currentTotalMs = todayTotalMs + elapsedMs
                binding.stopwatch.text = formatTimeMs(currentTotalMs)
                delay(30)
            }
        }
    }
    
    private fun stopTimer() {
        isRunning = false
        binding.buttonStartStop.text = "Start"
        // Change button color back to default primary
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        binding.buttonStartStop.backgroundTintList = ColorStateList.valueOf(typedValue.data)
        
        val durationMs = SystemClock.elapsedRealtime() - startTime
        
        // Save session
        viewModel.saveSession(habitId, durationMs)
        
        binding.stopwatch.text = formatTimeMs(todayTotalMs + durationMs)
    }
    
    private fun showStopConfirmationDialog(isFromBackButton: Boolean = false) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_stop_habit, null)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setBackground(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            .setCancelable(false)
            .create()
            
        dialogView.findViewById<View>(R.id.button_cancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<View>(R.id.button_confirm).setOnClickListener {
            stopTimer()
            dialog.dismiss()
            if (isFromBackButton) {
                findNavController().navigateUp()
            }
        }
        
        dialog.show()
    }
    
    private fun updateDailyGoalText() {
        if (currentDailyGoalMs > 0) {
            val totalSeconds = currentDailyGoalMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            
            val timeStr = buildString {
                if (hours > 0) append("${hours}h ")
                if (minutes > 0 || hours > 0) append("${minutes}m ")
                append("${seconds}s")
            }.trim()
            
            binding.textDailyGoal.text = "Daily Goal: $timeStr"
        } else {
            binding.textDailyGoal.text = "Daily Goal: Not set"
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0 || hours > 0) append("${minutes}m ")
            append("${seconds}s")
        }.trim()
    }
    
    private fun formatTimeMs(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val tenths = (ms % 1000) / 100
        
        return String.format("%02d:%02d:%02d.%1d", hours, minutes, seconds, tenths)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
