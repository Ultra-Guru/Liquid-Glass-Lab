package com.ultraguru.liquidglass.plugin.cloudy.scenes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.cloudy.LiquidGlassGlow
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.liquidGlass

/**
 * 场景 2（Cloudy）：按钮 UI。
 *
 * 两种玻璃配方并排对比：
 * - 磨砂玻璃：`cloudy(sky, radius)`（背景模糊，iOS 控制中心质感）；
 * - 液态玻璃：`liquidGlass(lensCenter)` 盖在自身半透明表面上——
 *   折射对均匀填充不可见，但 SDF 边缘光 + 镜面高光（glow）让按钮
 *   呈现出"果冻胶囊"的高光质感。
 * 全部带按压缩放 / 下沉弹性反馈。
 */
@Composable
fun ButtonsScene() {
    DemoSkyLayer {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("玻璃按钮矩阵", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("cloudy 磨砂 + liquidGlass 高光 · 按压看反馈", color = Color(0x99FFFFFF), fontSize = 12.sp)
            Spacer(Modifier.height(28.dp))

            // ── 液态高光按钮（liquidGlass 透镜盖自身表面） ──
            GlossyButton(
                label = "主按钮 · Liquid",
                onClick = { },
                modifier = Modifier.fillMaxWidth(0.72f),
                height = 56.dp
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GlossyButton("次要", {}, compact = true)
                GlossyButton("危险", {}, compact = true)
                GlossyButton("禁用", {}, compact = true, enabled = false)
            }
            Spacer(Modifier.height(26.dp))

            // ── 磨砂按钮（cloudy 背景模糊） ──
            FrostedButton(
                label = "磨砂主按钮 · Cloudy",
                onClick = { },
                modifier = Modifier.fillMaxWidth(0.72f),
                height = 52.dp
            )
            Spacer(Modifier.height(26.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FrostedIconButton(Icons.Filled.Favorite)
                FrostedIconButton(Icons.Filled.Notifications)
                FrostedIconButton(Icons.Filled.Share)
                FrostedIconButton(Icons.Filled.ThumbUp)
            }
            Spacer(Modifier.height(26.dp))

            var on by remember { mutableStateOf(true) }
            GlassToggle(checked = on, onCheckedChange = { on = it })
            Spacer(Modifier.height(30.dp))

            Text("FAB（liquidGlass 高光）", color = Color(0x99FFFFFF), fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            GlossyFab(Icons.Filled.Refresh)
            Spacer(Modifier.height(40.dp))
        }
    }
}

/**
 * 液态玻璃胶囊按钮：`liquidGlass` 透镜铺满自身 + 半透明填充，
 * 边缘光 / 镜面高光构成"果冻"质感。
 */
@Composable
private fun GlossyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
    compact: Boolean = false,
    enabled: Boolean = true
) {
    var pressed by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "glossyScale"
    )
    val lensSize = if (size == IntSize.Zero) Size(1f, 1f) else Size(size.width.toFloat(), size.height.toFloat())
    Row(
        modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (pressed) 2.dp.toPx() else 0f
            }
            .onSizeChanged { size = it }
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (!enabled) return@detectTapGestures
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { if (enabled) onClick() }
                )
            }
            .liquidGlass(
                lensCenter = Offset(size.width / 2f, size.height / 2f),
                lensSize = lensSize,
                cornerRadius = lensSize.height / 2f,
                refraction = 0.12f,
                curve = 0.18f,
                dispersion = 0.18f,
                edge = 0.55f,
                glow = LiquidGlassGlow(intensity = 0.85f, sharpness = 12f)
            )
            .background(
                if (enabled) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.38f),
                RoundedCornerShape(50)
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (enabled) Color(0xF2FFFFFF) else Color(0x66FFFFFF),
            fontSize = if (compact) 13.sp else 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = if (compact) 18.dp else 32.dp)
        )
    }
}

/** 磨砂按钮：cloudy(sky) 背景模糊，玻璃后面能看到光斑被糊开。 */
@Composable
private fun FrostedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
    enabled: Boolean = true
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "frostScale"
    )
    Row(
        modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (!enabled) return@detectTapGestures
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { if (enabled) onClick() }
                )
            }
            .cloudy(
                sky = LocalSky.current,
                radius = 18,
                tint = if (enabled) Color.Black.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.42f),
                shape = RoundedCornerShape(50)
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (enabled) Color(0xF2FFFFFF) else Color(0x66FFFFFF),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 30.dp)
        )
    }
}

/** 磨砂圆形图标按钮。 */
@Composable
private fun FrostedIconButton(icon: ImageVector) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "iconScale"
    )
    Box(
        Modifier
            .size(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            }
            .cloudy(
                sky = LocalSky.current,
                radius = 16,
                tint = Color.Black.copy(alpha = 0.16f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

/**
 * 玻璃开关：磨砂胶囊轨道（cloudy）+ 液态高光滑块（liquidGlass）。
 */
@Composable
private fun GlassToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val thumb by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f),
        label = "toggle"
    )
    var thumbSize by remember { mutableStateOf(IntSize.Zero) }
    val thumbLens =
        if (thumbSize == IntSize.Zero) Size(1f, 1f) else Size(thumbSize.width.toFloat(), thumbSize.height.toFloat())

    Box(
        Modifier
            .width(76.dp)
            .height(44.dp)
            .clickable { onCheckedChange(!checked) }
            .cloudy(
                sky = LocalSky.current,
                radius = 14,
                tint = if (checked) Color(0xFF2EC4B6).copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.30f),
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .padding(start = 5.dp + (30.dp * thumb))
                .size(34.dp)
                .onSizeChanged { thumbSize = it }
                .liquidGlass(
                    lensCenter = Offset(thumbSize.width / 2f, thumbSize.height / 2f),
                    lensSize = thumbLens,
                    cornerRadius = thumbLens.width / 2f,
                    edge = 0.6f,
                    glow = LiquidGlassGlow(intensity = 0.9f, sharpness = 14f)
                )
                .background(Color.White.copy(alpha = 0.38f), CircleShape)
        )
    }
}

/** 圆形 FAB：液态高光透镜 + 中心图标。 */
@Composable
private fun GlossyFab(icon: ImageVector) {
    var pressed by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "fab"
    )
    val lensSize = if (size == IntSize.Zero) Size(1f, 1f) else Size(size.width.toFloat(), size.height.toFloat())
    Box(
        Modifier
            .size(64.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            }
            .liquidGlass(
                lensCenter = Offset(size.width / 2f, size.height / 2f),
                lensSize = lensSize,
                cornerRadius = lensSize.width / 2f,
                refraction = 0.15f,
                curve = 0.22f,
                dispersion = 0.22f,
                edge = 0.5f,
                glow = LiquidGlassGlow(intensity = 0.9f, sharpness = 13f)
            )
            .background(Color.White.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}
