package com.example.startwatch

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val onDeleteClick: (Task) -> Unit,
    private val onTaskCheckChanged: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkbox_task)
        val textView: TextView = view.findViewById(R.id.text_view_task)
        val deleteIcon: ImageView = view.findViewById(R.id.image_view_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.textView.text = task.text
        
        // Remove listener to prevent loop when setting state
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = task.isDone
        
        updateTextStrikeThrough(holder.textView, task.isDone)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onTaskCheckChanged(task, isChecked)
            updateTextStrikeThrough(holder.textView, isChecked)
        }

        holder.deleteIcon.setOnClickListener {
            onDeleteClick(task)
        }
    }

    override fun getItemCount() = tasks.size

    private fun updateTextStrikeThrough(textView: TextView, isDone: Boolean) {
        if (isDone) {
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }
}
