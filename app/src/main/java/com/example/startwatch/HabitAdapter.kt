package com.example.startwatch

import android.animation.ArgbEvaluator
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.startwatch.data.HabitWithProgress
import com.example.startwatch.databinding.ItemHabitBinding

class HabitAdapter(
    private val onItemClick: (HabitWithProgress) -> Unit,
    private val onItemLongClick: (HabitWithProgress) -> Unit
) : ListAdapter<HabitWithProgress, HabitAdapter.HabitViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HabitViewHolder(binding, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HabitViewHolder(
        private val binding: ItemHabitBinding,
        private val onItemClick: (HabitWithProgress) -> Unit,
        private val onItemLongClick: (HabitWithProgress) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(habit: HabitWithProgress) {
            binding.textHabitName.text = habit.name
            
            // Format time nicely (e.g. 1h 30m)
            if (habit.type == "TIME") {
                val totalSeconds = habit.totalTimeSpent / 1000
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                
                val timeString = buildString {
                    if (hours > 0) append("${hours}h ")
                    if (minutes > 0 || hours > 0) append("${minutes}m ")
                    append("${seconds}s")
                }
                
                binding.textHabitTotalTime.text = "Total time: $timeString"
            } else if (habit.type == "REPETITION") {
                binding.textHabitTotalTime.text = "Total reps: ${habit.totalTimeSpent}"
            } else {
                binding.textHabitTotalTime.text = "Completed ${habit.totalTimeSpent} times"
            }

            var iconResId = binding.root.context.resources.getIdentifier(habit.iconName, "drawable", binding.root.context.packageName)
            if (iconResId == 0) iconResId = R.drawable.ic_timer
            binding.iconTimer.setImageResource(iconResId)

            if (habit.dailyGoalMs > 0) {
                binding.progressHabitDaily.visibility = View.VISIBLE
                binding.textHabitDailyProgress.visibility = View.VISIBLE
                
                val progressRatio = (habit.todayTimeSpent.toDouble() / habit.dailyGoalMs.toDouble()).coerceIn(0.0, 1.0)
                binding.progressHabitDaily.progress = (progressRatio * 1000).toInt()
                
                val evaluator = ArgbEvaluator()
                val color = evaluator.evaluate(progressRatio.toFloat(), Color.RED, Color.GREEN) as Int
                binding.progressHabitDaily.progressTintList = ColorStateList.valueOf(color)
                
                if (habit.type == "TIME") {
                    val spentMin = habit.todayTimeSpent / 60000
                    val goalMin = habit.dailyGoalMs / 60000
                    binding.textHabitDailyProgress.text = "${spentMin}m / ${goalMin}m"
                } else if (habit.type == "REPETITION") {
                    binding.textHabitDailyProgress.text = "${habit.todayTimeSpent} / ${habit.dailyGoalMs}"
                } else {
                    binding.textHabitDailyProgress.text = if (habit.todayTimeSpent > 0) "Done" else "Not done"
                }
                
                if (habit.todayTimeSpent >= habit.dailyGoalMs) {
                    binding.iconCheckmark.visibility = View.VISIBLE
                } else {
                    binding.iconCheckmark.visibility = View.GONE
                }
            } else {
                binding.progressHabitDaily.visibility = View.GONE
                binding.textHabitDailyProgress.visibility = View.GONE
                binding.iconCheckmark.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(habit)
            }
            binding.root.setOnLongClickListener {
                onItemLongClick(habit)
                true
            }
        }
    }

    class HabitDiffCallback : DiffUtil.ItemCallback<HabitWithProgress>() {
        override fun areItemsTheSame(oldItem: HabitWithProgress, newItem: HabitWithProgress): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HabitWithProgress, newItem: HabitWithProgress): Boolean {
            return oldItem == newItem
        }
    }
}
