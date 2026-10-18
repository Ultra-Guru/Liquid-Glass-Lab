package com.ultraguru.liquidglass.plugin.qwea0.scenes

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.liquidglass.GlassMaterial
import com.example.liquidglass.LiquidGlassView
import kotlin.math.cos
import kotlin.math.sin

/**
 * 场景 4：生物拟态（qwea0）。
 *
 * 两类有机形态：
 * 1. 中央「水银融合」面板——setPrimaryShape（底部胶囊 dock）+
 *    setSecondaryShape（绕行圆滴）：两形状以 smin 平滑并集融合，
 *    靠近时边缘像水银一样黏连合并（API 33+ 透镜管线）；开
 *    refractionOutward 走凸透镜模式（形状外的内容先被弯进边缘）。
 * 2. 周围「呼吸细胞」——每颗是独立 LiquidGlassView，用 setCornerRadii
 *    逐角圆角 + 相位差的正弦调制做有机形变，配合平移漂移与传感器高光。
 */
@Composable
internal fun BiomorphicScene() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> buildBiomorphicScene(ctx) }
    )
}

internal fun buildBiomorphicScene(context: Context): View {
    fun dp(v: Int): Int = context.dp(v)
    fun dpF(v: Float): Float = context.dpF(v)

    val root = FrameLayout(context)
    root.addView(PaintedBackdropView(context), FrameLayout.LayoutParams(MATCH, MATCH))
    root.addView(
        sceneHint(context, "水银融合（smin 黏连）· 逐角圆角呼吸 · 传感器高光"),
        FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(18)
        }
    )

    // ── 中央融合面板：主形状 = 胶囊 dock，副形状 = 绕行圆滴 ──
    val panelW = dp(320)
    val panelH = dp(330)
    val w = panelW.toFloat()
    val h = panelH.toFloat()
    val dockH = dpF(70f)
    val dockHalfW = dpF(118f)

    val mergeGlass = LiquidGlassView(context).apply {
        enableDynamicBackground = true
        refractionOutward = true
        dispersionStrength = 0.20f
        bevelWidth = dpF(40f)
        refractionHeight = dpF(110f)
        setPrimaryShape(
            RectF(
                w / 2f - dockHalfW,
                h - dockH - dpF(18f),
                w / 2f + dockHalfW,
                h - dpF(18f)
            ),
            dockH / 2f
        )
    }
    root.addView(
        mergeGlass,
        FrameLayout.LayoutParams(panelW, panelH, Gravity.CENTER)
    )

    // 副形状绕行：掠过 dock 上沿时 smin 融合（黏连 → 分离）
    val bubbleR = dpF(42f)
    fun applyBubble(bx: Float, by: Float) {
        mergeGlass.setSecondaryShape(
            RectF(bx - bubbleR, by - bubbleR, bx + bubbleR, by + bubbleR),
            bubbleR,
            dpF(44f)
        )
    }
    applyBubble(w / 2f, h * 0.30f)
    val orbit = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 12_000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { anim ->
            val t = anim.animatedValue as Float * 2f * Math.PI.toFloat()
            applyBubble(
                w / 2f + cos(t) * dpF(92f),
                h * 0.30f + sin(t) * dpF(66f)
            )
        }
    }
    lifecycleBound(mergeGlass, orbit)

    // ── 呼吸细胞群（逐角圆角正弦调制 + 平移漂移） ──
    breatheCell(root, sizeDp = 96, gravity = Gravity.TOP or Gravity.START, mLeft = dp(22), mTop = dp(64))
    breatheCell(root, sizeDp = 72, gravity = Gravity.TOP or Gravity.END, mLeft = 0, mTop = dp(92), mRight = dp(26))
    breatheCell(root, sizeDp = 84, gravity = Gravity.BOTTOM or Gravity.START, mLeft = dp(26), mTop = 0, mBottom = dp(112))
    breatheCell(root, sizeDp = 62, gravity = Gravity.BOTTOM or Gravity.END, mLeft = 0, mTop = 0, mRight = dp(30), mBottom = dp(132))

    root.addView(
        sceneHint(context, "副形状绕行至主形状附近时边缘黏连合并（Liquid Glass 2.0 · API 33+）")
            .apply { textSize = 11f },
        FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(20)
        }
    )

    return root
}

/**
 * 一颗「呼吸细胞」：逐角圆角按相位差的正弦调制（有机形变），视图同时
 * 小幅平移漂移。细胞与融合面板、细胞互相之间保持距离，避免玻璃
 * 折射玻璃的嵌套采样。
 */
private fun breatheCell(
    root: FrameLayout,
    sizeDp: Int,
    gravity: Int,
    mLeft: Int,
    mTop: Int,
    mRight: Int = 0,
    mBottom: Int = 0
) {
    val context = root.context
    fun dp(v: Int): Int = context.dp(v)
    fun dpF(v: Float): Float = context.dpF(v)

    val cell = LiquidGlassView(context).apply {
        enableDynamicBackground = true
        enableSensorHighlight = true
        dispersionStrength = 0.16f
        if (sizeDp < 80) material = GlassMaterial.CLEAR
        cornerRadius = dpF(sizeDp * 0.38f)
    }
    root.addView(
        cell,
        FrameLayout.LayoutParams(dp(sizeDp), dp(sizeDp), gravity).apply {
            leftMargin = mLeft
            topMargin = mTop
            rightMargin = mRight
            bottomMargin = mBottom
        }
    )

    val cap = dpF(sizeDp / 2f)
    val base = dpF(sizeDp * 0.34f)
    val amp = dpF(sizeDp * 0.16f)
    val drift = dpF(sizeDp * 0.22f)
    val phase = sizeDp * 0.07f
    val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3200L + (sizeDp * 10L)
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener { a ->
            val t = a.animatedValue as Float * 2f * Math.PI.toFloat()
            // 逐角圆角呼吸：四角相位错开 → 圆角在四角间流动，形似细胞
            cell.setCornerRadii(
                (base + amp * sin(t)).coerceIn(0f, cap),
                (base + amp * sin(t + 1.6f)).coerceIn(0f, cap),
                (base + amp * sin(t + 3.1f)).coerceIn(0f, cap),
                (base + amp * sin(t + 4.7f)).coerceIn(0f, cap)
            )
            cell.translationX = drift * sin(t + phase)
            cell.translationY = drift * 0.7f * sin(t + phase + 1.2f)
        }
    }
    lifecycleBound(cell, anim)
}

/** 动画跟随视图挂载生命周期（挂载即启、离屏即停，防泄漏）。 */
private fun lifecycleBound(view: View, animator: ValueAnimator) {
    view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            animator.start()
        }

        override fun onViewDetachedFromWindow(v: View) {
            animator.cancel()
        }
    })
}
