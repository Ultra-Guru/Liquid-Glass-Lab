package com.ultraguru.liquidglass.plugin.kyant0.scenes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 场景 3：液体。
 * 大玻璃液滴跟随手指拖拽（弹性 spring），带 depthEffect + 色散折射
 * （lens chromaticAberration = true）；表面有游移焦散与底部厚度暗层；
 * 一颗卫星小滴以更软的 spring 滞后跟随，松手后双双弹性回弹漂浮。
 */
@Composable
fun LiquidScene() {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val boxSizePx = with(density) { 150.dp.toPx() }

    // 主液滴 / 卫星小滴中心（px，屏幕坐标）
    val position = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    val satellite = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    var initialized by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }

    // 拖拽速度 → 挤压形变
    var stretch by remember { mutableStateOf(0f) }

    // 空闲漂浮
    val infinite = rememberInfiniteTransition(label = "float")
    val floatT by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200)),
        label = "floatT"
    )

    DemoBackdropLayer { _ ->
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val target = Offset(start.x - boxSizePx / 2, start.y - boxSizePx / 2)
                            if (!initialized) {
                                scope.launch {
                                    position.snapTo(target)
                                    satellite.snapTo(target + Offset(0f, 96f))
                                }
                                initialized = true
                            }
                            dragging = true
                            scope.launch {
                                position.animateTo(target, spring(stiffness = 600f))
                            }
                            scope.launch {
                                satellite.animateTo(
                                    target + Offset(0f, 96f),
                                    spring(dampingRatio = 0.55f, stiffness = 240f)
                                )
                            }
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            scope.launch {
                                stretch = (stretch + amount.getDistance() / 80f).coerceIn(0f, 1f)
                                position.animateTo(
                                    position.targetValue + amount,
                                    spring(dampingRatio = 0.75f, stiffness = 900f)
                                )
                            }
                            scope.launch {
                                satellite.animateTo(
                                    position.targetValue + amount + Offset(0f, 96f),
                                    spring(dampingRatio = 0.5f, stiffness = 260f)
                                )
                            }
                        },
                        onDragEnd = {
                            dragging = false
                            val rest = Offset(position.targetValue.x, position.targetValue.y - 60f)
                            scope.launch {
                                position.animateTo(
                                    rest,
                                    spring(dampingRatio = 0.35f, stiffness = 250f)
                                )
                            }
                            scope.launch {
                                satellite.animateTo(
                                    rest + Offset(0f, 96f),
                                    spring(dampingRatio = 0.6f, stiffness = 180f)
                                )
                            }
                        }
                    )
                },
            contentAlignment = Alignment.TopStart
        ) {
            // 指引
            if (!dragging) {
                Text(
                    "拖动液滴 🫧\n卫星小滴弹性跟随 · 松手回弹",
                    color = Color(0xB3FFFFFF),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            val idleT = if (dragging) 0f else sin(floatT * 6.28318f)
            val sIdle = if (dragging) 0f else sin(floatT * 6.28318f + 0.8f)
            val x = (if (initialized) position.value.x else 400f) + if (!dragging && initialized) idleT * 10f else 0f
            val y = (if (initialized) position.value.y else 700f) + if (!dragging && initialized) abs(idleT) * -14f else 0f
            val squash = (stretch * 0.16f).coerceIn(0f, 0.16f)

            // ── 卫星小滴：滞后弹性跟随，像被主滴拖着走的界面张力 ──
            val sx = if (initialized) satellite.value.x else 470f
            val sy = if (initialized) satellite.value.y else 796f
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            (sx + sIdle * 7f).roundToInt(),
                            (sy + abs(sIdle) * -10f).roundToInt()
                        )
                    }
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = 1f + squash * 0.5f
                        scaleY = 1f - squash * 0.4f
                    }
                    .liquidGlass(
                        shape = { CircleShape },
                        blurRadius = 1.dp,
                        refractionHeight = 14.dp,
                        refractionAmount = 28.dp,
                        chromaticAberration = true,
                        surface = {
                            circleGlassSurface(Color.White.copy(alpha = 0.07f), specularAlpha = 0.28f)
                            circleGlassRim(width = 1.dp, alpha = 0.45f)
                        }
                    )
            )

            // ── 主液滴 ──
            Box(
                Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .size(150.dp)
                    .graphicsLayer {
                        scaleX = 1f + squash
                        scaleY = 1f - squash
                        rotationZ = if (dragging) 0f else idleT * 1.5f
                    }
                    .drawBackdrop(
                        backdrop = LocalBackdrop.current,
                        shape = { CircleShape },
                        highlight = null,
                        onDrawBehind = {
                            // 液滴外圈的柔光晕（玻璃把光「漏」出来）
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.10f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                                    radius = size.minDimension * 0.85f
                                ),
                                radius = size.minDimension * 0.85f,
                                center = Offset(size.width * 0.5f, size.height * 0.5f)
                            )
                        },
                        effects = {
                            vibrancy()
                            colorControls(brightness = 0.03f, contrast = 1.05f)
                            blur(1.dp.toPx())
                            lens(
                                refractionHeight = 30.dp.toPx(),
                                refractionAmount = 60.dp.toPx(),
                                depthEffect = true,
                                chromaticAberration = true
                            )
                        },
                        onDrawSurface = {
                            val w = size.width
                            val h = size.height
                            val d = min(w, h)
                            // 1) 基底 + 左上镜面
                            circleGlassSurface(Color.White.copy(alpha = 0.08f), specularAlpha = 0.34f)
                            // 2) 游移焦散（大光斑 + 小热点，随时间缓慢摆动）
                            val c1 = Offset(
                                w * (0.34f + 0.05f * sin(floatT * 6.28318f)),
                                h * (0.30f + 0.04f * sin(floatT * 6.28318f + 1.3f))
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                                    center = c1,
                                    radius = d * 0.36f
                                )
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.38f), Color.Transparent),
                                    center = Offset(w * 0.30f, h * 0.24f),
                                    radius = d * 0.10f
                                )
                            )
                            // 3) 底部厚度（重力感）
                            drawCircle(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f)),
                                    startY = h * 0.55f,
                                    endY = h
                                )
                            )
                            // 4) 边缘光
                            circleGlassRim(width = 1.5.dp, alpha = 0.6f)
                        }
                    )
            )

            // 形变衰减
            LaunchedEffect(dragging) {
                if (!dragging) while (stretch > 0f) {
                    stretch = (stretch - 0.05f).coerceAtLeast(0f)
                    delay(40)
                }
            }
        }
    }
}
