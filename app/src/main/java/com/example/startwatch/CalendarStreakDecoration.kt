package com.example.startwatch

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CalendarStreakDecoration(private val adapter: CalendarAdapter) : RecyclerView.ItemDecoration() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) continue

            val colorCurrent = adapter.getColorForPosition(position) ?: continue
            val colorNext = adapter.getColorForPosition(position + 1)
            val colorPrev = adapter.getColorForPosition(position - 1)

            val textDayView = child.findViewById<View>(R.id.text_day) ?: continue
            val cx1 = child.left + textDayView.left + textDayView.width / 2f
            val cy1 = child.top + textDayView.top + textDayView.height / 2f + textDayView.height * 0.05f // Shifted slightly lower
            val strokeW = textDayView.height.toFloat() * 0.9f // 10% thinner than the circle
            paint.strokeWidth = strokeW

            // Color alpha: ~40% (0x66)
            val alphaMod = 0x66000000.toInt()
            val c1 = (colorCurrent and 0x00FFFFFF) or alphaMod

            // Right connection
            if (colorNext != null) {
                val c2 = (colorNext and 0x00FFFFFF) or alphaMod
                val cx2 = if ((position + 1) % 7 != 0) {
                    cx1 + child.width // Center of next cell
                } else {
                    child.right.toFloat() // Edge of row
                }
                paint.shader = LinearGradient(cx1, cy1, cx2, cy1, c1, c2, Shader.TileMode.CLAMP)
                c.drawLine(cx1, cy1, cx2, cy1, paint)
            }

            // Left connection (only if it's start of row, otherwise it's handled by prev day's right connection)
            if (colorPrev != null && position % 7 == 0) {
                val c0 = (colorPrev and 0x00FFFFFF) or alphaMod
                val cx0 = child.left.toFloat()
                paint.shader = LinearGradient(cx0, cy1, cx1, cy1, c0, c1, Shader.TileMode.CLAMP)
                c.drawLine(cx0, cy1, cx1, cy1, paint)
            }
        }
    }
}
