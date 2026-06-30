package com.example.startwatch

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarAdapter(
    private val onDateClicked: (Date) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private val dates = mutableListOf<Date?>()
    private var dailyTotals = mapOf<String, Pair<Long, Long>>()
    private var currentDailyGoalMs = 0L
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun updateData(newDates: List<Date?>, newTotals: Map<String, Pair<Long, Long>>, goalMs: Long) {
        dates.clear()
        dates.addAll(newDates)
        dailyTotals = newTotals
        currentDailyGoalMs = goalMs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val date = dates[position]
        holder.bind(date)
    }

    override fun getItemCount() = dates.size

    fun getColorForPosition(position: Int): Int? {
        val date = dates.getOrNull(position) ?: return null
        val dateStr = dateFormat.format(date)
        val pair = dailyTotals[dateStr]
        val totalMs = pair?.first ?: 0L
        val sessionGoalMs = pair?.second ?: 0L
        val effectiveGoalMs = if (sessionGoalMs > 0) sessionGoalMs else currentDailyGoalMs

        if (totalMs > 0 && effectiveGoalMs > 0) {
            val percentage = totalMs.toFloat() / effectiveGoalMs.toFloat()
            return when {
                percentage >= 1f -> Color.parseColor("#4CAF50") // Green
                percentage >= 0.5f -> Color.parseColor("#FFC107") // Yellow
                else -> Color.parseColor("#F44336") // Red
            }
        }
        return null
    }

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textDay: TextView = itemView.findViewById(R.id.text_day)
        private val defaultTextColor = textDay.textColors

        fun bind(date: Date?) {
            if (date == null) {
                textDay.text = ""
                textDay.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                itemView.setOnClickListener(null)
                return
            }

            val calendar = Calendar.getInstance().apply { time = date }
            textDay.text = calendar.get(Calendar.DAY_OF_MONTH).toString()

            val color = getColorForPosition(adapterPosition)
            
            if (color != null) {
                textDay.backgroundTintList = ColorStateList.valueOf(color)
                val pair = dailyTotals[dateFormat.format(date)]
                val totalMs = pair?.first ?: 0L
                val sessionGoalMs = pair?.second ?: 0L
                val effectiveGoalMs = if (sessionGoalMs > 0) sessionGoalMs else currentDailyGoalMs
                val percentage = totalMs.toFloat() / effectiveGoalMs.toFloat()
                textDay.setTextColor(if (percentage >= 0.5f && percentage < 1f) Color.BLACK else Color.WHITE)
            } else {
                textDay.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                textDay.setTextColor(defaultTextColor)
            }

            itemView.setOnClickListener {
                onDateClicked(date)
            }
        }
    }
}
