package com.ultraguru.liquidglass.plugin.qwea0.scenes

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin

/**
 * qwea0 插件四场景共享的「演示环境」。
 *
 * 库是 View 体系（LiquidGlassView 捕获直接父容器 / backdropSource），
 * 因此各场景统一结构：根 FrameLayout → 自绘背景（PaintedBackdropView）
 * → 玻璃组件叠加。背景必须提供高频结构（网格 + 彩色光斑），否则
 * SDF 折射与色散在纯渐变上不可见（库文档明确的集成要求）。
 */

/** dp → px（Context 扩展；场景构建函数内再周部 alias 成无参前缀调用）。 */
internal fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

/** dp → px 浮点（Context 扩展）。 */
internal fun Context.dpF(v: Float): Float = v * resources.displayMetrics.density

/**
 * 演示背景：近黑深色渐变 + 缓慢漂移的彩色光斑 + 细网格。
 *
 * 玻璃的折射/色散只在「背景有高频内容」时可见——网格线提供边缘密度，
 * 光斑提供彩色对比；ValueAnimator 驱动整个背景漂移（玻璃须开
 * enableDynamicBackground 才能逐帧重捕获）。
 */
internal class PaintedBackdropView(context: Context) : View(context) {

    private class Blob(
        val cx: Float,
        val cy: Float,
        val r: Float,
        val argb: Int,
        val phaseOff: Float,
        val drift: Float
    )

    private val blobs = listOf(
        Blob(0.22f, 0.16f, 0.42f, 0xFFE0524A.toInt(), 0.0f, 1.2f),
        Blob(0.80f, 0.28f, 0.38f, 0xFFFFB25E.toInt(), 1.2f, 0.9f),
        Blob(0.62f, 0.82f, 0.46f, 0xFF2EC4B6.toInt(), 2.4f, 1.4f),
        Blob(0.12f, 0.74f, 0.32f, 0xFF7C5CBF.toInt(), 3.6f, 0.8f),
        Blob(0.44f, 0.46f, 0.34f, 0xFF3E7CB1.toInt(), 4.7f, 1.0f)
    )

    private var phase = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 24_000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    private val basePaint = Paint()
    private val blobPaint = Paint()
    private val gridPaint = Paint().apply {
        color = 0x0EFFFFFF
        strokeWidth = 1f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        basePaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0xFF1A0608.toInt(), 0xFF0A0304.toInt(), 0xFF120309.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        canvas.drawRect(0f, 0f, w, h, basePaint)

        // 细网格：高频结构 → 透镜折射环与色散边纹肉眼可辨
        val step = 32f * resources.displayMetrics.density
        var gx = 0f
        while (gx <= w) {
            canvas.drawLine(gx, 0f, gx, h, gridPaint)
            gx += step
        }
        var gy = 0f
        while (gy <= h) {
            canvas.drawLine(0f, gy, w, gy, gridPaint)
            gy += step
        }

        // 彩色光斑（正弦漂移，相位各异）
        val t = phase * 2f * Math.PI.toFloat()
        val minDim = minOf(w, h)
        blobs.forEach { b ->
            val dx = sin(t + b.phaseOff) * b.drift * 0.05f
            val dy = sin(t * 0.8f + b.phaseOff * 1.7f) * b.drift * 0.04f
            val cx = w * (b.cx + dx)
            val cy = h * (b.cy + dy)
            val r = minDim * b.r
            blobPaint.shader = RadialGradient(
                cx, cy, r,
                intArrayOf(b.argb, b.argb and 0x00FFFFFF),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r, blobPaint)
        }
    }
}

/**
 * 代码绘制的矢量图标（24×24 路径空间，随 bounds 等比缩放）。
 *
 * 插件禁止引用自身 R 资源，全部图形以 Path 构造；实现 setColorFilter
 * 以便 ImageView 的 imageTintList（LiquidGlassFab 图标着色）生效。
 */
