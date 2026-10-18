package com.ultraguru.liquidglass.plugin.nadeem.scenes

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nadeemiqbal.liquidglass.GlassCard
import io.github.nadeemiqbal.liquidglass.LiquidGlassState
import io.github.nadeemiqbal.liquidglass.rememberDynamicSheen
import kotlin.math.cos
import kotlin.math.sin

/**
 * 场景 4：生物拟态（nadeem）。
 *
 * GlassCell 群落 = GlassCard + 极坐标有机形状（r = R·(1 + a·sin(kθ + t))，
 * 形状相位随时间流动）+ 呼吸缩放；refraction(0.55) + grain(0.05) 提供
 * 液态质感，rememberDynamicSheen 让每块玻璃的边缘光随背景采样色
 * 「呼吸」变化（库的招牌动态效果）。
 */
@Composable
internal fun BiomorphicScene() {
    DemoBackdropLayer { state ->
        val sheen = rememberDynamicSheen(state)

        val breath = rememberInfiniteTransition(label = "breath")
        val t by breath.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathT"
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("玻璃细胞群", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "有机极坐标形状 · 呼吸缩放 · DynamicSheen 边缘光",
                color = Color(0x99FFFFFF),
                fontSize = 12.sp
            )

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                val cells = listOf(
                    CellSpec(0, 150, 0f, 0, 5, 0.18f, 0f),
                    CellSpec(1, 92, 40f, 170, 6, 0.24f, 1.1f),
                    CellSpec(2, 74, 150f, 150, 4, 0.30f, 2.3f),
                    CellSpec(3, 104, 240f, 185, 7, 0.16f, 3.6f),
                    CellSpec(4, 60, 310f, 140, 5, 0.26f, 4.7f),
                    CellSpec(5, 48, 95f, 225, 3, 0.34f, 5.5f)
                )
                cells.forEach { cell ->
                    GlassCell(state, sheen, cell, t)
                }
            }

            Text(
                "每个形状由极坐标函数实时生成：r = R·(1 + a·sin(kθ + t))",
                color = Color(0x80FFFFFF),
                fontSize = 11.sp
            )
        }
    }
}

/** 一块「细胞」：GlassCard + 有机形状 + 呼吸缩放 + 边缘光。 */
@Composable
private fun BoxScope.GlassCell(
    state: LiquidGlassState,
    sheen: Brush,
    cell: CellSpec,
    t: Float
) {
    val wob = sin(t * 2f * Math.PI.toFloat() + cell.phase)
    val scale = 1f + 0.10f * wob
    val distDp = cell.distDp * (1f + 0.06f * wob)

    GlassCard(
        state = state,
        modifier = Modifier
            .align(Alignment.Center)
            .offset {
                val r = distDp.dp.toPx()
                IntOffset(
                    x = (cos(Math.toRadians(cell.angleDeg.toDouble())) * r).toInt(),
                    y = (sin(Math.toRadians(cell.angleDeg.toDouble())) * r).toInt()
                )
            }
            .size(cell.sizeDp.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = BlobShape(
            lobes = cell.lobes,
            wobble = cell.wobble,
            phase = t * 4f + cell.phase
        ),
        tint = Color(0x24FFFFFF),
        blurRadius = 14.dp,
        saturation = 1.6f,
        borderHighlight = sheen,
        grain = 0.05f,
        grainSeed = cell.index.toLong(),
        refraction = 0.55f,
        contentPadding = PaddingValues(0.dp)
    ) { }
}

private class CellSpec(
    val index: Int,
    val sizeDp: Int,
    val angleDeg: Float,
    val distDp: Int,
    val lobes: Int,
    val wobble: Float,
    val phase: Float
)

/** 有机「细胞」形状：极坐标花瓣 + Catmull-Rom 平滑闭合曲线。 */
private class BlobShape(
    private val lobes: Int,
    private val wobble: Float,
    private val phase: Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(buildPath(size))

    private fun buildPath(size: Size): Path {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = minOf(cx, cy)
        val steps = lobes * 12
        val pts = ArrayList<Offset>(steps)
        for (i in 0 until steps) {
            val theta = i.toFloat() / steps * 2f * Math.PI.toFloat()
            val r = baseR * (1f + wobble * sin(lobes * theta + phase))
            pts.add(Offset(cx + r * cos(theta), cy + r * sin(theta)))
        }
        return Path().apply {
            moveTo(pts[0].x, pts[0].y)
            // Catmull-Rom → 平滑闭合
            for (i in pts.indices) {
                val p0 = pts[(i - 1 + pts.size) % pts.size]
                val p1 = pts[i]
                val p2 = pts[(i + 1) % pts.size]
                val p3 = pts[(i + 2) % pts.size]
                val c1 = Offset(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f)
                val c2 = Offset(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f)
                cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
            }
            close()
        }
    }
}
