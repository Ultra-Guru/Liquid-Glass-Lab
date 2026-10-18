package com.ultraguru.liquidglass.plugin.cloudy.scenes

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
import com.skydoves.cloudy.Sky
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
import kotlin.math.sin

/**
 * 四场景共享的 [Sky] —— 由 [DemoSkyLayer] 提供，玻璃面直接
 * `Modifier.cloudy(sky = LocalSky.current, ...)` 采样背景。
 */
val LocalSky = compositionLocalOf<Sky> { error("DemoSkyLayer 未提供 Sky") }

/**
 * Cloudy 版「演示背景 + 取样层」。
 *
 * 与 kyant0 插件的 DemoBackdropLayer 同一视觉大纲：深色渐变 + 彩色光斑缓慢漂移；
 * 差别在于背景挂的是 `Modifier.sky(sky)`（Cloudy 的背景模糊取样器）。
 *
 * [source] 的内容会被整层录制进 GraphicsLayer（含滚动内容），
 * [overlay] 里的玻璃面用 `cloudy(sky, ...)` 对它做渐进模糊 / 磨砂。
 */
@Composable
fun DemoSkyLayer(
    modifier: Modifier = Modifier,
    source: @Composable BoxScope.() -> Unit = { RichGradient() },
    overlay: @Composable BoxScope.(sky: Sky) -> Unit
) {
    val sky = rememberSky()
    CompositionLocalProvider(LocalSky provides sky) {
        Box(modifier.fillMaxSize()) {
            // 背景源：挂 sky → 玻璃面模糊/折射的是这一层录到的内容
            Box(
                Modifier
                    .fillMaxSize()
                    .sky(sky)
            ) {
                source()
            }
            overlay(sky)
        }
    }
}

/**
 * 深色渐变 + 漂移彩色光斑（与宿主品牌一致的深色底）。
 * 光斑让渐进模糊 / 折射肉眼可见。
 */
@Composable
fun RichGradient(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "drift")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(24_000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "driftT"
    )

    Canvas(modifier.fillMaxSize().background(Color(0xFF0B0405))) {
        val w = size.width
        val h = size.height
        // 深色底：近黑红 → 纯黑
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1A0708), Color(0xFF0B0405), Color(0xFF120306))
            )
        )
        // 漂移彩色光斑（正弦相位）
        val blobs = listOf(
            BlobSpec(cx = 0.22f + 0.05f * sin(t * 6.28f), cy = 0.18f, r = 0.42f, color = Color(0xFFE0524A), alpha = 0.75f),
            BlobSpec(cx = 0.80f, cy = 0.30f + 0.06f * sin(t * 6.28f + 1.2f), r = 0.38f, color = Color(0xFFFFB25E), alpha = 0.55f),
            BlobSpec(cx = 0.62f, cy = 0.78f, r = 0.44f, color = Color(0xFF2EC4B6), alpha = 0.5f),
            BlobSpec(cx = 0.14f, cy = 0.72f, r = 0.30f + 0.03f * sin(t * 6.28f + 2.4f), color = Color(0xFF7C5CBF), alpha = 0.55f),
            BlobSpec(cx = 0.45f, cy = 0.48f, r = 0.30f, color = Color(0xFF3E7CB1), alpha = 0.35f)
        )
        blobs.forEach { b ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(b.color.copy(alpha = b.alpha), Color.Transparent),
                    center = Offset(w * b.cx, h * b.cy),
                    radius = (minOf(w, h) * b.r)
                ),
                radius = minOf(w, h) * b.r,
                center = Offset(w * b.cx, h * b.cy)
            )
        }
    }
}

private data class BlobSpec(
    val cx: Float,
    val cy: Float,
    val r: Float,
    val color: Color,
    val alpha: Float
)
