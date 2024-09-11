package com.example.recycleviewpractice.trackingSummary

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.DashPathEffect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.recycleviewpractice.R

class DottedLineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var lineColor = ContextCompat.getColor(context, R.color.buddha_gold)

    private val paint = Paint().apply {
        color = lineColor
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 1.5f
        pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
    }

    fun setLineColor(color: Int) {
        lineColor = color
        paint.color = lineColor
        invalidate() // Redraw the view with the new color
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val startX = width / 2f
        val startY = 0f
        val stopX = width / 2f
        val stopY = height.toFloat()
        canvas.drawLine(startX, startY, stopX, stopY, paint)
    }
}
