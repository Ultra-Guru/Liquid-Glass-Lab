package com.ultraguru.liquidglass.plugin.qmdeve.scenes

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin

/* ────────────────────────────────────────────────────────────────────────
 * qmdeve 插件公共件（纯 View 体系，零资源引用 —— 一切代码绘制）：
 *  - [BaseScene]   场景根容器：insets 跟踪 + 卸载钩子
 *  - [GlassBackdropView]  深色渐变 + 漂移光斑背景（作为玻璃的采样源）
 *  - [IconView]    代码绘制的线性图标（星 / 搜索 / 首页 / 人像 …）
 *  - dp 工具与文本工厂
 * ──────────────────────────────────────────────────────────────────────── */

/** dp → px（浮点）。 */
internal fun Context.dpf(dp: Number): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics)

/** dp → px（取整；Number 放宽：Int/Float 字面量都可直传）。 */
internal fun Context.dpi(dp: Number): Int = dpf(dp).toInt()

/** FrameLayout 的 MATCH_PARENT 布局参数（每次新实例）。 */
internal fun matchLP(): FrameLayout.LayoutParams = FrameLayout.LayoutParams(
    FrameLayout.LayoutParams.MATCH_PARENT,
    FrameLayout.LayoutParams.MATCH_PARENT
)

