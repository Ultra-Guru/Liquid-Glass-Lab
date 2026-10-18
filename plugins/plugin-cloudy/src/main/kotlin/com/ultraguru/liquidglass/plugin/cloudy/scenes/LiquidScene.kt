package com.ultraguru.liquidglass.plugin.cloudy.scenes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.cloudy.ExperimentalLiquidGlassMotion
import com.skydoves.cloudy.LiquidGlassGlow
import com.skydoves.cloudy.liquidGlass
import com.skydoves.cloudy.rememberGyroLightSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 场景 3（Cloudy）：液体。
 *
 * 液滴 = 背景层上的 `liquidGlass` 透镜（RenderEffect 只作用于自身内容，
 * 所以透镜必须挂在「画背景的那个 Box」上）：
 * - `lensCenter` 跟随手指（spring 弹性跟随）；
 * - `lensSize` 随拖拽速度挤压/拉伸（液体感）；
 * - `dispersion` 色散折射 + `rememberGyroLightSource()` 陀螺仪动态高光
 *   （倾斜设备看液滴边缘的镜面高光扫动）。
 */
@OptIn(ExperimentalLiquidGlassMotion::class)
@Composable
fun LiquidScene() {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dropPx = with(density) { 150.dp.toPx() }

    // 陀螺仪光源：倾斜设备，液滴边缘高光会扫动
    val gyroLight = rememberGyroLightSource()

    // 液滴中心（背景 Box 的本地坐标，px）
    val position = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
    var initialized by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }

    // 拖拽速度 → 挤压形变（0..1）
    var stretch by remember { mutableStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { coords ->
                if (!initialized) {
                    position.snapTo(Offset(coords.width / 2f, coords.height * 0.42f))
                    initialized = true
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start ->
                        dragging = true
                        scope.launch {
                            position.animateTo(
                                start,
                                spring(stiffness = 900f)
                            )
                        }
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        scope.launch {
                            stretch = (stretch + amount.getDistance() / 90f).coerceIn(0f, 1f)
                            position.animateTo(
                                position.targetValue + amount,
                                spring(dampingRatio = 0.75f, stiffness = 1100f)
                            )
                        }
                    },
                    onDragEnd = {
                        dragging = false
                        scope.launch {
                            position.animateTo(
                                Offset(position.targetValue.x, position.targetValue.y - 70f),
                                spring(dampingRatio = 0.35f, stiffness = 220f)
                            )
                        }
                    }
                )
            }
    ) {
        // ── 背景 + 透镜（液滴本体）：透镜挂在画背景的 Box 上 ──
        val squash = (stretch * 0.22f).coerceIn(0f, 0.22f)
        val lensW = dropPx * (1f + squash)
        val lensH = dropPx * (1f - squash)
        Box(
            Modifier
                .fillMaxSize()
                .liquidGlass(
                    lensCenter = position.value,
                    lensSize = Size(lensW, lensH),
                    cornerRadius = lensH / 2f,
                    refraction = 0.30f,
                    curve = 0.35f,
                    dispersion = 0.55f,
                    edge = 0.28f,
                    light = gyroLight,
                    glow = LiquidGlassGlow(intensity = 0.8f, sharpness = 11f),
                    enabled = initialized
                )
        ) {
            RichGradient()
        }

        // ── 液滴轮廓提示环（跟随液滴，脉冲呼吸） ──
        val ringAlpha = if (dragging) 0.55f else 0.25f
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = ringAlpha),
                radius = lensW / 2f + 8f,
                center = position.value,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // ── 指引文字 ──
        if (!dragging) {
            Text(
                "拖动液滴\n松手后弹性回弹 · 倾斜设备看高光扫动",
                color = Color(0xB3FFFFFF),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 48.dp)
            )
        }

        // 形变衰减（松手后缓慢回弹）
        LaunchedEffect(dragging) {
            if (!dragging) while (stretch > 0f) {
                stretch = (stretch - 0.05f).coerceAtLeast(0f)
                delay(40)
            }
        }
    }
}
