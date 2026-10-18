package com.ultraguru.liquidglass.plugin.qwea0.scenes

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
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
 * 场景 3：液体（qwea0）。
 *
 * 一颗大液滴（150dp，cornerRadius 999f = 正圆）可拖拽：OnTouchListener
 * 只改平移（v.x / v.y）并返回 false，把触摸继续交给玻璃自身的按压 /
 * 弹性拉伸逻辑（官方 HeroShowcase 的拖拽配方）。拖动时
 * enableDynamicBackground 让透镜逐帧重捕获，折射内容随手移动。
 *
 * 卫星小滴（Clear 材质）绕主滴公转、以插值追赶——拖得越快滞后越长，
 * 呈现液体的黏连感。主滴开 enableSensorHighlight（重力传感器视差高光）。
 */
@Composable
internal fun LiquidScene() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> buildLiquidScene(ctx) }
    )
}

internal fun buildLiquidScene(context: Context): View {
    fun dp(v: Int): Int = context.dp(v)
    fun dpF(v: Float): Float = context.dpF(v)

    val root = FrameLayout(context)
    root.addView(PaintedBackdropView(context), FrameLayout.LayoutParams(MATCH, MATCH))
    root.addView(
        sceneHint(context, "拖动大液滴 · 内建弹性拉伸 / 传感器高光 / 色散折射"),
        FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(18)
        }
    )

    // ── 主液滴 ──
    val grab = floatArrayOf(0f, 0f)
    val drop = LiquidGlassView(context).apply {
        enableDynamicBackground = true
        cornerRadius = 999f
        refractionHeight = 200f
        bevelWidth = 56f
        dispersionStrength = 0.16f
        saturation = 150f
        enableSensorHighlight = true
        // 拖拽：只移动自身，触摸仍交给玻璃的按压/弹性内部逻辑（返回 false）
        setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    grab[0] = v.x - e.rawX
                    grab[1] = v.y - e.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    v.x = e.rawX + grab[0]
                    v.y = e.rawY + grab[1]
                }
            }
            false
        }
    }
    root.addView(
        drop,
        FrameLayout.LayoutParams(dp(150), dp(150), Gravity.CENTER)
    )

    // ── 卫星小滴：绕主滴公转 + 插值追赶 ──
    val satellite = LiquidGlassView(context).apply {
        enableDynamicBackground = true
        cornerRadius = 999f
        refractionHeight = 140f
        bevelWidth = 40f
        dispersionStrength = 0.18f
        material = GlassMaterial.CLEAR
    }
    root.addView(
        satellite,
        FrameLayout.LayoutParams(dp(84), dp(84), Gravity.CENTER)
    )

    var angle = 0f
    val chase = object : Runnable {
        override fun run() {
            if (!drop.isAttachedToWindow || !satellite.isAttachedToWindow) return
            angle += 0.012f
            val targetX = drop.x + drop.width / 2f + cos(angle) * dpF(150f) - satellite.width / 2f
            val targetY = drop.y + drop.height / 2f + sin(angle) * dpF(92f) - satellite.height / 2f
            // 插值追赶：主滴被拖快时小滴被拉在后面，产生黏连滞后
            satellite.x += (targetX - satellite.x) * 0.10f
            satellite.y += (targetY - satellite.y) * 0.10f
            root.postOnAnimation(this)
        }
    }
    root.postOnAnimation(chase)

    return root
}
