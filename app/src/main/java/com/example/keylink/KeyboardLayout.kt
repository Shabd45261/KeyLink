package com.example.keylink

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import kotlin.math.roundToInt
import com.example.keylink.R

class KeyboardLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val horizontalKeyboardPadding = 20f * resources.displayMetrics.density
    private val verticalKeyboardPadding = 12f * resources.displayMetrics.density
    private val keyGap = 4f * resources.displayMetrics.density // Phone default
    private val tabletGap = 6f * resources.displayMetrics.density

    // Row Height Multipliers
    private val rowMultipliers = floatArrayOf(0.9f, 1.0f, 1.15f, 1.15f, 1.15f, 1.2f)
    private val totalRowWeight = rowMultipliers.sum()

    // Aspect Ratio
    private val targetAspectRatio = 2.65f
    
    var widthScale = 1.0f
    var heightScale = 1.0f
    var xOffset = 0f
    var yOffset = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        
        // Base content width (max possible with padding)
        val baseContentWidth = width - (horizontalKeyboardPadding * 2)
        
        // Apply scales
        val contentWidth = baseContentWidth * widthScale
        val contentHeight = (contentWidth / targetAspectRatio) * heightScale
        
        setMeasuredDimension(width, height)

        // Measure children
        val gap = if (width > 1200) tabletGap else keyGap
        val availableHeight = contentHeight - (gap * (rowMultipliers.size - 1))
        val unitHeight = availableHeight / totalRowWeight
        
        // Base Key Width calculation
        // Row 2 is the longest in terms of key units: Tab(1.55) + 12 keys + \ (1.5) = 15.05
        val baseCols = 15.05f
        val availableWidth = contentWidth - (gap * (baseCols.toInt()))
        val unitWidth = availableWidth / baseCols

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue

            val lp = child.layoutParams as LayoutParams
            val w = (unitWidth * lp.widthMultiplier + gap * (lp.widthMultiplier - 1)).coerceAtLeast(0f)
            val h = unitHeight * rowMultipliers[lp.rowIndex]
            
            child.measure(
                MeasureSpec.makeMeasureSpec(w.roundToInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(h.roundToInt(), MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        val baseContentWidth = width - (horizontalKeyboardPadding * 2)
        val contentWidth = baseContentWidth * widthScale
        val contentHeight = (contentWidth / targetAspectRatio) * heightScale
        
        val startY = (height - contentHeight) / 2f + yOffset
        val startX = horizontalKeyboardPadding + xOffset
        
        val gap = if (width > 1200) tabletGap else keyGap
        val availableHeight = contentHeight - (gap * (rowMultipliers.size - 1))
        val unitHeight = availableHeight / totalRowWeight

        val baseCols = 15.05f
        val availableWidth = contentWidth - (gap * (baseCols.toInt()))
        val unitWidth = availableWidth / baseCols

        // Row Offsets (Rule 12)
        val rowOffsets = floatArrayOf(
            0f,      // F Keys
            0f,      // Numbers
            0.45f,   // QWERTY
            0.65f,   // ASDF
            1.1f,    // ZXCV
            0f       // Bottom
        )

        val rowY = FloatArray(rowMultipliers.size)
        var currentY = startY
        for (i in rowMultipliers.indices) {
            rowY[i] = currentY
            currentY += unitHeight * rowMultipliers[i] + gap
        }

        val rowCurrentX = FloatArray(rowMultipliers.size)
        for (i in rowMultipliers.indices) {
            rowCurrentX[i] = startX + (rowOffsets[i] * (unitWidth + gap))
        }

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) continue

            val lp = child.layoutParams as LayoutParams
            val row = lp.rowIndex
            
            val left = rowCurrentX[row]
            val top = rowY[row]
            val right = left + child.measuredWidth
            val bottom = top + child.measuredHeight
            
            child.layout(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
            
            rowCurrentX[row] = right + gap
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    class LayoutParams : ViewGroup.LayoutParams {
        var rowIndex: Int = 0
        var widthMultiplier: Float = 1.0f

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.KeyboardLayout_Layout)
            rowIndex = a.getInt(R.styleable.KeyboardLayout_Layout_layout_rowIndex, 0)
            widthMultiplier = a.getFloat(R.styleable.KeyboardLayout_Layout_layout_widthMultiplier, 1.0f)
            a.recycle()
        }

        constructor(width: Int, height: Int) : super(width, height)
    }
}