internal class CodeIconDrawable(
    color: Int,
    private val path: Path
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        canvas.save()
        canvas.scale(b.width() / 24f, b.height() / 24f)
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** 把 24×24 路径包装成白色图标。 */
internal fun Path.asIcon(color: Int = Color.WHITE): Drawable = CodeIconDrawable(color, this)

/** 图标库（纯代码路径，无资源引用）。 */
internal object CodeIcons {

    /** 房子。 */
    fun home(): Path = Path().apply {
        moveTo(12f, 3f)
        lineTo(21f, 11f)
        lineTo(19.2f, 11f)
        lineTo(19.2f, 20f)
        lineTo(14.4f, 20f)
        lineTo(14.4f, 14.4f)
        lineTo(9.6f, 14.4f)
        lineTo(9.6f, 20f)
        lineTo(4.8f, 20f)
        lineTo(4.8f, 11f)
        lineTo(3f, 11f)
        close()
    }

    /** 放大镜。 */
    fun search(): Path = Path().apply {
        addCircle(10.4f, 10.4f, 6.2f, Path.Direction.CW)
        moveTo(14.9f, 13.2f)
        lineTo(20.3f, 18.6f)
        lineTo(18.6f, 20.3f)
        lineTo(13.2f, 14.9f)
        close()
    }

    /** 五角星。 */
    fun star(): Path = Path().apply {
        val cx = 12f
        val cy = 12.7f
        val outer = 9.2f
        val inner = 3.9f
        for (i in 0 until 10) {
            val ang = -Math.PI / 2.0 + i * Math.PI / 5.0
            val r = if (i % 2 == 0) outer else inner
            val px = cx + (r * cos(ang)).toFloat()
            val py = cy + (r * sin(ang)).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }

    /** 人形。 */
    fun person(): Path = Path().apply {
        addCircle(12f, 7.8f, 4.1f, Path.Direction.CW)
        addOval(RectF(4.6f, 13.4f, 19.4f, 21.4f), Path.Direction.CW)
    }

    /** 心形。 */
    fun heart(): Path = Path().apply {
        addCircle(7.9f, 9.1f, 4.6f, Path.Direction.CW)
        addCircle(16.1f, 9.1f, 4.6f, Path.Direction.CW)
        moveTo(4.3f, 10.6f)
        lineTo(12f, 21f)
        lineTo(19.7f, 10.6f)
        close()
    }

    /** 加号。 */
    fun plus(): Path = Path().apply {
        moveTo(10.6f, 4f)
        lineTo(13.4f, 4f)
        lineTo(13.4f, 10.6f)
        lineTo(20f, 10.6f)
        lineTo(20f, 13.4f)
        lineTo(13.4f, 13.4f)
        lineTo(13.4f, 20f)
        lineTo(10.6f, 20f)
        lineTo(10.6f, 13.4f)
        lineTo(4f, 13.4f)
        lineTo(4f, 10.6f)
        lineTo(10.6f, 10.6f)
        close()
    }

    /** 四芒星（火花）。 */
    fun spark(): Path = Path().apply {
        moveTo(12f, 2f)
        lineTo(14f, 10f)
        lineTo(22f, 12f)
        lineTo(14f, 14f)
        lineTo(12f, 22f)
        lineTo(10f, 14f)
        lineTo(2f, 12f)
        lineTo(10f, 10f)
        close()
    }
}

/** 场景顶部提示文字（半透明白 + 阴影）。 */
internal fun sceneHint(context: Context, text: CharSequence): TextView =
    TextView(context).apply {
        this.text = text
        textSize = 13f
        setTextColor(0xCCFFFFFF.toInt())
        gravity = Gravity.CENTER
        setShadowLayer(6f, 0f, 1f, 0x66000000)
        setPadding(context.dp(24), 0, context.dp(24), 0)
    }

/** 场景主标题（居中，加粗）。 */
internal fun sceneTitle(context: Context, text: CharSequence): TextView =
    TextView(context).apply {
        this.text = text
        textSize = 20f
        setTextColor(Color.WHITE)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setShadowLayer(8f, 0f, 2f, 0x66000000)
    }

/**
 * 把若干 View 的 bottomMargin 抬到系统导航栏之上（edge-to-edge 场景）。
 * 若 insets 未分发到该子树，则保持创建时设置的初始 margin（fallback）。
 */
internal fun liftAboveSystemBars(root: View, basePx: Int, vararg targets: View) {
    root.setOnApplyWindowInsetsListener { _, insets ->
        val bottom = insets.getInsets(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
        ).bottom
        targets.forEach { t ->
            (t.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                if (lp.bottomMargin != basePx + bottom) {
                    lp.bottomMargin = basePx + bottom
                    t.requestLayout()
                }
            }
        }
        insets
    }
}

/** MATCH_PARENT 常量（压缩样板）。 */
internal val MATCH = FrameLayout.LayoutParams.MATCH_PARENT

/** WRAP_CONTENT 常量（压缩样板）。 */
internal val WRAP = FrameLayout.LayoutParams.WRAP_CONTENT
