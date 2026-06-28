package com.voidascension.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.voidascension.utils.Vector2
import kotlin.math.*

class VirtualJoystick @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onMove: ((Vector2) -> Unit)? = null

    private val outerRadius get() = width / 2f * 0.92f
    private val innerRadius get() = outerRadius * 0.38f
    private var knobX = 0f
    private var knobY = 0f
    private var active = false
    private var trackingId = -1

    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f

        // Background circle (Dark Alien Void)
        bgPaint.color = 0x4402050A.toInt()
        canvas.drawCircle(cx, cy, outerRadius, bgPaint)

        // Outer energy ring
        outerPaint.color = if (active) 0xAA39FF14.toInt() else 0x4439FF14.toInt()
        outerPaint.strokeWidth = 4f
        canvas.drawCircle(cx, cy, outerRadius, outerPaint)
        
        // Secondary thin ring
        outerPaint.color = 0x2200D7FF.toInt()
        outerPaint.strokeWidth = 2f
        canvas.drawCircle(cx, cy, outerRadius * 0.85f, outerPaint)

        // Cross lines (Tech grid)
        outerPaint.strokeWidth = 1f
        outerPaint.color = 0x3300D7FF.toInt()
        canvas.drawLine(cx - outerRadius, cy, cx + outerRadius, cy, outerPaint)
        canvas.drawLine(cx, cy - outerRadius, cx, cy + outerRadius, outerPaint)
        
        // Diagonal cross lines
        val diag = outerRadius * 0.707f
        canvas.drawLine(cx - diag, cy - diag, cx + diag, cy + diag, outerPaint)
        canvas.drawLine(cx + diag, cy - diag, cx - diag, cy + diag, outerPaint)

        // Knob
        val kx = if (active) cx + knobX else cx
        val ky = if (active) cy + knobY else cy

        // Knob glow (Alien Pulsing)
        innerPaint.color = if (active) 0x66B026FF.toInt() else 0x22B026FF.toInt()
        canvas.drawCircle(kx, ky, innerRadius * 1.8f, innerPaint)

        // Knob body
        innerPaint.color = if (active) 0xFF00D7FF.toInt() else 0x8800D7FF.toInt()
        canvas.drawCircle(kx, ky, innerRadius, innerPaint)

        // Knob highlight (Energy core)
        innerPaint.color = 0xBBFFFFFF.toInt()
        canvas.drawCircle(kx - innerRadius * 0.15f, ky - innerRadius * 0.15f, innerRadius * 0.25f, innerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = width / 2f
        val cy = height / 2f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                trackingId = event.getPointerId(idx)
                active = true
                updateKnob(event.getX(idx) - cx, event.getY(idx) - cy)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val pIdx = event.findPointerIndex(trackingId)
                if (pIdx >= 0) {
                    updateKnob(event.getX(pIdx) - cx, event.getY(pIdx) - cy)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.getPointerId(event.actionIndex) == trackingId) {
                    active = false
                    trackingId = -1
                    knobX = 0f; knobY = 0f
                    onMove?.invoke(Vector2(0f, 0f))
                    invalidate()
                }
                return true
            }
        }
        return false
    }

    private fun updateKnob(dx: Float, dy: Float) {
        val dist = sqrt(dx * dx + dy * dy)
        val maxDist = outerRadius - innerRadius
        if (dist > maxDist) {
            knobX = dx / dist * maxDist
            knobY = dy / dist * maxDist
        } else {
            knobX = dx; knobY = dy
        }
        val normX = knobX / maxDist
        val normY = knobY / maxDist
        onMove?.invoke(Vector2(normX, normY))
        invalidate()
    }

    fun reset() {
        active = false
        trackingId = -1
        knobX = 0f; knobY = 0f
        onMove?.invoke(Vector2(0f, 0f))
        invalidate()
    }
}
