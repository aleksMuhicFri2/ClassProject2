package si.uni_lj.fri.pbd.classproject2.ui.custom
// didn't work before and this works, so im not touching it

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import androidx.core.graphics.toColorInt

// PieChartView -> handles the pie chart UI -> the center element of DashboardFragment
class PieChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class ActivityData(val durationSeconds: Int, val percentage: Float)

    var activityData: Map<String, ActivityData> = emptyMap()
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
    }

    // Sets the colors for different activities
    private val colors = listOf(
        "#66BB6A".toColorInt(), // Walking   -> green
        "#FFA726".toColorInt(), // Running   -> yellow
        "#42A5F5".toColorInt(), // Cycling   -> blue
        "#EF5350".toColorInt()  // Sedentary -> red
    )

    private val segmentPaths = mutableListOf<Pair<Path, String>>()
    private var pressedActivity: String? = null
    private var pressedIndex: Int? = null

    private val popupBackgroundColor = "#5050A2".toColorInt()
    private val popupBorderColor = "#191919".toColorInt()

    // When we press on a segment it darkens
    private fun darkenColor(color: Int, factor: Float = 0.9f): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private val tempRect = RectF()
    private val popupRect = RectF()

    private val backgroundPaint = Paint().apply {
        color = popupBackgroundColor
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = popupBorderColor
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (activityData.isEmpty()) return

        val total = activityData.values.sumOf { it.percentage.toDouble() }

        if (total <= 0f) return

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(centerX, centerY) * 0.85f
        val growthFactor = 1.07f // Amount the pressed segment grows
        var startAngle = -90f
        segmentPaths.clear()

        activityData.entries.forEachIndexed { index, (name, data) ->
            val sweepAngle = min((data.percentage / 100f) * 360f, 359.99f)
            val baseColor = colors[index]

            paint.color = if (name == pressedActivity) {
                darkenColor(baseColor)
            } else {
                baseColor
            }

            // Increase the radius for the pressed segment
            val effectiveRadius = if (index == pressedIndex) {
                radius * (growthFactor)
            } else {
                radius
            }

            tempRect.set(
                centerX - effectiveRadius, centerY - effectiveRadius,
                centerX + effectiveRadius, centerY + effectiveRadius
            )

            val path = Path()
            path.moveTo(centerX, centerY)
            path.arcTo(tempRect, startAngle, sweepAngle)
            path.close()

            canvas.drawPath(path, paint)
            segmentPaths.add(path to name)

            startAngle += sweepAngle
        }

        // We generate a label here
        pressedActivity?.let { label ->
            val data = activityData[label] ?: return

            val minutes = data.durationSeconds / 60
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            val remainingSeconds = data.durationSeconds % 60

            // Time display on press -> changes based on current duration
            val durationText = if (hours > 0) {
                "${hours}h${remainingMinutes}min"
            } else {
                if(remainingMinutes == 0){
                    "${remainingSeconds}sec"
                } else {
                    "${remainingMinutes}min"
                }
            }

            val text = """
                Type: $label
                Duration: $durationText
                Percentage: ${"%.1f".format(data.percentage)}%
            """.trimIndent()

            // Label text properties
            val padding = 20f
            val textLines = text.split("\n")
            val textHeight = textLines.size * textPaint.textSize + padding
            val maxTextWidth = textLines.maxOf { textPaint.measureText(it) }

            // Display data Box size
            val boxLeft = centerX - maxTextWidth / 2 - padding
            val boxRight = centerX + maxTextWidth / 2 + padding
            val boxTop = centerY - textHeight / 2 - padding
            val boxBottom = centerY + textHeight / 2 + padding

            popupRect.set(boxLeft, boxTop, boxRight, boxBottom)
            backgroundPaint.color = popupBackgroundColor
            canvas.drawRoundRect(
                popupRect,
                20f, 20f,
                backgroundPaint
            )

            borderPaint.color = popupBorderColor
            canvas.drawRoundRect(
                popupRect,
                20f, 20f,
                borderPaint
            )

            // Text
            textLines.forEachIndexed { i, line ->
                canvas.drawText(
                    line,
                    centerX - maxTextWidth / 2,
                    boxTop + padding + (i + 1.1f) * textPaint.textSize,
                    textPaint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y

                val foundActivity = segmentPaths.find { (path, _) ->
                    val region = Region()
                    val bounds = RectF()

                    // Couldn't find an un-deprecated one
                    path.computeBounds(bounds, true)
                    region.setPath(path, Region(
                        bounds.left.toInt(),
                        bounds.top.toInt(),
                        bounds.right.toInt(),
                        bounds.bottom.toInt()
                    ))
                    region.contains(x.toInt(), y.toInt())
                }?.second

                pressedIndex = foundActivity?.let { activityData.keys.indexOf(it) }
                pressedActivity = foundActivity
                invalidate()
            }
            // Reset pressed state when touch is released
            MotionEvent.ACTION_UP -> {
                pressedIndex = null
                pressedActivity = null
                invalidate()
            }
        }
        return true
    }
}