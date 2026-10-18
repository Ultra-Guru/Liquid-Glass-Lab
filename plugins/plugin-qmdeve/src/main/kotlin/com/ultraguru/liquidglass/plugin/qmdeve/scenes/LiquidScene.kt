package com.ultraguru.liquidglass.plugin.qmdeve.scenes

import android.animation.ValueAnimator
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.qmdeve.liquidglass.widget.LiquidGlassView

/**
 * 场景 3（QMDeve）：液体。
 *
 * - 主液滴 + 卫星小液滴都是 LiquidGlassView：
 *   `setDraggableEnabled(true)` 手指直接拖动（限位在父容器内）、
 *   `setElasticEnabled(true)` 启用 LiquidTracker —— 拖拽速度驱动
 *   液滴沿运动方向拉伸 / 垂直压缩（弹簧回弹，iOS 26 液体感）；
 * - `setDispersion(0.9)` 色散拉满 + 光斑持续漂移 → 彩虹折射边缘；
 * - 主液滴的 `refractionOffset` 用 ValueAnimator 呼吸（60~100dp）。
 */
internal object LiquidScene {

    fun create(context: Context): View {
        val root = BaseScene(context)

        // ── 采样源：漂移光斑背景 ───────────────────────────────────
        val source = FrameLayout(context)
        source.addView(GlassBackdropView(context), matchLP())
        root.addView(source, matchLP())

        // ── 主液滴（可拖拽 + 弹性 + 色散） ─────────────────────────
        val droplet = LiquidGlassView(context).apply {
            layoutParams = FrameLayout.LayoutParams(context.dpi(150), context.dpi(150), Gravity.CENTER).apply {
                topMargin = -context.dpi(60)
            }
            setCornerRadius(context.dpf(75f))
            setRefractionHeight(context.dpf(50f))
            setRefractionOffset(context.dpf(90f))
            setDispersion(0.9f)
            setBlurRadius(1.5f)
            setTintAlpha(0.08f)
            setDraggableEnabled(true)
            setElasticEnabled(true)
        }
        droplet.addView(NucleusView(context), matchLP())
        root.addView(droplet)
        droplet.bind(source)

        // ── 卫星小液滴（同样可拖拽） ───────────────────────────────
        val satellite = LiquidGlassView(context).apply {
            layoutParams = FrameLayout.LayoutParams(context.dpi(92), context.dpi(92), Gravity.CENTER).apply {
                topMargin = context.dpi(90)
                leftMargin = context.dpi(64)
            }
            setCornerRadius(context.dpf(46f))
            setRefractionHeight(context.dpf(26f))
            setRefractionOffset(context.dpf(50f))
            setDispersion(0.7f)
            setBlurRadius(1f)
            setTintAlpha(0.06f)
            setDraggableEnabled(true)
            setElasticEnabled(true)
        }
        satellite.addView(
            NucleusView(context).apply { nucleusColor = 0x4DFFFFFF },
            matchLP()
        )
        root.addView(satellite)
        satellite.bind(source)

        // ── 主液滴折射呼吸（refractionOffset 60↔100dp 循环） ───────
        val pulse = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1800L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                droplet.setRefractionOffset(context.dpf(60f + 40f * (it.animatedValue as Float)))
            }
        }
        pulse.start()
        root.onTeardown { pulse.cancel() }

        // ── 指引文字 ────────────────────────────────────────────────
        val hint = context.text("拖动液滴 · 松手弹性回弹\nLiquidTracker 弹簧物理 + 色散折射", 14f, 0xB3FFFFFF)
        val hintLP = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply { bottomMargin = context.dpi(40) }
        root.addView(hint, hintLP)
        root.onInsets {
            hint.layoutParams = (hint.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = root.insetBottom + context.dpi(32)
            }
            hint.requestLayout()
        }

        return root
    }
}
