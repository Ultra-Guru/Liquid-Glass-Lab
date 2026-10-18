package com.ultraguru.liquidglass.plugin.kyant0.scenes

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

/**
 * 场景 2：按钮 UI。
 * 胶囊玻璃按钮（按压缩放 + 加深模糊 + 表面变暗）+ 图标按钮 + 连续曲率矩阵磁贴 +
 * 开关 + FAB。按压时观察玻璃面在背景光斑上的折射变化。
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
            Text("按压观察形变 · 模糊加深 · 折射光斑变化", color = Color(0x99FFFFFF), fontSize = 12.sp)
            Spacer(Modifier.height(28.dp))

            SectionLabel("胶囊按钮")
            Spacer(Modifier.height(12.dp))
            GlassButton(
                label = "主按钮 · Liquid",
                onClick = {},
                modifier = Modifier.fillMaxWidth(0.72f),
                height = 56.dp,
                dispersive = true
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GlassButton("次要", {}, compact = true)
                GlassButton("危险", {}, compact = true)
                GlassButton("禁用", {}, compact = true, enabled = false)
            }
            Spacer(Modifier.height(28.dp))

            SectionLabel("图标按钮")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassIconButton(Icons.Filled.Favorite)
                GlassIconButton(Icons.Filled.Notifications)
                GlassIconButton(Icons.Filled.Share)
                GlassIconButton(Icons.Filled.ThumbUp)
            }
            Spacer(Modifier.height(28.dp))

            SectionLabel("连续曲率矩阵")
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassTile(Icons.Filled.Home, "首页", Modifier.weight(1f))
                GlassTile(Icons.Filled.Search, "搜索", Modifier.weight(1f))
                GlassTile(Icons.Filled.Star, "收藏", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassTile(Icons.Filled.Favorite, "喜欢", Modifier.weight(1f))
                GlassTile(Icons.Filled.Notifications, "提醒", Modifier.weight(1f))
                GlassTile(Icons.Filled.Share, "分享", Modifier.weight(1f))
            }
            Spacer(Modifier.height(28.dp))

            SectionLabel("开关")
            Spacer(Modifier.height(12.dp))
            var on by remember { mutableStateOf(true) }
            GlassToggle(checked = on, onCheckedChange = { on = it })
            Spacer(Modifier.height(30.dp))

            SectionLabel("FAB")
            Spacer(Modifier.height(14.dp))
            GlassFab(Icons.Filled.Refresh)
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Color(0x80FFFFFF),
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )
}

/**
 * 胶囊玻璃按钮：按压缩放 0.97 + 下沉 + 模糊加深 + 表面变暗（镜面减弱），
 * 松手弹性回弹。
 */
@Composable
private fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp,
    compact: Boolean = false,
    enabled: Boolean = true,
    dispersive: Boolean = false
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 900f),
        label = "btnScale"
    )
    val blur by animateDpAsState(
        targetValue = if (pressed) 4.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "btnBlur"
    )
    Row(
        modifier
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
            .liquidGlass(
                shape = { Capsule() },
                blurRadius = blur,
                refractionHeight = 12.dp,
                refractionAmount = 26.dp,
                chromaticAberration = dispersive,
                surface = {
                    val radius = (size.minDimension * 0.5f).toDp()
                    roundedGlassSurface(
                        radius,
                        if (enabled) Color.Black.copy(alpha = if (pressed) 0.24f else 0.18f) else Color.Black.copy(alpha = 0.34f),
                        specularAlpha = if (pressed) 0.16f else 0.30f
                    )
                    roundedGlassRim(radius, width = 1.25.dp, alpha = if (enabled) 0.5f else 0.24f)
                }
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

/** 圆形图标按钮：按压更强形变（0.88）+ 表面收紧。 */
@Composable
private fun GlassIconButton(icon: ImageVector) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "iconScale"
    )
    val blur by animateDpAsState(
        targetValue = if (pressed) 3.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "iconBlur"
    )
    Box(
        Modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
                shape = { CircleShape },
                blurRadius = blur,
                refractionHeight = 10.dp,
                refractionAmount = 22.dp,
                surface = {
                    circleGlassSurface(
                        Color.Black.copy(alpha = if (pressed) 0.26f else 0.16f),
                        specularAlpha = if (pressed) 0.22f else 0.34f
                    )
                    circleGlassRim(width = 1.dp, alpha = 0.5f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

/** 连续曲率方形磁贴（squircle）：lens 对连续曲率的光滑弯曲最友好。 */
@Composable
private fun GlassTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "tileScale"
    )
    val blur by animateDpAsState(
        targetValue = if (pressed) 3.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "tileBlur"
    )
    Column(
        modifier
            .height(84.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
                shape = { RoundedRectangle(20.dp) },
                blurRadius = blur,
                refractionHeight = 11.dp,
                refractionAmount = 22.dp,
                surface = {
                    roundedGlassSurface(20.dp, Color.Black.copy(alpha = if (pressed) 0.26f else 0.18f))
                    roundedGlassRim(20.dp)
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = Color(0xF2FFFFFF), modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color(0xCCFFFFFF), fontSize = 11.sp)
    }
}

/** 玻璃开关：胶囊轨道 + 玻璃滑块（选中时轨道变亮、镜面增强）。 */
@Composable
private fun GlassToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val thumb by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 600f),
        label = "toggle"
    )
    Box(
        Modifier
            .width(78.dp)
            .height(44.dp)
            .clickable { onCheckedChange(!checked) }
            .liquidGlass(
                shape = { Capsule() },
                blurRadius = 2.dp,
                refractionHeight = 10.dp,
                refractionAmount = 22.dp,
                surface = {
                    // 未选中 0.26 → 选中 0.10：选中时更「透」
                    val baseAlpha = 0.26f + (0.10f - 0.26f) * thumb
                    val radius = (size.minDimension * 0.5f).toDp()
                    roundedGlassSurface(
                        radius,
                        Color.Black.copy(alpha = baseAlpha),
                        specularAlpha = 0.22f + 0.10f * thumb
                    )
                    roundedGlassRim(radius, width = 1.dp, alpha = 0.42f)
                }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .padding(start = 5.dp + (34.dp * thumb))
                .size(34.dp)
                .drawBackdrop(
                    backdrop = LocalBackdrop.current,
                    shape = { CircleShape },
                    highlight = null,
                    effects = {
                        vibrancy()
                        colorControls(brightness = 0.03f, contrast = 1.04f)
                        blur(1.dp.toPx())
                        lens(
                            refractionHeight = 9.dp.toPx(),
                            refractionAmount = 18.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = true
                        )
                    },
                    onDrawSurface = {
                        circleGlassSurface(Color.White.copy(alpha = 0.26f), specularAlpha = 0.45f)
                        circleGlassRim(width = 1.dp, alpha = 0.6f)
                    }
                )
        )
    }
}

/** 圆形 FAB：更强的折射 + 轻度色散。 */
@Composable
private fun GlassFab(icon: ImageVector) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "fab"
    )
    Box(
        Modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
                shape = { CircleShape },
                blurRadius = 2.dp,
                refractionHeight = 14.dp,
                refractionAmount = 30.dp,
                chromaticAberration = true,
                surface = {
                    circleGlassSurface(Color.White.copy(alpha = 0.10f), specularAlpha = 0.36f)
                    circleGlassRim(width = 1.5.dp, alpha = 0.55f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}
