package com.example.startwatch

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.startwatch.databinding.FragmentFirstBinding
import kotlinx.coroutines.launch

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HabitsViewModel by viewModels()
    private lateinit var adapter: HabitAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = HabitAdapter { habit ->
            val bundle = Bundle().apply {
                putString("habitId", habit.id)
                putString("habitName", habit.name)
                putLong("dailyGoalMs", habit.dailyGoalMs)
            }
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment, bundle)
        }

        binding.recyclerViewHabits.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewHabits.adapter = adapter

        // Observe habits
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.habits.collect { habits ->
                    adapter.submitList(habits)
                }
            }
        }

        // Search functionality
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.searchHabits(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchHabits(newText ?: "")
                return true
            }
        })

        // Add Habit FAB
        binding.fabAddHabit.setOnClickListener {
            showAddHabitDialog()
        }
    }

    private fun showAddHabitDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_habit, null)
        val nameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_habit_name)
        val pickerHours = dialogView.findViewById<android.widget.NumberPicker>(R.id.picker_hours)
        val pickerMinutes = dialogView.findViewById<android.widget.NumberPicker>(R.id.picker_minutes)
        val pickerSeconds = dialogView.findViewById<android.widget.NumberPicker>(R.id.picker_seconds)

        pickerHours.minValue = 0
        pickerHours.maxValue = 23
        
        pickerMinutes.minValue = 0
        pickerMinutes.maxValue = 59

        pickerSeconds.minValue = 0
        pickerSeconds.maxValue = 59

        AlertDialog.Builder(requireContext())
            .setTitle("New Habit")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val name = nameInput.text?.toString() ?: ""
                val hours = pickerHours.value
                val minutes = pickerMinutes.value
                val seconds = pickerSeconds.value
                val dailyGoalMs = (hours * 3600L + minutes * 60L + seconds) * 1000L

                if (name.isNotBlank()) {
                    viewModel.addHabit(name, dailyGoalMs)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}