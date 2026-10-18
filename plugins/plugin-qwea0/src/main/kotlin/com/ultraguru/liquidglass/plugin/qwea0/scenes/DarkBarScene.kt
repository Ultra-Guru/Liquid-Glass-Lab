package com.ultraguru.liquidglass.plugin.qwea0.scenes

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.liquidglass.LiquidGlassTabBar
import com.example.liquidglass.LiquidGlassView

/**
 * 场景 1：深色栏 UI（qwea0 / LiquidGlassView）。
 *
 * 顶部玻璃栏 + 底部 LiquidGlassTabBar（iOS 26 风格、玻璃滴指示器可拖拽），
 * 滚动内容从两道玻璃后流过。玻璃捕获直接父容器（root），因此背景、
 * 滚动列表与玻璃条必须是同一 FrameLayout 的兄弟节点，且全部开
 * enableDynamicBackground——这是本库最关键的集成点（默认 false，不开则
 * 玻璃背景被冻结）。
 */
@Composable
internal fun DarkBarScene() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> buildDarkBarScene(ctx) }
    )
}

internal fun buildDarkBarScene(context: Context): View {
    fun dp(v: Int): Int = context.dp(v)
    fun dpF(v: Float): Float = context.dpF(v)

    val root = FrameLayout(context)

    // ── 1. 背景（玻璃捕获内容的一部分） ──
    root.addView(PaintedBackdropView(context), FrameLayout.LayoutParams(MATCH, MATCH))

    // ── 2. 滚动内容：从玻璃后流过 ──
    val list = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(92), dp(20), dp(150))
    }
    list.addView(
        sceneHint(context, "内容在玻璃下方滚动\n注意顶栏与底部标签条的折射实时变化").apply {
            gravity = Gravity.START
        },
        LinearLayout.LayoutParams(WRAP, WRAP).apply { bottomMargin = dp(12) }
    )
    repeat(16) { i -> list.addView(contentCard(context, i)) }

    root.addView(
        ScrollView(context).apply { isVerticalScrollBarEnabled = false }.apply {
            addView(list, LinearLayout.LayoutParams(MATCH, WRAP))
        },
        FrameLayout.LayoutParams(MATCH, MATCH)
    )

    // ── 3. 顶部玻璃栏 ──
    val title = TextView(context).apply {
        text = "Liquid Glass"
        textSize = 17f
        setTextColor(Color.WHITE)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setShadowLayer(8f, 0f, 2f, 0x66000000)
    }
    val battery = TextView(context).apply {
        text = "33%"
        textSize = 12f
        setTextColor(0xB3FFFFFF.toInt())
    }
    val starIcon = iconView(context, CodeIcons.star()).apply { setColorFilter(0xFFFFC24B.toInt()) }
    val searchIcon = iconView(context, CodeIcons.search())

    val topBar = LiquidGlassView(context).apply {
        enableDynamicBackground = true
        enableAdaptiveTint = true
        cornerRadius = dpF(28f)
        bevelWidth = dpF(14f)
        refractionHeight = dpF(7f)
        dispersionStrength = 0.10f
        glassAppearanceListener = { overLight ->
            title.setTextColor(if (overLight) 0xDE1B1B1B.toInt() else Color.WHITE)
            battery.setTextColor(if (overLight) 0x99000000.toInt() else 0xB3FFFFFF.toInt())
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
        }
        row.addView(starIcon, LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(10) })
        row.addView(title)
        row.addView(View(context), LinearLayout.LayoutParams(0, 1).apply { weight = 1f })
        row.addView(battery, LinearLayout.LayoutParams(WRAP, WRAP).apply { rightMargin = dp(14) })
        row.addView(searchIcon, LinearLayout.LayoutParams(dp(20), dp(20)))
        addView(row, FrameLayout.LayoutParams(MATCH, MATCH))
    }
    root.addView(
        topBar,
        FrameLayout.LayoutParams(MATCH, dp(64), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            .apply { setMargins(dp(16), dp(10), dp(16), 0) }
    )

    // ── 4. 底部玻璃标签条（玻璃滴指示器自带折射 + 可拖拽吸附） ──
    val tabBar = LiquidGlassTabBar(context).apply {
        enableDynamicBackground = true
        enableAdaptiveTint = true
        setTabs(
            listOf(
                LiquidGlassTabBar.TabItem("首页", CodeIcons.home().asIcon()),
                LiquidGlassTabBar.TabItem("发现", CodeIcons.search().asIcon()),
                LiquidGlassTabBar.TabItem("收藏", CodeIcons.star().asIcon()),
                LiquidGlassTabBar.TabItem("我的", CodeIcons.person().asIcon())
            )
        )
        selectedTintColor = 0xFFFFC24B.toInt()
    }
    root.addView(
        tabBar,
        FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            .apply { setMargins(dp(20), 0, dp(20), dp(14)) }
    )
    liftAboveSystemBars(root, basePx = dp(14), tabBar)

    return root
}

/** 滚动内容卡片：彩色渐变 + 文字（高频内容，折射明显）。 */
private fun contentCard(context: Context, i: Int): View {
    fun dp(v: Int): Int = context.dp(v)
    fun dpF(v: Float): Float = context.dpF(v)

    val hue = (i * 37f) % 360f
    val c1 = Color.HSVToColor(floatArrayOf(hue, 0.42f, 0.30f))
    val c2 = Color.HSVToColor(floatArrayOf((hue + 28f) % 360f, 0.48f, 0.42f))
    return FrameLayout(context).apply {
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2)).apply {
            cornerRadius = dpF(18f)
        }
        addView(
            TextView(context).apply {
                text = "滚动卡片 #$i"
                textSize = 15f
                setTextColor(0xE6FFFFFF.toInt())
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                setPadding(dp(16), 0, dp(16), 0)
            },
            FrameLayout.LayoutParams(MATCH, MATCH)
        )
        layoutParams = LinearLayout.LayoutParams(MATCH, dp(72)).apply { bottomMargin = dp(12) }
    }
}

/** 统一构造的图标 ImageView（白描图标）。 */
private fun iconView(context: Context, path: Path): ImageView =
    ImageView(context).apply { setImageDrawable(path.asIcon()) }
