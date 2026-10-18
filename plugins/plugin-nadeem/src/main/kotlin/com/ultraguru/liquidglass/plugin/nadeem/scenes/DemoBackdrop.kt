package com.ultraguru.liquidglass.plugin.nadeem.scenes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.nadeemiqbal.liquidglass.LiquidGlassState
import io.github.nadeemiqbal.liquidglass.liquidGlassSource
import io.github.nadeemiqbal.liquidglass.rememberLiquidGlassState
import kotlin.math.cos
import kotlin.math.sin

/**
 * nadeem 插件四场景共享的「演示背景」。
 *
 * 与库的采样模型对齐：liquidGlassSource(state) 挂在背景层（必须是
 * 修饰链中最先应用的绘制前缀——drawWithContent 包住 background /
 * drawBehind 的内容才会被录进 GraphicsLayer），上层的 liquidGlass /
 * Glass* 组件用同一 state 采样这块层。
 *
 * 深色系：近黑渐变 + 漂移彩色光斑 + 细网格（高频结构让 refraction 的
 * 正弦位移肉眼可辨——平滑渐变上折射不可见）。
 */

/** 演示场景统一玻璃 tint：18% 白（深色背景上读作清透玻璃）。 */
internal val DemoGlassTint = Color(0x2EFFFFFF)

/** 深色基底渐变（近黑红 → 纯黑 → 深紫黑）。 */
internal val BackdropBaseGradient: Brush = Brush.verticalGradient(
    colors = listOf(Color(0xFF1A0608), Color(0xFF0A0304), Color(0xFF120309))
)

private class BlobSpec(
    val cx: Float,
    val cy: Float,
    val r: Float,
    val color: Color,
    val phaseOff: Float,
    val drift: Float
)

private val Blobs = listOf(
    BlobSpec(0.22f, 0.16f, 0.42f, Color(0xFFE0524A), 0.0f, 1.2f),
    BlobSpec(0.80f, 0.28f, 0.38f, Color(0xFFFFB25E), 1.2f, 0.9f),
    BlobSpec(0.62f, 0.82f, 0.46f, Color(0xFF2EC4B6), 2.4f, 1.4f),
    BlobSpec(0.12f, 0.74f, 0.32f, Color(0xFF7C5CBF), 3.6f, 0.8f),
    BlobSpec(0.44f, 0.46f, 0.34f, Color(0xFF3E7CB1), 4.7f, 1.0f)
)

/** 漂移相位（四场景共用同一节奏，视觉上互相连贯）。 */
@Composable
internal fun rememberBackdropPhase(): Float {
    val transition = rememberInfiniteTransition(label = "backdrop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "backdropPhase"
    )
    return phase
}

/** 深色渐变 + 网格 + 光斑（绘制层，挂在 source 内部）。 */
@Composable
internal fun DarkBackdropCanvas(modifier: Modifier = Modifier) {
    val phase = rememberBackdropPhase()
    Box(
        modifier
            .background(BackdropBaseGradient)
            .drawBehind {
                val w = size.width
                val h = size.height
                val minDim = minOf(w, h)

                // 细网格：高频结构 → 折射位移肉眼可辨
                val step = 32.dp.toPx()
                val lineColor = Color.White.copy(alpha = 0.055f)
                var gx = 0f
                while (gx <= w) {
                    drawLine(lineColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                    gx += step
                }
                var gy = 0f
                while (gy <= h) {
                    drawLine(lineColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                    gy += step
                }

                // 彩色光斑（正弦漂移，相位各异）
                Blobs.forEach { b ->
                    val dx = sin(phase + b.phaseOff) * b.drift * 0.05f
                    val dy = sin(phase * 0.8f + b.phaseOff * 1.7f) * b.drift * 0.04f
                    val center = Offset(w * (b.cx + dx), h * (b.cy + dy))
                    val radius = minDim * b.r
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(b.color.copy(alpha = 0.65f), Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }
            }
    )
}

/**
 * 四场景共用骨架：背景层挂 [liquidGlassSource]（内容 + 渐变一起被录制），
 * 玻璃组件在 [content] 里用回传的 [LiquidGlassState] 采样。
 */
@Composable
internal fun DemoBackdropLayer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(state: LiquidGlassState) -> Unit
) {
    val state = rememberLiquidGlassState()
    Box(modifier.fillMaxSize()) {
        DarkBackdropCanvas(
            Modifier
                .fillMaxSize()
                .liquidGlassSource(state)
        )
        content(state)
    }
}
