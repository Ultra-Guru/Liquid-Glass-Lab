package com.ultraguru.liquidglass.plugin.qmdeve.scenes

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.qmdeve.liquidglass.widget.LiquidGlassView

/**
 * 场景 1（QMDeve）：深色栏 UI。
 *
 * - 采样源 = 背景光斑 + 滚动卡片（ScrollView 整个作为 bind 目标）；
 * - 顶部 / 底部两根 LiquidGlassView 玻璃栏 bind 同一源，
 *   滚动时每帧 PreDraw 重采样 —— 卡片真实地从玻璃后折射流过；
 * - 顶部栏向内折射（refractionOffset 负向收缩），底部导航同配方；
 * - 深色 tint（TINT RGB=0）保证「深色玻璃」观感。
 */
internal object DarkBarScene {

    fun create(context: Context): View {
        val root = BaseScene(context)

        // ── 1. 采样源：渐变光斑 + 滚动卡片 ──────────────────────────
        val source = FrameLayout(context)
        source.addView(GlassBackdropView(context), matchLP())

        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val list = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dpi(20), context.dpi(112), context.dpi(20), context.dpi(140))
        }
        list.addView(
            context.text("内容在玻璃下方滚动\n注意顶栏 / 底栏的折射随内容实时变化", 13f, 0xCCFFFFFF).apply {
                gravity = Gravity.START
                setLineSpacing(context.dpf(4f), 1f)
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = context.dpi(14) }
        )
        for (i in 0 until 16) {
            list.addView(contentCard(context, i), cardLP(context))
        }
        scroll.addView(
            list,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        source.addView(scroll, matchLP())
        root.addView(source, matchLP())

        // ── 2. 顶部玻璃栏 ──────────────────────────────────────────
        val topBar = LiquidGlassView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, context.dpi(64), Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply {
                topMargin = context.dpi(8)
                leftMargin = context.dpi(16)
                rightMargin = context.dpi(16)
            }
            glassConfig(context, cornerDp = 28f, dispersion = 0.4f, tintAlpha = 0.25f)
        }
        val titleRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dpi(18), 0, context.dpi(18), 0)
        }
        titleRow.addView(
            IconView(context, IconView.Glyph.STAR).apply { setIconColor(0xFFFFC24B.toInt()) },
            LinearLayout.LayoutParams(context.dpi(22), context.dpi(22)).apply { rightMargin = context.dpi(10) }
        )
        titleRow.addView(
            context.text("Liquid Glass", 17f, bold = true).apply { gravity = Gravity.START },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        titleRow.addView(
            context.text("33%", 12f, 0xB3FFFFFF).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        titleRow.addView(
            IconView(context, IconView.Glyph.SEARCH),
            LinearLayout.LayoutParams(context.dpi(20), context.dpi(20)).apply { leftMargin = context.dpi(14) }
        )
        topBar.addView(titleRow, matchLP())
        root.addView(topBar)
        topBar.bind(source)

        // ── 3. 底部玻璃导航 ────────────────────────────────────────
        val navBar = LiquidGlassView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, context.dpi(68), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                bottomMargin = context.dpi(10)
                leftMargin = context.dpi(28)
                rightMargin = context.dpi(28)
            }
            glassConfig(context, cornerDp = 30f, dispersion = 0.5f, tintAlpha = 0.22f)
        }
        val navRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val items = listOf(
            IconView.Glyph.HOME to "首页",
            IconView.Glyph.SEARCH to "发现",
            IconView.Glyph.STAR to "收藏",
            IconView.Glyph.PERSON to "我的"
        )
        var selected = 0
        val navItemViews = mutableListOf<Pair<IconView, android.widget.TextView>>()
        items.forEachIndexed { index, (glyph, label) ->
            val item = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(context.dpi(12), context.dpi(6), context.dpi(12), context.dpi(6))
            }
            val icon = IconView(context, glyph)
            val text = context.text(label, 10f)
            item.addView(icon, LinearLayout.LayoutParams(context.dpi(22), context.dpi(22)))
            item.addView(
                text,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = context.dpi(3) }
            )
            item.setOnClickListener {
                selected = index
                navItemViews.forEachIndexed { i, (ic, tx) ->
                    val tint = if (i == selected) Color.WHITE.toLong() else 0x80FFFFFF
                    ic.setIconColor(tint)
                    tx.setTextColor(tint.toInt())
                }
            }
            navItemViews += icon to text
            navRow.addView(
                item,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            )
        }
        navBar.addView(navRow, matchLP())
        root.addView(navBar)
        navBar.bind(source)

        // ── 4. insets：顶栏避开状态栏、导航栏避开手势条 ─────────────
        root.onInsets {
            topBar.layoutParams = (topBar.layoutParams as FrameLayout.LayoutParams).apply {
                topMargin = root.insetTop + context.dpi(8)
            }
            navBar.layoutParams = (navBar.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = root.insetBottom + context.dpi(10)
            }
            topBar.requestLayout()
            navBar.requestLayout()
        }

        return root
    }

    /** 玻璃栏统一配方：深色 tint + 中等折射。 */
    private fun LiquidGlassView.glassConfig(
        context: Context,
        cornerDp: Float,
        dispersion: Float,
        tintAlpha: Float
    ) {
        setCornerRadius(context.dpf(cornerDp))
        setRefractionHeight(context.dpf(30f))
        setRefractionOffset(context.dpf(70f))
        setDispersion(dispersion)
        setBlurRadius(10f)
        setTintAlpha(tintAlpha)
        // 深色玻璃：tint 黑
        setTintColorRed(0f)
        setTintColorGreen(0f)
        setTintColorBlue(0f)
    }

    private fun contentCard(context: Context, i: Int): View {
        val hue = (i * 37f) % 360f
        val base = Color.HSVToColor(floatArrayOf(hue, 0.42f, 0.30f))
        val bg = GradientDrawable().apply {
            cornerRadius = context.dpf(18f)
            setColor((0xD9 shl 24) or (base and 0xFFFFFF))
        }
        return android.widget.TextView(context).apply {
            text = "滚动卡片 #$i"
            textSize = 15f
            setTextColor(0xE6FFFFFF.toInt())
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            setPadding(context.dpi(16), 0, context.dpi(16), 0)
            background = bg
        }
    }

    private fun cardLP(context: Context) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpi(72))
            .apply { bottomMargin = context.dpi(12) }
}
