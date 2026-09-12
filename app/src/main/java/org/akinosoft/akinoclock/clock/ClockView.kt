package org.akinosoft.akinoclock.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.akinosoft.akinoclock.R
import org.akinosoft.akinoclock.util.PeriodicScheduler
import org.akinosoft.akinoclock.util.SecondAlignedScheduler

/**
 * Braun BC12-style analog clock: a cached dial (background, bezel, ticks, numerals) drawn once
 * per size change, with hour/minute/second hands redrawn on top once per second.
 */
open class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var clock: Clock = Clock.systemDefaultZone()
        set(value) {
            field = value
            time = readTime()
            // Always refresh, unguarded: the new clock's minute may coincidentally match
            // whatever `lastDescriptionMinute` was left at (e.g. the real wall-clock minute
            // at construction, before an injected clock replaces the systemDefaultZone()
            // default) — the minute-unchanged guard in updateContentDescription() is only
            // valid for same-clock ticks, not a clock swap.
            setContentDescriptionFromTime()
        }

    var tickScheduler: PeriodicScheduler = SecondAlignedScheduler(Handler(Looper.getMainLooper()))

    var palette: DialPalette = DialPalette.fromResources(context)
        set(value) {
            field = value
            invalidateDialCache()
            invalidate()
        }

    var time: ClockTime = readTime()
        private set

    var nextAlarm: Instant? = null
        set(value) {
            field = value
            // Bypass the per-minute guard, same as the `clock` setter: the alarm hand's
            // visibility can change independently of the wall-clock minute.
            setContentDescriptionFromTime()
            invalidate()
        }

    private var lastDescriptionMinute: Int = -1

    init {
        updateContentDescription()
    }

    constructor(context: Context, clock: Clock, tickScheduler: PeriodicScheduler) : this(context) {
        this.tickScheduler = tickScheduler
        this.clock = clock
    }

    private fun readTime(): ClockTime {
        val t = LocalTime.now(clock)
        return ClockTime(t.hour, t.minute, t.second)
    }

    private fun applyTime(newTime: ClockTime) {
        time = newTime
        updateContentDescription()
    }

    private fun updateContentDescription() {
        if (time.minute != lastDescriptionMinute) {
            setContentDescriptionFromTime()
        }
    }

    private fun setContentDescriptionFromTime() {
        lastDescriptionMinute = time.minute
        val base = LocalTime.of(time.hour, time.minute).format(DESCRIPTION_FORMATTER)
        contentDescription = nextAlarm?.takeIf { alarmAngleDeg() != null }?.let { alarm ->
            val alarmWall = alarm.atZone(clock.zone)
            val alarmText = LocalTime.of(alarmWall.hour, alarmWall.minute).format(DESCRIPTION_FORMATTER)
            resources.getString(R.string.clock_alarm_description_format, base, alarmText)
        } ?: base
    }

    private fun alarmAngleDeg(): Float? = AlarmHand.angleDeg(clock.instant(), nextAlarm, clock.zone)

    private var dialBitmap: Bitmap? = null
    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f

    private val dialBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val numeralPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val handBatonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val handTipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val alarmHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val secondHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val centerCapRingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerCapPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val timeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context, intent: Intent) {
            applyTime(readTime())
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
        }
        context.registerReceiver(timeChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        if (visibility == VISIBLE) start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.unregisterReceiver(timeChangeReceiver)
        stop()
    }

    // Both visibility callbacks are wired to the same dispatch: onVisibilityAggregated is the
    // correct modern signal, but onVisibilityChanged is kept alongside it because the aggregated
    // callback alone was not observed to fire reliably for a directly-set Activity content view
    // under Robolectric. start()/stop() are idempotent, so a duplicate call from both firing is
    // harmless.
    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        onVisibilityUpdated(isVisible)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView === this) {
            onVisibilityUpdated(visibility == VISIBLE)
        }
    }

    private fun onVisibilityUpdated(isVisible: Boolean) {
        if (isVisible) start() else stop()
    }

    fun start() {
        tickScheduler.start {
            applyTime(readTime())
            invalidate()
        }
    }

    fun stop() {
        tickScheduler.stop()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthSize, heightSize)
            return
        }

        if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            val defaultSize = resources.getDimensionPixelSize(R.dimen.clock_default_size)
            setMeasuredDimension(defaultSize, defaultSize)
            return
        }

        val boundedWidth = if (widthMode == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE else widthSize
        val boundedHeight = if (heightMode == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE else heightSize
        val square = minOf(boundedWidth, boundedHeight)
        setMeasuredDimension(square, square)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateDialCache()
        radius = minOf(w, h) / 2f
        centerX = w / 2f
        centerY = h / 2f
        dialBitmap = buildDialBitmap(w, h)
    }

    private fun invalidateDialCache() {
        dialBitmap?.recycle()
        dialBitmap = null
    }

    private fun buildDialBitmap(w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        dialBackgroundPaint.color = palette.dialBackground
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), dialBackgroundPaint)

        bezelPaint.color = palette.bezelRing
        bezelPaint.strokeWidth = 0.02f * radius
        canvas.drawCircle(centerX, centerY, radius, bezelPaint)

        tickPaint.color = palette.tickMinute
        tickPaint.strokeWidth = 0.012f * radius
        canvas.drawLines(toAbsolute(ClockGeometry.minorTickLines(radius)), tickPaint)

        tickPaint.color = palette.numeral
        tickPaint.strokeWidth = 0.02f * radius
        canvas.drawLines(toAbsolute(ClockGeometry.hourTickLines(radius)), tickPaint)

        numeralPaint.color = palette.numeral
        numeralPaint.textSize = 0.16f * radius
        val fontMetrics = numeralPaint.fontMetrics
        val textBaselineOffset = (fontMetrics.ascent + fontMetrics.descent) / 2
        for (hour in 1..12) {
            val p = ClockGeometry.numeralCenter(hour, radius)
            canvas.drawText(hour.toString(), centerX + p.x, centerY + p.y - textBaselineOffset, numeralPaint)
        }

        return bitmap
    }

    private fun toAbsolute(lines: FloatArray): FloatArray {
        val absolute = FloatArray(lines.size)
        for (i in lines.indices step 2) {
            absolute[i] = lines[i] + centerX
            absolute[i + 1] = lines[i + 1] + centerY
        }
        return absolute
    }

    override fun onDraw(canvas: Canvas) {
        dialBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        alarmAngleDeg()?.let {
            drawTippedHand(canvas, HandKind.ALARM, it, alarmHandPaint, palette.alarmHand, palette.alarmTip)
        }
        val angles = time.toHandAngles()
        drawTippedHand(canvas, HandKind.HOUR, angles.hourDeg, handBatonPaint, palette.handBaton, palette.handTip)
        drawTippedHand(canvas, HandKind.MINUTE, angles.minuteDeg, handBatonPaint, palette.handBaton, palette.handTip)
        drawSecondHand(canvas, angles.secondDeg)
        drawCenterCap(canvas)
    }

    private fun drawTippedHand(
        canvas: Canvas,
        kind: HandKind,
        angleDeg: Float,
        batonPaint: Paint,
        batonColor: Int,
        tipColor: Int,
    ) {
        val rect = ClockGeometry.handRect(kind, radius)
        canvas.save()
        canvas.rotate(angleDeg, centerX, centerY)

        drawHandLine(canvas, rect.tailY, rect.tipY, rect.width, batonPaint, batonColor)

        val tip = ClockGeometry.tipSegment(kind, radius)
        drawHandLine(canvas, tip.startY, tip.endY, tip.width, handTipPaint, tipColor)

        canvas.restore()
    }

    private fun drawSecondHand(canvas: Canvas, angleDeg: Float) {
        val rect = ClockGeometry.handRect(HandKind.SECOND, radius)
        canvas.save()
        canvas.rotate(angleDeg, centerX, centerY)

        drawHandLine(canvas, rect.tailY, rect.tipY, rect.width, secondHandPaint, palette.secondHand)

        canvas.restore()
    }

    private fun drawHandLine(canvas: Canvas, fromY: Float, toY: Float, width: Float, paint: Paint, color: Int) {
        paint.color = color
        paint.strokeWidth = width
        canvas.drawLine(centerX, centerY + fromY, centerX, centerY + toY, paint)
    }

    private fun drawCenterCap(canvas: Canvas) {
        val capRadius = ClockGeometry.centerCapRadius(radius)
        centerCapRingPaint.color = palette.centerCapRing
        canvas.drawCircle(centerX, centerY, capRadius * 1.6f, centerCapRingPaint)
        centerCapPaint.color = palette.secondHand
        canvas.drawCircle(centerX, centerY, capRadius, centerCapPaint)
    }

    private companion object {
        val DESCRIPTION_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
