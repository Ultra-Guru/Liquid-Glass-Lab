package com.ultraguru.liquidglass.plugin.nadeem.scenes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nadeemiqbal.liquidglass.GlassButton
import io.github.nadeemiqbal.liquidglass.LiquidGlassState
import io.github.nadeemiqbal.liquidglass.liquidGlass

/**
 * 场景 2：按钮 UI（nadeem）。
 *
 * 两类按钮同屏对比：
 * 1. 库的现成组件 GlassButton（胶囊形 + clickable，组件内部封装了
 *    Modifier.liquidGlass）——展示「拿来即用」；
 * 2. 基于 Modifier.liquidGlass 的自定义按压按钮——用 interactionSource
 *    的 collectIsPressedAsState 驱动 graphicsLayer 弹性缩放（库组件不带
 *    按压反馈，自己补），按压时玻璃整体形变。
 */
@Composable
internal fun ButtonsScene() {
    DemoBackdropLayer { state ->
        var clickCount by remember { mutableIntStateOf(0) }

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
            Text(
                "GlassButton 组件 · liquidGlass 修饰符按压形变",
                color = Color(0x99FFFFFF),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(28.dp))

            // ── 库现成组件：GlassButton（点击计数） ──
            GlassButton(
                state = state,
                onClick = { clickCount++ },
                modifier = Modifier.fillMaxWidth(0.72f),
                tint = DemoGlassTint,
                blurRadius = 20.dp,
                saturation = 1.5f,
                refraction = 0.35f,
                grain = 0.03f
            ) {
                Text(
                    "主按钮 · 已点 $clickCount 次",
                    color = Color(0xF2FFFFFF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(16.dp))

            // ── 次要行：自定义按压形变按钮（Modifier.liquidGlass） ──
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PressGlass(state, "次要") {}
                PressGlass(state, "危险") {}
                PressGlass(state, "强调") {}
            }
            Spacer(Modifier.height(26.dp))

            // ── 图标按钮（圆形玻璃 + 按压缩放） ──
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconGlass(state, Icons.Filled.Favorite)
                IconGlass(state, Icons.Filled.ThumbUp)
                IconGlass(state, Icons.Filled.Refresh)
            }
            Spacer(Modifier.height(30.dp))

            // ── 玻璃开关：胶囊轨道 + 滑块 ──
            var on by remember { mutableStateOf(true) }
            GlassToggle(state, checked = on, onCheckedChange = { on = it })
            Spacer(Modifier.height(34.dp))

            Text("FAB", color = Color(0x99FFFFFF), fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))

            // ── 圆形 FAB（56dp，按压缩放） ──
            IconGlass(state, Icons.Filled.Refresh, size = 64.dp, shape = CircleShape)
            Spacer(Modifier.height(36.dp))
        }
    }
}

/** 自定义按压玻璃按钮：collectIsPressedAsState → graphicsLayer 弹性缩放。 */
@Composable
private fun PressGlass(
    state: LiquidGlassState,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "pressScale"
    )
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .liquidGlass(
                state = state,
                shape = RoundedCornerShape(50),
                tint = DemoGlassTint,
                blurRadius = 16.dp,
                saturation = 1.5f,
                refraction = 0.35f
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(label, color = Color(0xF2FFFFFF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 圆形图标玻璃按钮（默认 56dp，可调尺寸 / 形状）。 */
@Composable
private fun IconGlass(
    state: LiquidGlassState,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: Dp = 56.dp,
    shape: Shape = CircleShape,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "iconScale"
    )
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .liquidGlass(
                state = state,
                shape = shape,
                tint = DemoGlassTint,
                blurRadius = 14.dp,
                saturation = 1.5f,
                refraction = 0.4f
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

/** 玻璃开关：胶囊玻璃轨道 + 玻璃圆滑块（animateFloatAsState 弹性滑动）。 */
@Composable
private fun GlassToggle(
    state: LiquidGlassState,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val thumb by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f),
        label = "toggleThumb"
    )
    Box(
        Modifier
            .width(76.dp)
            .height(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .liquidGlass(
                state = state,
                shape = RoundedCornerShape(50),
                tint = if (checked) Color(0x4D2EC4B6) else DemoGlassTint,
                blurRadius = 14.dp,
                refraction = 0.3f
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // 滑块本身也是一块玻璃
        Box(
            Modifier
                .padding(start = 5.dp + (30.dp * thumb))
                .size(34.dp)
                .liquidGlass(
                    state = state,
                    shape = CircleShape,
                    tint = Color.White.copy(alpha = 0.35f),
                    blurRadius = 8.dp
                )
        )
    }
}
