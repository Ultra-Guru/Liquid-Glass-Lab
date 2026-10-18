package com.ultraguru.liquidglass.plugin.qwea0.scenes

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.liquidglass.GlassMaterial
import com.example.liquidglass.LiquidGlassButton
import com.example.liquidglass.LiquidGlassChip
import com.example.liquidglass.LiquidGlassChipGroup
import com.example.liquidglass.LiquidGlassFab
import com.example.liquidglass.LiquidGlassView

/**
 * 场景 2：按钮 UI（qwea0）。
 *
 * 库的现成组件矩阵：LiquidGlassButton（胶囊）、LiquidGlassFab（圆形图标）、
 * LiquidGlassChip / ChipGroup（可勾选 / 可关闭）。所有玻璃件内建 iOS 26
 * 按压液态形变（enablePressEffect + pressScale + 拖拽弹性 elasticity），
 * 不需要外部再包一层动画。
 *
 * 集成点：按钮位于 ScrollView 的 LinearLayout 里——玻璃默认捕获的
 * 「直接父容器」在这里不绘制任何内容，必须逐个 backdropSource 指向
 * 演示背景（透明容器的标准用法；ChipGroup 同理由组统一下发）。
 */
@Composable
internal fun ButtonsScene() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> buildButtonsScene(ctx) }
    )
}

internal fun buildButtonsScene(context: Context): View {
    fun dp(v: Int): Int = context.dp(v)

    val root = FrameLayout(context)

    // ── 演示背景（玻璃的 backdropSource 目标） ──
    val backdrop = PaintedBackdropView(context)
    root.addView(backdrop, FrameLayout.LayoutParams(MATCH, MATCH))

    /** 统一玻璃管线配置：动态背景 + 跨层级背景来源。 */
    fun LiquidGlassView.plumb() {
        enableDynamicBackground = true
        backdropSource = backdrop
    }

    val list = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(24), dp(8), dp(24), dp(32))
    }

    // 标题
    list.addView(
        sceneTitle(context, "玻璃按钮矩阵"),
        LinearLayout.LayoutParams(WRAP, WRAP).apply { bottomMargin = dp(4) }
    )
    list.addView(
        sceneHint(context, "LiquidGlassButton / Fab / Chip · 内建按压液态形变").apply {
            textSize = 12f
        },
        LinearLayout.LayoutParams(WRAP, WRAP).apply { bottomMargin = dp(26) }
    )

    // ── 主按钮（Regular 材质） ──
    list.addView(
        LiquidGlassButton(context).apply {
            plumb()
            text = "主按钮 · Liquid"
            setOnClickListener { }
        },
        LinearLayout.LayoutParams(dp(232), WRAP).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(14)
        }
    )

    // ── 次要行：Clear 材质 / 蓝色染色 / 暗色染色 ──
    val secondary = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_HORIZONTAL
    }
    secondary.addView(
        LiquidGlassButton(context).apply {
            plumb()
            material = GlassMaterial.CLEAR
            text = "清透"
        },
        rowParams(context)
    )
    secondary.addView(
        LiquidGlassButton(context).apply {
            plumb()
            setGlassTint(0xFF0A84FF.toInt(), 0.35f)
            text = "蓝色"
        },
        rowParams(context)
    )
    secondary.addView(
        LiquidGlassButton(context).apply {
            plumb()
            setGlassTint(0xFF1B1B1F.toInt(), 0.45f)
            text = "暗色"
        },
        rowParams(context)
    )
    list.addView(secondary, LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(26) })

    // ── 圆形图标按钮（LiquidGlassFab） ──
    list.addView(
        sceneHint(context, "图标按钮（LiquidGlassFab）").apply { textSize = 12f },
        LinearLayout.LayoutParams(WRAP, WRAP).apply { bottomMargin = dp(12) }
    )
    val icons = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_HORIZONTAL
    }
    icons.addView(
        LiquidGlassFab(context).apply {
            plumb()
            icon = CodeIcons.heart().asIcon()
            setOnClickListener { }
        },
        fabParams(context)
    )
    icons.addView(
        LiquidGlassFab(context).apply {
            plumb()
            icon = CodeIcons.spark().asIcon()
            setOnClickListener { }
        },
        fabParams(context)
    )
    icons.addView(
        LiquidGlassFab(context).apply {
            plumb()
            icon = CodeIcons.person().asIcon()
            setOnClickListener { }
        },
        fabParams(context)
    )
    list.addView(icons, LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(26) })

    // ── Chip 组（操作 / 可勾选筛选 / 可关闭输入） ──
    list.addView(
        sceneHint(context, "玻璃 Chip（可勾选 / 可关闭）").apply { textSize = 12f },
        LinearLayout.LayoutParams(WRAP, WRAP).apply { bottomMargin = dp(12) }
    )
    val chipGroup = LiquidGlassChipGroup(context).apply {
        // 组是透明容器：背景来源与动态背景由组统一下发到每个 chip
        backdropSource = backdrop
        enableDynamicBackground = true
    }
    chipGroup.addView(
        LiquidGlassChip(context).apply {
            text = "探索"
            chipIcon = CodeIcons.search().asIcon()
            setOnClickListener { }
        }
    )
    listOf("照片" to false, "视频" to false, "音乐" to true).forEach { (label, checked) ->
        chipGroup.addView(
            LiquidGlassChip(context).apply {
                text = label
                isCheckable = true
                isChecked = checked
            }
        )
    }
    chipGroup.addView(
        LiquidGlassChip(context).apply {
            text = "巴黎"
            isCloseIconVisible = true
            setOnCloseIconClickListener { chipGroup.removeView(this) }
        }
    )
    list.addView(
        chipGroup,
        LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(28) }
    )

    // ── 大号 FAB ──
    list.addView(
        LiquidGlassFab(context).apply {
            plumb()
            icon = CodeIcons.plus().asIcon()
            setOnClickListener { }
        },
        LinearLayout.LayoutParams(dp(64), dp(64)).apply { gravity = Gravity.CENTER_HORIZONTAL }
    )

    root.addView(
        ScrollView(context).apply { isVerticalScrollBarEnabled = false }.apply {
            addView(list, LinearLayout.LayoutParams(MATCH, WRAP))
        },
        FrameLayout.LayoutParams(MATCH, MATCH)
    )
    return root
}

/** 行内按钮参数（水平 margin）。 */
private fun rowParams(context: Context): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(WRAP, WRAP).apply {
        leftMargin = context.dp(5)
        rightMargin = context.dp(5)
    }

/** 图标按钮（LiquidGlassFab）行内参数：固定 56dp 圆形 + 间距。 */
private fun fabParams(context: Context): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(context.dp(56), context.dp(56)).apply {
        leftMargin = context.dp(10)
        rightMargin = context.dp(10)
    }