/** 通用白系文字。 */
internal fun Context.text(
    content: String,
    sizeSp: Float,
    color: Long = Color.WHITE.toLong(),
    bold: Boolean = false
): TextView = TextView(this).apply {
    text = content
    textSize = sizeSp
    setTextColor(color.toInt())
    gravity = Gravity.CENTER
    if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

/**
 * 场景根容器。
 *
 * - 跟踪 systemBars insets（宿主把场景嵌在标题栏下方；AndroidView 内的
 *   interop View 仍能收到 WindowInsets 分发），未收到时使用保守回退值；
 * - [onTeardown] 注册的清理动作在脱离窗口时执行（取消 ValueAnimator 等）。
 */
internal open class BaseScene(context: Context) : FrameLayout(context) {

    /** 已收到的 insets（px）。 */
    var insetTop: Int = 0
        private set
    var insetBottom: Int = 0
        private set

    private val insetCallbacks = mutableListOf<() -> Unit>()
    private val teardownActions = mutableListOf<() -> Unit>()

    init {
        setOnApplyWindowInsetsListener { _, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            insetTop = bars.top
            insetBottom = bars.bottom
            insetCallbacks.forEach { it() }
            insets
        }
    }

    /** insets 到达 / 变化时回调（用于调整顶部 / 底部锚定元素的边距）。 */
    fun onInsets(action: () -> Unit) {
        insetCallbacks += action
    }

    /** 场景销毁（脱离窗口）时执行的清理。 */
    fun onTeardown(action: () -> Unit) {
        teardownActions += action
    }

    override fun onDetachedFromWindow() {
        teardownActions.forEach { it() }
        super.onDetachedFromWindow()
    }
}

/**
 * 深色渐变 + 漂移彩色光斑背景 —— 五插件统一的品牌视觉。
 * 自带 24s 循环动画并 postInvalidateOnAnimation：
 * LiquidGlassView 的 PreDrawListener 每帧重采样本 View，
 * 于是玻璃面的折射 / 色散随光斑漂移持续变化（QMDeve 采样的就是 View 树）。
 */
internal class GlassBackdropView(context: Context) : View(context) {

    private data class Blob(val cx: Float, val cy: Float, val r: Float, val color: Int, val alpha: Float)

    private val blobs = listOf(
        Blob(0.22f, 0.18f, 0.42f, 0xFFE0524A.toInt(), 0.75f),
        Blob(0.80f, 0.30f, 0.38f, 0xFFFFB25E.toInt(), 0.55f),
        Blob(0.62f, 0.78f, 0.44f, 0xFF2EC4B6.toInt(), 0.50f),
        Blob(0.14f, 0.72f, 0.30f, 0xFF7C5CBF.toInt(), 0.55f),
        Blob(0.45f, 0.48f, 0.30f, 0xFF3E7CB1.toInt(), 0.35f)
    )

    private var phase = 0f
    private var lastW = -1
    private var lastH = -1
    private val basePaint = Paint()
    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 24_000L
        interpolator = android.view.animation.LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        addUpdateListener {
            phase = it.animatedValue as Float
            postInvalidateOnAnimation()
        }
    }

    init {
        basePaint.isDither = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        if (lastW != width || lastH != height) {
            basePaint.shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(0xFF1A0708.toInt(), 0xFF0B0405.toInt(), 0xFF120306.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            lastW = width
            lastH = height
        }
        canvas.drawRect(0f, 0f, w, h, basePaint)

        blobs.forEachIndexed { i, b ->
            val t = phase * 6.28318f
            val cx = when (i) {
                0 -> w * (b.cx + 0.05f * sin(t))
                1 -> w * b.cx
                else -> w * b.cx
            }
            val cy = when (i) {
                1 -> h * (b.cy + 0.06f * sin(t + 1.2f))
                else -> h * b.cy
            }
            val r = when (i) {
                3 -> minOf(w, h) * (b.r + 0.03f * sin(t + 2.4f))
                else -> minOf(w, h) * b.r
            }
            val argb = (((b.alpha * 255).toInt()) shl 24) or (b.color and 0xFFFFFF)
            blobPaint.shader = RadialGradient(
                cx, cy, r,
                intArrayOf(argb, 0x00000000),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r, blobPaint)
        }
    }
}

/**
 * 纯代码绘制的线性图标（笔画风格，2dp 圆头）。
 * QMDeve 是 View 体系插件且禁用资源引用 —— 图标一律 Canvas path 手绘。
 */
internal class IconView(context: Context, val glyph: Glyph) : View(context) {

    enum class Glyph { STAR, SEARCH, HOME, PERSON, HEART, BELL, SHARE, THUMB_UP, REFRESH }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = Color.WHITE
    }
    private val path = Path()
    private val rect = RectF()

    fun setIconColor(color: Long) {
        paint.color = color.toInt()
        fillPaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        paint.strokeWidth = 0.075f * minOf(w, h)
        when (glyph) {
            Glyph.STAR -> {
                val cx = 0.5f * w
                val cy = 0.52f * h
                val outer = 0.48f * minOf(w, h)
                val inner = 0.20f * minOf(w, h)
                path.reset()
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outer else inner
                    val theta = Math.toRadians(-90.0 + i * 36.0)
                    val x = cx + (r * cos(theta)).toFloat()
                    val y = cy + (r * sin(theta)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, fillPaint)
            }
            Glyph.SEARCH -> {
                val r = 0.24f * minOf(w, h)
                rect.set(0.30f * w - r, 0.30f * h - r, 0.30f * w + r, 0.30f * h + r)
                canvas.drawArc(rect, 0f, 360f, false, paint)
                canvas.drawLine(0.48f * w, 0.48f * h, 0.82f * w, 0.82f * h, paint)
            }
            Glyph.HOME -> {
                path.reset()
                path.moveTo(0.50f * w, 0.08f * h)
                path.lineTo(0.90f * w, 0.42f * h)
                path.lineTo(0.76f * w, 0.42f * h)
                path.lineTo(0.76f * w, 0.90f * h)
                path.lineTo(0.24f * w, 0.90f * h)
                path.lineTo(0.24f * w, 0.42f * h)
                path.lineTo(0.10f * w, 0.42f * h)
                path.close()
                canvas.drawPath(path, paint)
                canvas.drawLine(0.42f * w, 0.90f * h, 0.42f * w, 0.60f * h, paint)
                canvas.drawLine(0.42f * w, 0.60f * h, 0.58f * w, 0.60f * h, paint)
                canvas.drawLine(0.58f * w, 0.60f * h, 0.58f * w, 0.90f * h, paint)
            }
            Glyph.PERSON -> {
                val r = 0.15f * minOf(w, h)
                canvas.drawCircle(0.50f * w, 0.28f * h, r, paint)
                rect.set(0.18f * w, 0.52f * h, 0.82f * w, 1.30f * h)
                canvas.drawArc(rect, 180f, 180f, false, paint)
            }
            Glyph.HEART -> {
                path.reset()
                path.moveTo(0.50f * w, 0.86f * h)
                path.cubicTo(0.08f * w, 0.54f * h, 0.16f * w, 0.14f * h, 0.50f * w, 0.32f * h)
                path.cubicTo(0.84f * w, 0.14f * h, 0.92f * w, 0.54f * h, 0.50f * w, 0.86f * h)
                path.close()
                canvas.drawPath(path, paint)
            }
            Glyph.BELL -> {
                path.reset()
                path.moveTo(0.24f * w, 0.64f * h)
                path.cubicTo(0.24f * w, 0.30f * h, 0.34f * w, 0.16f * h, 0.50f * w, 0.16f * h)
                path.cubicTo(0.66f * w, 0.16f * h, 0.76f * w, 0.30f * h, 0.76f * w, 0.64f * h)
                path.lineTo(0.84f * w, 0.72f * h)
                path.lineTo(0.16f * w, 0.72f * h)
                path.close()
                canvas.drawPath(path, paint)
                canvas.drawCircle(0.50f * w, 0.84f * h, 0.055f * minOf(w, h), paint)
            }
            Glyph.SHARE -> {
                val r = 0.10f * minOf(w, h)
                canvas.drawCircle(0.50f * w, 0.16f * h, r, paint)
                canvas.drawCircle(0.18f * w, 0.76f * h, r, paint)
                canvas.drawCircle(0.82f * w, 0.76f * h, r, paint)
                canvas.drawLine(0.44f * w, 0.24f * h, 0.24f * w, 0.67f * h, paint)
                canvas.drawLine(0.56f * w, 0.24f * h, 0.76f * w, 0.67f * h, paint)
            }
            Glyph.THUMB_UP -> {
                path.reset()
                path.moveTo(0.14f * w, 0.42f * h)
                path.lineTo(0.30f * w, 0.42f * h)
                path.lineTo(0.30f * w, 0.90f * h)
                path.lineTo(0.14f * w, 0.90f * h)
                path.close()
                canvas.drawPath(path, paint)
                path.reset()
                path.moveTo(0.32f * w, 0.46f * h)
                path.lineTo(0.42f * w, 0.46f * h)
                path.cubicTo(0.46f * w, 0.36f * h, 0.48f * w, 0.16f * h, 0.58f * w, 0.18f * h)
                path.cubicTo(0.66f * w, 0.20f * h, 0.64f * w, 0.32f * h, 0.60f * w, 0.40f * h)
                path.lineTo(0.78f * w, 0.42f * h)
                path.cubicTo(0.88f * w, 0.44f * h, 0.88f * w, 0.58f * h, 0.76f * w, 0.62f * h)
                path.lineTo(0.46f * w, 0.72f * h)
                path.cubicTo(0.38f * w, 0.75f * h, 0.32f * w, 0.68f * h, 0.32f * w, 0.60f * h)
                path.close()
                canvas.drawPath(path, paint)
            }
            Glyph.REFRESH -> {
                val r = 0.36f * minOf(w, h)
                val cx = 0.5f * w
                val cy = 0.5f * h
                rect.set(cx - r, cy - r, cx + r, cy + r)
                canvas.drawArc(rect, 300f, 290f, false, paint)
                // 箭头（终点在 300°+290°=590°→230°）
                val tipDeg = Math.toRadians((300.0 + 290.0))
                val tx = cx + (r * cos(tipDeg)).toFloat()
                val ty = cy + (r * sin(tipDeg)).toFloat()
                val inward = Math.toRadians(tipDeg - 180.0 + 35.0)
                val outward = Math.toRadians(tipDeg - 180.0 - 35.0)
                val ah = 0.16f * minOf(w, h)
                canvas.drawLine(tx, ty, tx + (ah * cos(inward)).toFloat(), ty + (ah * sin(inward)).toFloat(), paint)
                canvas.drawLine(tx, ty, tx + (ah * cos(outward)).toFloat(), ty + (ah * sin(outward)).toFloat(), paint)
            }
        }
    }
}

/**
 * 玻璃「细胞 / 液滴」的细胞核：match_parent 铺满 LiquidGlassView
 * （其 onLayout 把子 View 固定在左上角），内部自绘居中内容。
 */
internal class NucleusView(context: Context) : View(context) {

    var nucleusColor: Int = 0x66FFFFFF
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        paint.color = nucleusColor
        canvas.drawCircle(w / 2f, h / 2f, minOf(w, h) * 0.16f, paint)
        paint.color = 0x30FFFFFF
        canvas.drawCircle(w / 2f - minOf(w, h) * 0.10f, h / 2f - minOf(w, h) * 0.12f, minOf(w, h) * 0.06f, paint)
    }
}
