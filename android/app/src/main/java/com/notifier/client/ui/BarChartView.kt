package com.notifier.client.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.notifier.client.R

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var data: List<DailyCount> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.dash_emerald_bright)
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.glass_card_stroke)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.glass_text_secondary)
        textAlign = Paint.Align.CENTER
        textSize = 28f
    }

    fun submitData(newData: List<DailyCount>) {
        data = newData
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val maxCount = data.maxOf { it.count }.coerceAtLeast(1)
        val labelAreaHeight = 44f
        val chartBottom = height - labelAreaHeight
        val chartTop = 8f
        val barAreaWidth = width.toFloat() / data.size

        data.forEachIndexed { index, item ->
            val centerX = barAreaWidth * index + barAreaWidth / 2f
            val barWidth = barAreaWidth * 0.36f
            val cornerRadius = barWidth / 2f
            val ratio = item.count / maxCount.toFloat()
            val barTop = chartBottom - (chartBottom - chartTop) * ratio

            canvas.drawRoundRect(
                RectF(centerX - barWidth / 2, chartTop, centerX + barWidth / 2, chartBottom),
                cornerRadius, cornerRadius, trackPaint
            )
            if (item.count > 0) {
                canvas.drawRoundRect(
                    RectF(centerX - barWidth / 2, barTop, centerX + barWidth / 2, chartBottom),
                    cornerRadius, cornerRadius, barPaint
                )
            }
            canvas.drawText(item.label, centerX, height.toFloat() - 6f, labelPaint)
        }
    }
}
