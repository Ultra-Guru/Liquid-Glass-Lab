package com.ultraguru.liquidglass.plugin.kyant0.scenes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.sin

/** 子组件直接拿当前源 Backdrop（由 [DemoBackdropLayer] 提供）。 */
val LocalBackdrop = compositionLocalOf<Backdrop> { error("DemoBackdropLayer 未提供 Backdrop") }

/**
 * 四场景共用「演示背景」：深色渐变 + 高对比彩色光斑/光丝/微光点缓慢漂移 +
 * 暗角。光斑与亮线让折射/色散肉眼可见 —— 玻璃面放在上层做 drawBackdrop。
 *
 * 背景挂 [com.kyant.backdrop.backdrops.layerBackdrop] 源标记，然后把 [Backdrop]
 * 交给上层内容做玻璃面；同时通过 [LocalBackdrop] 提供给子组件。
 */
@Composable
fun DemoBackdropLayer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(backdrop: Backdrop) -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier.fillMaxSize()) {
        // 背景源：挂 layerBackdrop → 玻璃面折射的是这里画的内容
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            RichGradient()
        }
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            content(backdrop)
        }
    }
}

@Composable
private fun RichGradient() {
    val transition = rememberInfiniteTransition(label = "drift")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "driftT"
    )

    Canvas(Modifier.fillMaxSize().background(Color(0xFF0B0405))) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)
        val cycle = t * 6.28318f

        // 1) 深色基底：近黑红 → 纯黑 → 深酒红
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1C070A), Color(0xFF0B0405), Color(0xFF100308))
            )
        )

        // 2) 漂移彩色光斑（正弦相位）—— 7 个，冷暖交错、饱和度拉高
        val blobs = listOf(
            BlobSpec(cx = 0.20f + 0.05f * sin(cycle), cy = 0.16f, r = 0.42f, color = Color(0xFFE0524A), alpha = 0.80f),
            BlobSpec(cx = 0.82f, cy = 0.26f + 0.06f * sin(cycle + 1.2f), r = 0.34f, color = Color(0xFFFFB25E), alpha = 0.62f),
            BlobSpec(cx = 0.64f, cy = 0.80f + 0.03f * sin(cycle + 2.0f), r = 0.44f, color = Color(0xFF2EC4B6), alpha = 0.58f),
            BlobSpec(cx = 0.12f + 0.03f * sin(cycle + 2.4f), cy = 0.74f, r = 0.32f, color = Color(0xFF7C5CBF), alpha = 0.55f),
            BlobSpec(cx = 0.45f, cy = 0.48f + 0.05f * sin(cycle + 0.7f), r = 0.28f, color = Color(0xFF3E7CB1), alpha = 0.42f),
            BlobSpec(cx = 0.90f, cy = 0.68f + 0.04f * sin(cycle + 0.6f), r = 0.30f, color = Color(0xFFFF4FA3), alpha = 0.50f),
            BlobSpec(cx = 0.06f, cy = 0.42f + 0.05f * sin(cycle + 3.1f), r = 0.24f, color = Color(0xFF53E8FF), alpha = 0.38f)
        )
        blobs.forEach { b ->
            val center = Offset(w * b.cx, h * b.cy)
            val radius = minDim * b.r
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(b.color.copy(alpha = b.alpha), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        // 3) 细亮「光丝」—— 玻璃折射/色散的高对比来源（拖动玻璃时能看见光线被掰弯）
        val streaks = listOf(
            StreakSpec(0.04f, 0.24f, 0.72f, 0.10f, 2.5f, Color(0xFFFFF4E0), 0.85f, 0.0f),
            StreakSpec(0.30f, 0.94f, 0.98f, 0.78f, 2.0f, Color(0xFFFFD9A8), 0.60f, 2.1f),
            StreakSpec(0.55f, 0.36f, 0.95f, 0.52f, 1.5f, Color(0xFF9FE8FF), 0.50f, 4.2f)
        )
        streaks.forEach { s ->
            val drift = 0.02f * sin(cycle + s.phase)
            val p1 = Offset(w * (s.x1 + drift), h * s.y1)
            val p2 = Offset(w * s.x2, h * (s.y2 - drift))
            val twinkle = 0.55f + 0.45f * sin(cycle * 1.5f + s.phase)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        s.color.copy(alpha = s.peak * twinkle),
                        Color.Transparent
                    ),
                    start = p1,
                    end = p2
                ),
                start = p1,
                end = p2,
                strokeWidth = s.width.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 4) 微光点 —— 尺寸不一、缓慢漂移 + 闪烁，路过玻璃边缘时产生星芒般的折射
        val dots = listOf(
            0.16f to 0.34f,
            0.36f to 0.12f,
            0.58f to 0.66f,
            0.76f to 0.44f,
            0.48f to 0.86f,
            0.88f to 0.10f
        )
        dots.forEachIndexed { i, (dx, dy) ->
            val drift = 0.012f * sin(cycle + i * 1.7f)
            val center = Offset(w * (dx + drift), h * (dy - drift))
            val radius = (1.6f + (i % 3) * 0.7f).dp.toPx() * 3f
            val a = (0.30f + 0.30f * sin(cycle * 2f + i * 2.3f)).coerceIn(0.06f, 0.65f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF6E8).copy(alpha = a), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        // 5) 暗角 —— 四周收拢视线，中间留给玻璃主舞台
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.52f)),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = minDim * 0.92f
            )
        )
    }
}

private data class BlobSpec(
    val cx: Float,
    val cy: Float,
    val r: Float,
    val color: Color,
    val alpha: Float
)

private data class StreakSpec(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val width: Float,
    val color: Color,
    val peak: Float,
    val phase: Float
)
