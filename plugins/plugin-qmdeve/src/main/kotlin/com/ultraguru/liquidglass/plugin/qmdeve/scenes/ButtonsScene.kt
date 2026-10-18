package com.ultraguru.liquidglass.plugin.qmdeve.scenes

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.qmdeve.liquidglass.widget.LiquidGlassView

/**
 * 场景 2（QMDeve）：按钮 UI。
 *
 * 全部按钮 = LiquidGlassView：
 * - `setTouchEffectEnabled(true)`：iOS 风格按压辉光（RadialGradient 跟手）
 *   + LiquidTracker 弹簧缩放（按下 1.02，松手回 1）；
 * - 胶囊 / 圆形由 `setCornerRadius(高度/2)` 实现；
 * - 玻璃后面采样漂移光斑，每个按钮的折射随位置不同而不同。
 */
internal object ButtonsScene {

    fun create(context: Context): View {
        val root = BaseScene(context)

        // ── 采样源：光斑背景 ──────────────────────────────────────
        val source = FrameLayout(context)
        source.addView(GlassBackdropView(context), matchLP())
        root.addView(source, matchLP())

        // ── 按钮矩阵（可滚动） ────────────────────────────────────
        val scroll = ScrollView(context).apply { isVerticalScrollBarEnabled = false }
        val list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dpi(24), context.dpi(26), context.dpi(24), context.dpi(40))
        }

        list.addView(
            context.text("玻璃按钮矩阵", 20f, bold = true).apply { gravity = Gravity.CENTER },
            wrapLP().apply { bottomMargin = context.dpi(4) }
        )
        list.addView(
            context.text("LiquidGlassView 按压辉光（touchEffect）", 12f, 0x99FFFFFF).apply {
                gravity = Gravity.CENTER
            },
            wrapLP().apply { bottomMargin = context.dpi(28) }
        )

        // 主按钮
        list.addView(
            glassButton(context, source, "主按钮 · Liquid", ViewGroup.LayoutParams.MATCH_PARENT, 56f),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpi(56))
                .apply { bottomMargin = context.dpi(14) }
        )

        // 次要 / 危险 / 禁用
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        listOf(Triple("次要", 0.35f, true), Triple("危险", 0.7f, true), Triple("禁用", 0.2f, false)).forEach { (label, dispersion, enabled) ->
            row.addView(
                glassButton(context, source, label, ViewGroup.LayoutParams.WRAP_CONTENT, 44f, dispersion)
                    .apply { if (enabled) setOnClickListener { } else alpha = 0.55f },
                LinearLayout.LayoutParams(context.dpi(88), context.dpi(44)).apply {
                    leftMargin = context.dpi(7)
                    rightMargin = context.dpi(7)
                }
            )
        }
        list.addView(
            row,
            wrapLP().apply { bottomMargin = context.dpi(28) }
        )

        // 图标按钮排
        val iconRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        listOf(
            IconView.Glyph.HEART, IconView.Glyph.BELL,
            IconView.Glyph.SHARE, IconView.Glyph.THUMB_UP
        ).forEach { glyph ->
            iconRow.addView(
                glassIconButton(context, source, glyph),
                LinearLayout.LayoutParams(context.dpi(56), context.dpi(56)).apply {
                    leftMargin = context.dpi(8)
                    rightMargin = context.dpi(8)
                }
            )
        }
        list.addView(
            iconRow,
            wrapLP().apply { bottomMargin = context.dpi(30) }
        )

        // 开关
        list.addView(
            context.text("玻璃开关", 12f, 0x99FFFFFF).apply { gravity = Gravity.CENTER },
            wrapLP().apply { bottomMargin = context.dpi(10) }
        )
        list.addView(glassToggle(context, source), wrapLP().apply { bottomMargin = context.dpi(34) })

        // FAB
        list.addView(
            context.text("FAB（按压发光形变）", 12f, 0x99FFFFFF).apply { gravity = Gravity.CENTER },
            wrapLP().apply { bottomMargin = context.dpi(12) }
        )
        list.addView(glassFab(context, source), wrapLP())

        scroll.addView(
            list,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        root.addView(scroll, matchLP())
        return root
    }

    /** 文字玻璃按钮：胶囊 + 按压辉光。 */
    private fun glassButton(
        context: Context,
        source: ViewGroup,
        label: String,
        width: Int,
        heightDp: Float,
        dispersion: Float = 0.35f
    ): LiquidGlassView {
        val heightPx = context.dpi(heightDp)
        return LiquidGlassView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                if (width == ViewGroup.LayoutParams.MATCH_PARENT) ViewGroup.LayoutParams.MATCH_PARENT else width,
                heightPx
            )
            setCornerRadius(context.dpf(heightDp / 2f))
            setRefractionHeight(context.dpf(26f))
            setRefractionOffset(context.dpf(60f))
            setDispersion(dispersion)
            setBlurRadius(8f)
            setTintAlpha(0.24f)
            setTintColorRed(0f)
            setTintColorGreen(0f)
            setTintColorBlue(0f)
            setTouchEffectEnabled(true)
            addView(
                context.text(label, if (heightDp >= 56f) 16f else 13f).apply {
                    gravity = Gravity.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                },
                matchLP()
            )
            bind(source)
        }
    }

    /** 图标玻璃按钮：圆形 + 按压辉光。 */
    private fun glassIconButton(context: Context, source: ViewGroup, glyph: IconView.Glyph): LiquidGlassView {
        return LiquidGlassView(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dpi(56), context.dpi(56))
            setCornerRadius(context.dpf(28f))
            setRefractionHeight(context.dpf(26f))
            setRefractionOffset(context.dpf(55f))
            setDispersion(0.45f)
            setBlurRadius(6f)
            setTintAlpha(0.20f)
            setTintColorRed(0f)
            setTintColorGreen(0f)
            setTintColorBlue(0f)
            setTouchEffectEnabled(true)
            val holder = FrameLayout(context)
            holder.addView(
                IconView(context, glyph),
                FrameLayout.LayoutParams(context.dpi(24), context.dpi(24), Gravity.CENTER)
            )
            addView(holder, matchLP())
            bind(source)
        }
    }

    /**
     * 玻璃开关：胶囊轨道（LiquidGlassView）+ 实心滑块。
     * LiquidGlassView 的 onLayout 把子 View 固定在左上，
     * 所以滑块放在铺满轨道的 FrameLayout 里用 gravity 定位。
     */
    private fun glassToggle(context: Context, source: ViewGroup): View {
        val trackWidth = context.dpi(76)
        val trackHeight = context.dpi(44)
        val thumbSize = context.dpi(34)
        val travel = trackWidth - thumbSize - context.dpi(10)

        var checked = true
        var animator: ValueAnimator? = null

        val track = LiquidGlassView(context).apply {
            layoutParams = LinearLayout.LayoutParams(trackWidth, trackHeight)
            setCornerRadius(context.dpf(22f))
            setRefractionHeight(context.dpf(22f))
            setRefractionOffset(context.dpf(45f))
            setDispersion(0.3f)
            setBlurRadius(6f)
            setTintAlpha(0.28f)
            setTintColorRed(0f)
            setTintColorGreen(0f)
            setTintColorBlue(0f)
        }
        val holder = FrameLayout(context)
        val thumb = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xE6FFFFFF)
            }
        }
        holder.addView(
            thumb,
            FrameLayout.LayoutParams(thumbSize, thumbSize, Gravity.CENTER_VERTICAL or Gravity.START)
                .apply { leftMargin = context.dpi(5) }
        )
        track.addView(holder, matchLP())
        track.bind(source)

        fun animateTo(on: Boolean) {
            animator?.cancel()
            val start = thumb.translationX
            val end = if (on) travel.toFloat() else 0f
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 180
                interpolator = OvershootInterpolator(1.2f)
                addUpdateListener {
                    thumb.translationX = start + (end - start) * (it.animatedValue as Float)
                }
                start()
            }
        }

        track.setOnClickListener {
            checked = !checked
            animateTo(checked)
        }
        // 初始状态：开
        thumb.post { thumb.translationX = travel.toFloat() }
        return track
    }

    /** 圆形 FAB。 */
    private fun glassFab(context: Context, source: ViewGroup): LiquidGlassView {
        return LiquidGlassView(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dpi(64), context.dpi(64))
            setCornerRadius(context.dpf(32f))
            setRefractionHeight(context.dpf(30f))
            setRefractionOffset(context.dpf(70f))
            setDispersion(0.55f)
            setBlurRadius(7f)
            setTintAlpha(0.20f)
            setTintColorRed(0f)
            setTintColorGreen(0f)
            setTintColorBlue(0f)
            setTouchEffectEnabled(true)
            val holder = FrameLayout(context)
            holder.addView(
                IconView(context, IconView.Glyph.REFRESH),
                FrameLayout.LayoutParams(context.dpi(26), context.dpi(26), Gravity.CENTER)
            )
            addView(holder, matchLP())
            bind(source)
        }
    }

    private fun wrapLP() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { gravity = Gravity.CENTER_HORIZONTAL }
}
