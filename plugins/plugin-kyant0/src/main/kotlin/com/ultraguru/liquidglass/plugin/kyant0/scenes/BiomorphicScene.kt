package com.ultraguru.liquidglass.plugin.kyant0.scenes

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.RoundedRectangle
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 场景 4：生物拟态。
 * 中心一颗连续曲率「方形晶胞」（squircle，可安全走 lens 折射 + 色散），
 * 外周是有机极坐标「细胞」（r = R(1 + a·sin(kθ + φ))）—— 呼吸缩放各自相位独立、
 * 微幅自转 + Ambient 高光 + 定向镜面描边 + 加法内辉光，细胞彼此轻微交叠。
 *
 * 注意：有机 BlobShape 是 Generic 轮廓，vendor 的 lens() 只支持圆角矩形族，
 * 硬配 lens 会抛 UnsupportedOperationException —— 因此外周细胞走 softGlass
 * （vibrancy + blur + 表面层），折射集中在中心晶胞。
 */
@Composable
fun BiomorphicScene() {
    val infinite = rememberInfiniteTransition(label = "breath")
    val breath by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
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
            Text("有机形态 · 呼吸缩放 · Ambient 高光 · 中心晶胞色散折射", color = Color(0x99FFFFFF), fontSize = 12.sp)

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                val cycle = breath * 2f * Math.PI.toFloat()
                val coreWob = sin(cycle)

                // ── 中心方形晶胞：squircle + lens 折射 + 色散 ──
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .offset {
                            IntOffset(
                                (sin(cycle) * 8f).roundToInt(),
                                (cos(cycle) * 6f).roundToInt()
                            )
                        }
                        .size(148.dp)
                        .graphicsLayer {
                            scaleX = 1f + 0.05f * coreWob
                            scaleY = 1f + 0.05f * coreWob
                            rotationZ = 2f * coreWob
                        }
                        .liquidGlass(
                            shape = { RoundedRectangle(62.dp) },
                            blurRadius = 1.5.dp,
                            refractionHeight = 18.dp,
                            refractionAmount = 38.dp,
                            chromaticAberration = true,
                            surface = {
                                roundedGlassSurface(62.dp, Color.White.copy(alpha = 0.07f), specularAlpha = 0.26f)
                                // 细胞核：中心加法辉光
                                drawRoundRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                                        center = Offset(size.width * 0.44f, size.height * 0.40f),
                                        radius = size.minDimension * 0.42f
                                    ),
                                    cornerRadius = CornerRadius(
                                        62.dp.toPx().coerceAtMost(size.minDimension * 0.5f)
                                    )
                                )
                                roundedGlassRim(62.dp, width = 1.5.dp, alpha = 0.45f)
                            }
                        )
                )

                // ── 外周有机细胞群：围绕中心呼吸（相位/幅度各自独立）──
                val cells = listOf(
                    CellSpec(sizeDp = 92, angleDeg = 40f, distDp = 112, lobes = 6, wobble = 0.24f, phase = 1.1f, amp = 0.12f, tint = Color(0x33FF8A5C)),
                    CellSpec(sizeDp = 74, angleDeg = 150f, distDp = 106, lobes = 4, wobble = 0.30f, phase = 2.3f, amp = 0.16f, tint = Color(0x33FF4FA3)),
                    CellSpec(sizeDp = 104, angleDeg = 240f, distDp = 118, lobes = 7, wobble = 0.16f, phase = 3.6f, amp = 0.10f, tint = Color(0x3353E8FF)),
                    CellSpec(sizeDp = 60, angleDeg = 310f, distDp = 96, lobes = 5, wobble = 0.26f, phase = 4.7f, amp = 0.18f, tint = Color(0x33B08CFF)),
                    CellSpec(sizeDp = 48, angleDeg = 95f, distDp = 158, lobes = 3, wobble = 0.34f, phase = 5.5f, amp = 0.20f, tint = Color(0x66FFE08A))
                )
                cells.forEach { cell ->
                    val wob = sin(cycle + cell.phase)
                    val scale = 1f + cell.amp * wob
                    val shapePhase = breath * 2f + cell.phase
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .offset {
                                // distDp → px（在 Density 作用域内换算，修正旧版 dp 当 px 用的问题）
                                val d = cell.distDp.dp.toPx() * (1f + 0.05f * wob)
                                IntOffset(
                                    x = (cos(Math.toRadians(cell.angleDeg.toDouble())) * d).roundToInt(),
                                    y = (sin(Math.toRadians(cell.angleDeg.toDouble())) * d).roundToInt()
                                )
                            }
                            .size(cell.sizeDp.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                rotationZ = 3f * wob
                            }
                            .softGlass(
                                shape = { BlobShape(lobes = cell.lobes, wobble = cell.wobble, phase = shapePhase) },
                                blurRadius = 2.dp,
                                surface = { blobGlassSurface(cell, shapePhase) }
                            )
                    )
                }
            }

            Text(
                "中心为连续曲率晶胞（折射）；外周细胞由极坐标函数实时生成：r = R·(1 + a·sin(kθ + t))",
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
    val phase: Float,
    val amp: Float,
    val tint: Color
)

/** 有机细胞表面：淡彩基底 + 加法内辉光 + 左上亮右下暗的镜面描边。 */
private fun DrawScope.blobGlassSurface(cell: CellSpec, phase: Float) {
    val path = buildBlobPath(size, cell.lobes, cell.wobble, phase)
    // 1) 淡彩基底（顶部带细胞色，底部偏白）
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                cell.tint.copy(alpha = (cell.tint.alpha * 1.4f).coerceAtMost(1f)),
                Color.White.copy(alpha = 0.05f)
            )
        )
    )
    // 2) 内辉光（加法混合，左上受光面更亮）
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.13f), Color.Transparent),
            center = Offset(size.width * 0.40f, size.height * 0.34f),
            radius = size.minDimension * 0.60f
        ),
        blendMode = BlendMode.Plus
    )
    // 3) 定向镜面描边（液态玻璃边缘光）
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.06f)),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        ),
        style = Stroke(width = 1.2.dp.toPx())
    )
}

/** 有机「细胞」形状：极坐标花瓣闭合平滑曲线。 */
private class BlobShape(
    private val lobes: Int,
    private val wobble: Float,
    private val phase: Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(buildBlobPath(size, lobes, wobble, phase))
}

/** 极坐标花瓣曲线（Catmull-Rom 平滑闭合），供形状轮廓与表面描边共用。 */
private fun buildBlobPath(
    size: Size,
    lobes: Int,
    wobble: Float,
    phase: Float
): Path {
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
