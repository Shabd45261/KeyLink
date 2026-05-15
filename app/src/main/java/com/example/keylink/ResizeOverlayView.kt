package com.example.keylink

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ResizeOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val handlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val handleSize = 30f
    private val touchThreshold = 60f

    private var targetView: KeyboardLayout? = null
    private var onResized: (() -> Unit)? = null

    private enum class DragMode { NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP, BOTTOM, LEFT, RIGHT }
    private var dragMode = DragMode.NONE
    private var lastX = 0f
    private var lastY = 0f

    fun setTarget(view: KeyboardLayout, onResizedCallback: () -> Unit) {
        this.targetView = view
        this.onResized = onResizedCallback
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val target = targetView ?: return

        // Calculate current bounds of the keyboard content
        val width = width.toFloat()
        val height = height.toFloat()
        val baseContentWidth = width - (20f * resources.displayMetrics.density * 2)
        val contentWidth = baseContentWidth * target.widthScale
        val contentHeight = (contentWidth / 2.65f) * target.heightScale
        
        val startX = (20f * resources.displayMetrics.density) + target.xOffset
        val startY = (height - contentHeight) / 2f + target.yOffset

        val rect = RectF(startX, startY, startX + contentWidth, startY + contentHeight)

        // Draw dimmed background
        val dimPaint = Paint().apply {
            color = Color.BLACK
            alpha = 150
        }
        canvas.drawRect(0f, 0f, width, rect.top, dimPaint) // Top
        canvas.drawRect(0f, rect.bottom, width, height, dimPaint) // Bottom
        canvas.drawRect(0f, rect.top, rect.left, rect.bottom, dimPaint) // Left
        canvas.drawRect(rect.right, rect.top, width, rect.bottom, dimPaint) // Right

        // Draw border
        canvas.drawRect(rect, borderPaint)

        // Draw handles (rectangles on corners and midpoints for better feel)
        drawHandle(canvas, rect.left, rect.top)
        drawHandle(canvas, rect.right, rect.top)
        drawHandle(canvas, rect.left, rect.bottom)
        drawHandle(canvas, rect.right, rect.bottom)
        
        // Midpoints
        drawHandle(canvas, rect.centerX(), rect.top)
        drawHandle(canvas, rect.centerX(), rect.bottom)
        drawHandle(canvas, rect.left, rect.centerY())
        drawHandle(canvas, rect.right, rect.centerY())
    }

    private fun drawHandle(canvas: Canvas, cx: Float, cy: Float) {
        canvas.drawRect(cx - handleSize/2, cy - handleSize/2, cx + handleSize/2, cy + handleSize/2, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val target = targetView ?: return false

        val x = event.x
        val y = event.y

        val width = width.toFloat()
        val height = height.toFloat()
        val baseContentWidth = width - (20f * resources.displayMetrics.density * 2)
        val contentWidth = baseContentWidth * target.widthScale
        val contentHeight = (contentWidth / 2.65f) * target.heightScale
        val startX = (20f * resources.displayMetrics.density) + target.xOffset
        val startY = (height - contentHeight) / 2f + target.yOffset
        val rect = RectF(startX, startY, startX + contentWidth, startY + contentHeight)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragMode = when {
                    isNear(x, y, rect.left, rect.top) -> DragMode.TOP_LEFT
                    isNear(x, y, rect.right, rect.top) -> DragMode.TOP_RIGHT
                    isNear(x, y, rect.left, rect.bottom) -> DragMode.BOTTOM_LEFT
                    isNear(x, y, rect.right, rect.bottom) -> DragMode.BOTTOM_RIGHT
                    isNear(x, y, rect.centerX(), rect.top) -> DragMode.TOP
                    isNear(x, y, rect.centerX(), rect.bottom) -> DragMode.BOTTOM
                    isNear(x, y, rect.left, rect.centerY()) -> DragMode.LEFT
                    isNear(x, y, rect.right, rect.centerY()) -> DragMode.RIGHT
                    rect.contains(x, y) -> DragMode.MOVE
                    else -> DragMode.NONE
                }
                lastX = x
                lastY = y
                return true // Always consume in resize mode
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = x - lastX
                val dy = y - lastY

                when (dragMode) {
                    DragMode.MOVE -> {
                        target.xOffset += dx
                        target.yOffset += dy
                    }
                    DragMode.TOP_LEFT -> {
                        val oldW = contentWidth
                        val oldH = contentHeight
                        val newW = (oldW - dx).coerceAtLeast(100f)
                        val newH = (oldH - dy).coerceAtLeast(100f)
                        target.widthScale = newW / baseContentWidth
                        target.heightScale = newH / (newW / 2.65f)
                        target.xOffset += (oldW - newW)
                        target.yOffset += (oldH - newH) / 2f 
                    }
                    DragMode.BOTTOM_RIGHT -> {
                        val newW = (contentWidth + dx).coerceAtLeast(100f)
                        val newH = (contentHeight + dy).coerceAtLeast(100f)
                        target.widthScale = newW / baseContentWidth
                        target.heightScale = newH / (newW / 2.65f)
                    }
                    DragMode.TOP_RIGHT -> {
                        val newW = (contentWidth + dx).coerceAtLeast(100f)
                        val newH = (contentHeight - dy).coerceAtLeast(100f)
                        target.widthScale = newW / baseContentWidth
                        target.heightScale = newH / (newW / 2.65f)
                        target.yOffset += (contentHeight - newH) / 2f
                    }
                    DragMode.BOTTOM_LEFT -> {
                        val oldW = contentWidth
                        val newW = (oldW - dx).coerceAtLeast(100f)
                        val newH = (contentHeight + dy).coerceAtLeast(100f)
                        target.widthScale = newW / baseContentWidth
                        target.heightScale = newH / (newW / 2.65f)
                        target.xOffset += (oldW - newW)
                    }
                    DragMode.TOP -> {
                        val newH = (contentHeight - dy).coerceAtLeast(100f)
                        target.heightScale = newH / (contentWidth / 2.65f)
                        target.yOffset += (contentHeight - newH) / 2f
                    }
                    DragMode.BOTTOM -> {
                        val newH = (contentHeight + dy).coerceAtLeast(100f)
                        target.heightScale = newH / (contentWidth / 2.65f)
                    }
                    DragMode.LEFT -> {
                        val oldW = contentWidth
                        val newW = (oldW - dx).coerceAtLeast(100f)
                        target.widthScale = newW / baseContentWidth
                        target.xOffset += (oldW - newW)
                    }
                    DragMode.RIGHT -> {
                        val newW = (contentWidth + dx).coerceAtLeast(100f)
                        target.widthScale = newW / baseContentWidth
                    }
                    else -> {}
                }
                lastX = x
                lastY = y
                target.requestLayout()
                invalidate()
                onResized?.invoke()
                return true
            }
            MotionEvent.ACTION_UP -> {
                dragMode = DragMode.NONE
            }
        }
        return true
    }

    private fun isNear(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        return Math.sqrt(Math.pow((x1 - x2).toDouble(), 2.0) + Math.pow((y1 - y2).toDouble(), 2.0)) < touchThreshold
    }
}
