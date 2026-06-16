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

        // Background circle
        bgPaint.color = 0x22004488.toInt()
        canvas.drawCircle(cx, cy, outerRadius, bgPaint)

        // Outer ring
        outerPaint.color = if (active) 0xAA00FFFF.toInt() else 0x6600AACC.toInt()
        canvas.drawCircle(cx, cy, outerRadius, outerPaint)

        // Cross lines
        outerPaint.strokeWidth = 1f
        outerPaint.color = 0x3300FFFF.toInt()
        canvas.drawLine(cx - outerRadius, cy, cx + outerRadius, cy, outerPaint)
        canvas.drawLine(cx, cy - outerRadius, cx, cy + outerRadius, outerPaint)
        outerPaint.strokeWidth = 3f

        // Knob
        val kx = if (active) cx + knobX else cx
        val ky = if (active) cy + knobY else cy

        // Knob glow
        innerPaint.color = 0x4400FFFF.toInt()
        canvas.drawCircle(kx, ky, innerRadius * 1.6f, innerPaint)

        // Knob body
        innerPaint.color = if (active) 0xCC00FFFF.toInt() else 0x8800AACC.toInt()
        canvas.drawCircle(kx, ky, innerRadius, innerPaint)

        // Knob highlight
        innerPaint.color = 0x88FFFFFF.toInt()
        canvas.drawCircle(kx - innerRadius * 0.2f, ky - innerRadius * 0.2f, innerRadius * 0.3f, innerPaint)
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
