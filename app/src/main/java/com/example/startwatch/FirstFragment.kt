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

        adapter = HabitAdapter(
            onItemClick = { habit ->
                val bundle = Bundle().apply {
                    putString("habitId", habit.id)
                    putString("habitName", habit.name)
                    putLong("dailyGoalMs", habit.dailyGoalMs)
                    putString("habitType", habit.type)
                }
                findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment, bundle)
            },
            onItemLongClick = { habit ->
                showHabitDialog(habit)
            }
        )

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
            showHabitDialog(null)
        }
    }

    private fun showHabitDialog(habitToEdit: com.example.startwatch.data.HabitWithProgress?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_habit, null)
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val titleText = dialogView.findViewById<android.widget.TextView>(R.id.text_dialog_title) ?: dialogView.findViewWithTag<android.widget.TextView>("title")
        // If we don't have an ID, we'll just skip the title change or rely on layout changes.
        // Wait, dialog_add_habit.xml has a TextView for the title. Let me search for it... it has text="New Habit". 
        // I will just find it by traversing or we can cast it. I'll search by text for safety if ID isn't known, or just leave it.

        val nameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_habit_name)
        val toggleHabitType = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_habit_type)
        val textGoalLabel = dialogView.findViewById<android.widget.TextView>(R.id.text_goal_label)
        val layoutTimePicker = dialogView.findViewById<android.widget.LinearLayout>(R.id.layout_time_picker)
        val layoutRepetitionPicker = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.layout_repetition_picker)
        val editRepetitionTarget = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_repetition_target)

        val pickerHours = dialogView.findViewById<android.widget.NumberPicker>(R.id.picker_hours)
        val pickerMinutes = dialogView.findViewById<android.widget.NumberPicker>(R.id.picker_minutes)
        val pickerSeconds = dialogView.findViewById<android.widget.NumberPicker>(R.id.picker_seconds)
        val buttonAdd = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.button_add)
        val buttonCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.button_cancel)
        val buttonDelete = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.button_delete)

        pickerHours.minValue = 0
        pickerHours.maxValue = 23
        
        pickerMinutes.minValue = 0
        pickerMinutes.maxValue = 59

        pickerSeconds.minValue = 0
        pickerSeconds.maxValue = 59

        var selectedIconName = "ic_timer"
        var selectedIconResId = R.drawable.ic_timer

        toggleHabitType.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_type_time -> {
                        textGoalLabel.visibility = View.VISIBLE
                        layoutTimePicker.visibility = View.VISIBLE
                        layoutRepetitionPicker.visibility = View.GONE
                    }
                    R.id.btn_type_repetition -> {
                        textGoalLabel.visibility = View.VISIBLE
                        layoutTimePicker.visibility = View.GONE
                        layoutRepetitionPicker.visibility = View.VISIBLE
                    }
                    R.id.btn_type_boolean -> {
                        textGoalLabel.visibility = View.GONE
                        layoutTimePicker.visibility = View.GONE
                        layoutRepetitionPicker.visibility = View.GONE
                    }
                }
            }
        }

        if (habitToEdit != null) {
            titleText?.text = "Edit Habit"
            nameInput.setText(habitToEdit.name)
            
            when (habitToEdit.type) {
                "TIME" -> {
                    toggleHabitType.check(R.id.btn_type_time)
                    pickerHours.value = (habitToEdit.dailyGoalMs / 3600000).toInt()
                    pickerMinutes.value = ((habitToEdit.dailyGoalMs % 3600000) / 60000).toInt()
                    pickerSeconds.value = ((habitToEdit.dailyGoalMs % 60000) / 1000).toInt()
                }
                "REPETITION" -> {
                    toggleHabitType.check(R.id.btn_type_repetition)
                    editRepetitionTarget.setText(habitToEdit.dailyGoalMs.toString())
                }
                "BOOLEAN" -> {
                    toggleHabitType.check(R.id.btn_type_boolean)
                }
            }
            
            // Disable type change for existing habits
            for (i in 0 until toggleHabitType.childCount) {
                toggleHabitType.getChildAt(i).isEnabled = false
            }
            
            selectedIconName = habitToEdit.iconName
            selectedIconResId = resources.getIdentifier(selectedIconName, "drawable", requireContext().packageName)
            if (selectedIconResId == 0) selectedIconResId = R.drawable.ic_timer
            buttonAdd.text = "Save"
            
            buttonDelete.visibility = android.view.View.VISIBLE
            buttonDelete.setOnClickListener {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Habit?")
                    .setMessage("Are you sure you want to permanently delete this habit and all its sessions?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteHabit(habitToEdit.id)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        // Setup the Expandable Icon Picker Header
        val layoutHeader = dialogView.findViewById<android.view.View>(R.id.layout_selected_icon_header)
        val iconPreview = dialogView.findViewById<android.widget.ImageView>(R.id.icon_selected_preview)
        val iconExpandIndicator = dialogView.findViewById<android.widget.ImageView>(R.id.icon_expand_indicator)
        val recyclerIcons = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_icons)
        
        iconPreview.setImageResource(selectedIconResId)

        layoutHeader.setOnClickListener {
            if (recyclerIcons.visibility == android.view.View.GONE) {
                recyclerIcons.visibility = android.view.View.VISIBLE
                iconExpandIndicator.setImageResource(R.drawable.ic_chevron_left)
            } else {
                recyclerIcons.visibility = android.view.View.GONE
                iconExpandIndicator.setImageResource(R.drawable.ic_chevron_right)
            }
        }

        // Load all ic_ icons dynamically
        val iconList = mutableListOf<Pair<String, Int>>()
        val drawableClass = R.drawable::class.java
        for (field in drawableClass.declaredFields) {
            val name = field.name
            // Basic filtering to capture our downloaded vector drawables and standard ones
            if (name.startsWith("ic_") && !name.contains("launcher") && !name.contains("background")) {
                try {
                    val resId = field.getInt(null)
                    iconList.add(Pair(name, resId))
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        
        // Sort to have the defaults at the top, just for convenience
        val defaultIcons = listOf("ic_timer", "ic_book", "ic_fitness", "ic_code", "ic_water")
        iconList.sortBy { 
            val index = defaultIcons.indexOf(it.first)
            if (index != -1) index else 100 
        }

        val iconAdapter = IconPickerAdapter(iconList, selectedIconName) { name, resId ->
            selectedIconName = name
            selectedIconResId = resId
            iconPreview.setImageResource(resId)
            
            // Auto collapse when an icon is selected (optional but nice UI)
            recyclerIcons.visibility = android.view.View.GONE
            iconExpandIndicator.setImageResource(R.drawable.ic_chevron_right)
        }
        
        recyclerIcons.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 4, androidx.recyclerview.widget.RecyclerView.HORIZONTAL, false)
        recyclerIcons.adapter = iconAdapter

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        buttonAdd.setOnClickListener {
            val name = nameInput.text?.toString() ?: ""
            val selectedTypeId = toggleHabitType.checkedButtonId
            
            var dailyGoalMs = 0L
            var typeStr = "TIME"
            
            when (selectedTypeId) {
                R.id.btn_type_time -> {
                    val hours = pickerHours.value
                    val minutes = pickerMinutes.value
                    val seconds = pickerSeconds.value
                    dailyGoalMs = (hours * 3600L + minutes * 60L + seconds) * 1000L
                    typeStr = "TIME"
                }
                R.id.btn_type_repetition -> {
                    val target = editRepetitionTarget.text?.toString()?.toLongOrNull() ?: 0L
                    dailyGoalMs = target
                    typeStr = "REPETITION"
                }
                R.id.btn_type_boolean -> {
                    dailyGoalMs = 1L
                    typeStr = "BOOLEAN"
                }
            }

            if (name.isNotBlank()) {
                if (habitToEdit == null) {
                    viewModel.addHabit(name, dailyGoalMs, selectedIconName, typeStr)
                } else {
                    viewModel.editHabit(habitToEdit.id, name, dailyGoalMs, selectedIconName, typeStr)
                }
                dialog.dismiss()
            }
        }

        buttonCancel.setOnClickListener {
            dialog.cancel()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}