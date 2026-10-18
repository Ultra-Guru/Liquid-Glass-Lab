package com.ultraguru.liquidglass.plugin.nadeem.scenes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nadeemiqbal.liquidglass.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 场景 3：液体（nadeem）。
 *
 * 库本身不含拖拽 / 弹簧动画——按官方建议用 Compose 动画在外层驱动：
 * Animatable(Offset) + spring 跟随手指（拖拽），松手上浮回弹；
 * 卫星小滴以更软的弹簧持续追赶主滴（黏连滞后）。玻璃面用
 * Modifier.liquidGlass 的 refraction（AGSL 正弦折射）+ grain 呈现
 * 液体质感；graphicsLayer 按速度做挤压形变。
 */
@Composable
internal fun LiquidScene() {
    DemoBackdropLayer { state ->
        val scope = rememberCoroutineScope()
        val dropSize = 150.dp
        val satelliteSize = 84.dp
        val dropPx = with(LocalDensity.current) { dropSize.toPx() }

        val position = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
        val satellite = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
        var dragging by remember { mutableStateOf(false) }
        var started by remember { mutableStateOf(false) }

        // 卫星小滴：软弹簧持续追赶主滴（黏连滞后感）
        LaunchedEffect(Unit) {
            while (isActive) {
                if (started) {
                    satellite.animateTo(
                        position.value + Offset(150f, 70f),
                        spring(dampingRatio = 0.42f, stiffness = 110f)
                    )
                } else {
                    delay(100)
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .onSizeChanged { box ->
                    if (!started) {
                        // 初始：液滴居中（snapTo 是挂起函数 → 协程包裹）
                        scope.launch {
                            position.snapTo(Offset((box.width - dropPx) / 2f, (box.height - dropPx) / 2f))
                        }
                        started = true
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            started = true
                            dragging = true
                            scope.launch {
                                // 指尖对齐液滴中心
                                position.animateTo(
                                    Offset(start.x - dropPx / 2f, start.y - dropPx / 2f),
                                    spring(dampingRatio = 0.7f, stiffness = 1400f)
                                )
                            }
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            scope.launch {
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
                                    position.targetValue + Offset(0f, -60f),
                                    spring(dampingRatio = 0.35f, stiffness = 220f)
                                )
                            }
                        }
                    )
                }
        ) {
            if (!dragging) {
                Text(
                    "拖动液滴\n松手后弹性回弹 · 尾随小滴黏连滞后",
                    color = Color(0xB3FFFFFF),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // ── 主液滴：refraction 正弦折射 + 速度挤压 ──
            val v = position.velocity
            val squash = if (dragging) 0.16f else 0.08f
            Box(
                Modifier
                    .offset { IntOffset(position.value.x.roundToInt(), position.value.y.roundToInt()) }
                    .size(dropSize)
                    .graphicsLayer {
                        scaleX = 1f + (abs(v.x) / 6000f).coerceIn(0f, squash) -
                            (abs(v.y) / 9000f).coerceIn(0f, squash / 2f)
                        scaleY = 1f + (abs(v.y) / 6000f).coerceIn(0f, squash) -
                            (abs(v.x) / 9000f).coerceIn(0f, squash / 2f)
                    }
                    .liquidGlass(
                        state = state,
                        shape = CircleShape,
                        tint = Color(0x24FFFFFF),
                        blurRadius = 12.dp,
                        saturation = 1.6f,
                        grain = 0.05f,
                        refraction = 0.8f
                    )
            )

            // ── 卫星小滴（软弹簧追赶） ──
            if (started) {
                Box(
                    Modifier
                        .offset { IntOffset(satellite.value.x.roundToInt(), satellite.value.y.roundToInt()) }
                        .size(satelliteSize)
                        .liquidGlass(
                            state = state,
                            shape = CircleShape,
                            tint = Color(0x2EFFFFFF),
                            blurRadius = 10.dp,
                            saturation = 1.6f,
                            grain = 0.04f,
                            refraction = 0.65f
                        )
                )
            }
        }
    }
}
