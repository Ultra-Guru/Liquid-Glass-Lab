package com.ultraguru.liquidglass.plugin.kyant0.scenes

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.ui.unit.IntOffset
import kotlin.math.cos
import kotlin.math.sin

/**
 * 场景 4：生物拟态。
 * 有机极坐标「细胞」形状（r = R(1 + a·sin(kθ + φ))）+ 呼吸缩放 +
 * Ambient 高光 + 色散折射 —— 玻璃化的生物群落。
 */
@Composable
fun BiomorphicScene() {
    val infinite = rememberInfiniteTransition(label = "breath")
    val breath by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )

    DemoBackdropLayer { _ ->
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("玻璃细胞群", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("有机形态 · 呼吸缩放 · Ambient 高光 · 色散折射", color = Color(0x99FFFFFF), fontSize = 12.sp)

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                // 细胞群：围绕中心呼吸
                val cells = listOf(
                    CellSpec(index = 0, sizeDp = 150, angleDeg = 0f, distDp = 0f, lobes = 5, wobble = 0.18f, phase = 0f),
                    CellSpec(index = 1, sizeDp = 92, angleDeg = 40f, distDp = 170, lobes = 6, wobble = 0.24f, phase = 1.1f),
                    CellSpec(index = 2, sizeDp = 74, angleDeg = 150f, distDp = 150, lobes = 4, wobble = 0.30f, phase = 2.3f),
                    CellSpec(index = 3, sizeDp = 104, angleDeg = 240f, distDp = 185, lobes = 7, wobble = 0.16f, phase = 3.6f),
                    CellSpec(index = 4, sizeDp = 60, angleDeg = 310f, distDp = 140, lobes = 5, wobble = 0.26f, phase = 4.7f),
                    CellSpec(index = 5, sizeDp = 48, angleDeg = 95f, distDp = 225, lobes = 3, wobble = 0.34f, phase = 5.5f)
                )
                cells.forEach { cell ->
                    val wob = sin(breath * 2f * Math.PI.toFloat() + cell.phase)
                    val scale = 1f + 0.10f * wob
                    val shapePhase = breath * 2f + cell.phase
                    val dist = cell.distDp * (1f + 0.06f * wob)

                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .offset {
                                IntOffset(
                                    x = (cos(Math.toRadians(cell.angleDeg.toDouble())) * dist.value).toInt(),
                                    y = (sin(Math.toRadians(cell.angleDeg.toDouble())) * dist.value).toInt()
                                )
                            }
                            .size(cell.sizeDp.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .drawBackdrop(
                                backdrop = LocalBackdrop.current,
                                shape = { BlobShape(lobes = cell.lobes, wobble = cell.wobble, phase = shapePhase) },
                                effects = {
                                    vibrancy()
                                    blur(1.dp.toPx())
                                    lens(
                                        refractionHeight = 18.dp.toPx(),
                                        refractionAmount = 36.dp.toPx(),
                                        chromaticAberration = true
                                    )
                                },
                                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.09f)) }
                            )
                    )
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

private data class CellSpec(
    val index: Int,
    val sizeDp: Int,
    val angleDeg: Float,
    val distDp: Int,
    val lobes: Int,
    val wobble: Float,
    val phase: Float
)


/** 有机「细胞」形状：极坐标花瓣闭合平滑曲线。 */
private class BlobShape(
    private val lobes: Int,
    private val wobble: Float,
    private val phase: Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        androidx.compose.ui.graphics.Outline.Generic(buildPath(size))

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
