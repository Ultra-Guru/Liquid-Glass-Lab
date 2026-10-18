package com.ultraguru.liquidglass.plugin.qmdeve.scenes

import android.animation.ValueAnimator
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import com.qmdeve.liquidglass.widget.LiquidGlassView
import kotlin.math.cos
import kotlin.math.sin

/**
 * 场景 4（QMDeve）：生物拟态。
 *
 * 6 个玻璃「细胞」围绕中心呼吸：
 * - 呼吸动画（ValueAnimator REVERSE）驱动整体缩放 + 轨道微移（View 属性，零重采样成本）；
 * - 两个细胞同时做 `setCornerRadius` 形变（R 的 62%↔100%，
 *   每次 setter 走 updateConfig 重编译 uniform —— 有意演示其形变能力与代价）；
 * - `setDispersion(0.9)` 的细胞在光斑上呈现彩虹色散边缘。
 */
internal object BiomorphicScene {

    private class Cell(
        val sizeDp: Int,
        val angleDeg: Float,
        val distDp: Int,
        val dispersion: Float,
        val morph: Boolean,
        val phase: Float
    )

    private val cells = listOf(
        Cell(150, 0f, 0, 0.5f, false, 0f),
        Cell(92, 40f, 170, 0.6f, true, 1.1f),
        Cell(74, 150f, 150, 0.9f, false, 2.3f),
        Cell(104, 240f, 185, 0.5f, true, 3.6f),
        Cell(60, 310f, 140, 0.7f, false, 4.7f),
        Cell(48, 95f, 225, 0.9f, false, 5.5f)
    )

    fun create(context: Context): View {
        val root = BaseScene(context)

        // ── 采样源 ─────────────────────────────────────────────────
        val source = FrameLayout(context)
        source.addView(GlassBackdropView(context), matchLP())
        root.addView(source, matchLP())

        // ── 标题 ───────────────────────────────────────────────────
        root.addView(
            context.text("玻璃细胞群", 20f, bold = true),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
                .apply { topMargin = context.dpi(24) }
        )
        root.addView(
            context.text("有机形态 · 圆角形变 · 色散细胞", 12f, 0x99FFFFFF),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
                .apply { topMargin = context.dpi(50) }
        )

        // ── 细胞群 ─────────────────────────────────────────────────
        val cellViews = ArrayList<LiquidGlassView>(cells.size)
        val baseX = FloatArray(cells.size)
        val baseY = FloatArray(cells.size)
        var poseInitialized = false

        fun applyPose(t: Float) {
            if (cellViews.size < cells.size) return
            cells.forEachIndexed { i, cell ->
                val view = cellViews[i]
                val wob = sin(2f * Math.PI.toFloat() * t + cell.phase)
                // 呼吸缩放 + 轨道微移（View 属性，不触发重采样）
                view.scaleX = 1f + 0.10f * wob
                view.scaleY = 1f + 0.10f * wob
                view.translationX = baseX[i] + context.dpf(8f) * wob * cos(Math.toRadians(cell.angleDeg.toDouble())).toFloat()
                view.translationY = baseY[i] + context.dpf(8f) * wob * sin(Math.toRadians(cell.angleDeg.toDouble())).toFloat()
                // 圆角形变：R 的 62% ↔ 100%（height/2 为上限）
                if (cell.morph) {
                    val half = context.dpf(cell.sizeDp * 0.5f)
                    view.setCornerRadius(half * (0.62f + 0.38f * (0.5f + 0.5f * wob)))
                }
            }
        }

        // 舞台：首帧 onLayout 即初始化基准位置（细胞用 translation 定位，
        // 避免第一帧堆在左上角闪一下）
        val stage = object : FrameLayout(context) {
            override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
                super.onLayout(changed, left, top, right, bottom)
                if (!poseInitialized && width > 0 && height > 0) {
                    poseInitialized = true
                    val cx = width / 2f
                    val cy = height / 2f
                    cells.forEachIndexed { i, cell ->
                        val rad = Math.toRadians(cell.angleDeg.toDouble())
                        baseX[i] = cx + (context.dpf(cell.distDp.toFloat()) * cos(rad)).toFloat() - context.dpf(cell.sizeDp * 0.5f)
                        baseY[i] = cy + (context.dpf(cell.distDp.toFloat()) * sin(rad)).toFloat() - context.dpf(cell.sizeDp * 0.5f)
                    }
                    applyPose(0f)
                }
            }
        }
        root.addView(
            stage,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                .apply {
                    topMargin = context.dpi(76)
                    bottomMargin = context.dpi(56)
                }
        )

        cells.forEach { cell ->
            val glass = LiquidGlassView(context).apply {
                // TOP|START：最终位置完全由 translation 控制（applyPose）
                layoutParams = FrameLayout.LayoutParams(context.dpi(cell.sizeDp), context.dpi(cell.sizeDp), Gravity.TOP or Gravity.START)
                setCornerRadius(context.dpf(cell.sizeDp * 0.5f))
                setRefractionHeight(context.dpf(cell.sizeDp * 0.16f).coerceIn(context.dpf(12f), context.dpf(50f)))
                setRefractionOffset(context.dpf(cell.sizeDp * 0.55f).coerceIn(context.dpf(20f), context.dpf(120f)))
                setDispersion(cell.dispersion)
                setBlurRadius(3f)
                setTintAlpha(0.12f)
            }
            glass.addView(NucleusView(context), matchLP())
            stage.addView(glass)
            glass.bind(source)
            cellViews += glass
        }

        val breath = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2600L
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { applyPose(it.animatedValue as Float) }
        }
        breath.start()
        root.onTeardown { breath.cancel() }

        // ── 页脚 ─────────────────────────────────────────────────────
        val footer = context.text("圆角由 setCornerRadius 实时形变 · dispersion 拉满呈彩虹边缘", 11f, 0x80FFFFFF)
        root.addView(
            footer,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                .apply { bottomMargin = context.dpi(20) }
        )
        root.onInsets {
            footer.layoutParams = (footer.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = root.insetBottom + context.dpi(12)
            }
            footer.requestLayout()
        }

        return root
    }
}
