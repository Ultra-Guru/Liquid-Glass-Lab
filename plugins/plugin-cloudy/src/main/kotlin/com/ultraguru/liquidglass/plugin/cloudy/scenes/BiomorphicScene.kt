package com.ultraguru.liquidglass.plugin.cloudy.scenes

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.skydoves.cloudy.ExperimentalLiquidGlassMotion
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.rememberGyroLightSource
import kotlin.math.cos
import kotlin.math.sin

/**
 * 场景 4（Cloudy）：生物拟态。
 *
 * 有机极坐标「细胞」形状（r = R·(1 + a·sin(kθ + t))）+ 呼吸缩放 +
 * `cloudy(sky, shape = BlobShape)` 把磨砂玻璃裁剪成有机形态 ——
 * 叠加 `light = rememberGyroLightSource()` 后，倾斜设备时每个细胞上
 * 有一枚动态镜面高光在游动（Cloudy 特有的 focal-pool 高光）。
 */
@OptIn(ExperimentalLiquidGlassMotion::class)
@Composable
fun BiomorphicScene() {
    val infinite = rememberInfiniteTransition(label = "breath")
    val breath by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "breath"
    )
    // 动态光源：细胞表面的高光随设备倾斜游动
    val gyroLight = rememberGyroLightSource()

    DemoSkyLayer {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("玻璃细胞群", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("有机形态 · 渐进磨砂 · 陀螺仪动态高光", color = Color(0x99FFFFFF), fontSize = 12.sp)

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                // 细胞群：围绕中心呼吸
                val cells = listOf(
                    CellSpec(sizeDp = 150, angleDeg = 0f, distDp = 0f, lobes = 5, wobble = 0.18f, phase = 0f),
                    CellSpec(sizeDp = 92, angleDeg = 40f, distDp = 170, lobes = 6, wobble = 0.24f, phase = 1.1f),
                    CellSpec(sizeDp = 74, angleDeg = 150f, distDp = 150, lobes = 4, wobble = 0.30f, phase = 2.3f),
                    CellSpec(sizeDp = 104, angleDeg = 240f, distDp = 185, lobes = 7, wobble = 0.16f, phase = 3.6f),
                    CellSpec(sizeDp = 60, angleDeg = 310f, distDp = 140, lobes = 5, wobble = 0.26f, phase = 4.7f),
                    CellSpec(sizeDp = 48, angleDeg = 95f, distDp = 225, lobes = 3, wobble = 0.34f, phase = 5.5f)
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
                            .cloudy(
                                sky = LocalSky.current,
                                radius = 14,
                                tint = Color.White.copy(alpha = 0.09f),
                                light = gyroLight,
                                shape = BlobShape(
                                    lobes = cell.lobes,
                                    wobble = cell.wobble,
                                    phase = shapePhase
                                )
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
    val sizeDp: Int,
    val angleDeg: Float,
    val distDp: Int,
    val lobes: Int,
    val wobble: Float,
    val phase: Float
)

/** 有机「细胞」形状：极坐标花瓣闭合平滑曲线（Catmull-Rom 平滑）。 */
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
