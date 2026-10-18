package com.ultraguru.liquidglass.plugin.kyant0.scenes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule

/**
 * 场景 2：按钮 UI。
 * 胶囊玻璃按钮（按压缩放弹性）+ 图标按钮 + 开关 + FAB。
 * 按压时观察玻璃面在背景光斑上的折射变化。
 */
@Composable
fun ButtonsScene() {
    DemoBackdropLayer {
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
            Text("按压观察形变与折射光斑变化", color = Color(0x99FFFFFF), fontSize = 12.sp)
            Spacer(Modifier.height(28.dp))

            GlassButton(
                label = "主按钮 · Liquid",
                onClick = {},
                modifier = Modifier.fillMaxWidth(0.72f),
                height = 56.dp
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GlassButton("次要", {}, compact = true)
                GlassButton("危险", {}, compact = true)
                GlassButton("禁用", {}, compact = true, enabled = false)
            }
            Spacer(Modifier.height(26.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassIconButton(Icons.Filled.Favorite)
                GlassIconButton(Icons.Filled.Notifications)
                GlassIconButton(Icons.Filled.Share)
                GlassIconButton(Icons.Filled.ThumbUp)
            }
            Spacer(Modifier.height(26.dp))

            var on by remember { mutableStateOf(true) }
            GlassToggle(checked = on, onCheckedChange = { on = it })
            Spacer(Modifier.height(30.dp))

            Text("FAB", color = Color(0x99FFFFFF), fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            GlassFab(Icons.Filled.Refresh)
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** 统一玻璃配方（@Composable：读 LocalBackdrop）。 */
@Composable
private fun Modifier.glassSurface(
    shape: () -> androidx.compose.ui.graphics.Shape,
    tint: Float = 0.18f
): Modifier = this.drawBackdrop(
        backdrop = LocalBackdrop.current,
        shape = shape,
        effects = {
            vibrancy()
            blur(2.dp.toPx())
            lens(12.dp.toPx(), 24.dp.toPx())
        },
        onDrawSurface = { drawRect(Color.Black.copy(alpha = tint)) }
    )

/** 胶囊玻璃按钮：按压缩放 + 下沉反馈。 */
@Composable
private fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 42.dp,
    compact: Boolean = false,
    enabled: Boolean = true
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "btnScale"
    )
    Row(
        modifier
            .then(if (compact) Modifier else Modifier)
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (pressed) 2.dp.toPx() else 0f
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
            .glassSurface(shape = { Capsule() }, tint = if (enabled) 0.18f else 0.34f),
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

@Composable
private fun GlassIconButton(icon: ImageVector) {
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
            .glassSurface(shape = { CircleShape }, tint = 0.16f),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

/** 玻璃开关：胶囊轨道 + 玻璃滑块。 */
@Composable
private fun GlassToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val thumb by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f),
        label = "toggle"
    )
    Box(
        Modifier
            .width(76.dp)
            .height(44.dp)
            .clickable { onCheckedChange(!checked) }
            .glassSurface(
                shape = { Capsule() },
                tint = if (checked) 0.10f else 0.28f
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .padding(start = 5.dp + (30.dp * thumb))
                .size(34.dp)
                .drawBackdrop(
                    backdrop = LocalBackdrop.current,
                    shape = { CircleShape },
                    effects = {
                        vibrancy()
                        blur(1.dp.toPx())
                        lens(8.dp.toPx(), 16.dp.toPx())
                    },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.35f)) }
                )
        )
    }
}

/** 圆形 FAB。 */
@Composable
private fun GlassFab(icon: ImageVector) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "fab"
    )
    Box(
        Modifier
            .size(64.dp)
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
            .drawBackdrop(
                backdrop = LocalBackdrop.current,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(14.dp.toPx(), 28.dp.toPx())
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.14f)) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}
