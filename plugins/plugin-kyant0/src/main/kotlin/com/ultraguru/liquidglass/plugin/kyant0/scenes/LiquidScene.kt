package com.ultraguru.liquidglass.plugin.kyant0.scenes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 场景 3：液体。
 * 大玻璃液滴跟随手指拖拽（弹性 spring），带色散折射
 * （lens chromaticAberration = true）；松手后自动漂浮。
 */
@Composable
fun LiquidScene() {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val boxSizePx = with(density) { 150.dp.toPx() }

    // 液滴中心（px, 屏幕坐标）
    val position = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
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
                                position.snapTo(target)
                                initialized = true
                            }
                            dragging = true
                            scope.launch {
                                position.animateTo(target, spring(stiffness = 600f))
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
                        },
                        onDragEnd = {
                            dragging = false
                            scope.launch {
                                position.animateTo(
                                    Offset(position.targetValue.x, position.targetValue.y - 60f),
                                    spring(dampingRatio = 0.35f, stiffness = 250f)
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
                    "拖动液滴 🫧\n松手后弹性回弹",
                    color = Color(0xB3FFFFFF),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 液滴
            val idleT = if (dragging) 0f else kotlin.math.sin(floatT * 6.28318f)
            val x = (if (initialized) position.value.x else 400f) + if (!dragging && initialized) idleT * 10f else 0f
            val y = (if (initialized) position.value.y else 700f) + if (!dragging && initialized) kotlin.math.abs(idleT) * -14f else 0f
            val squash = (stretch * 0.18f).coerceIn(0f, 0.18f)

            Box(
                Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .size(150.dp)
                    .graphicsLayer {
                        scaleX = 1f + squash
                        scaleY = 1f - squash
                    }
                    .drawBackdrop(
                        backdrop = LocalBackdrop.current,
                        shape = { CircleShape },
                        effects = {
                            vibrancy()
                            blur(1.dp.toPx())
                            lens(
                                refractionHeight = 28.dp.toPx(),
                                refractionAmount = 56.dp.toPx(),
                                chromaticAberration = true
                            )
                        },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.10f)) }
                    )
            )
            // 形变衰减
            androidx.compose.runtime.LaunchedEffect(dragging) {
                if (!dragging) while (stretch > 0f) {
                    stretch = (stretch - 0.05f).coerceAtLeast(0f)
                    kotlinx.coroutines.delay(40)
                }
            }
        }
    }
}
