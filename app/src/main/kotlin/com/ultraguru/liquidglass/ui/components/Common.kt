package com.ultraguru.liquidglass.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraguru.liquidglass.ui.theme.BrandRed
import com.ultraguru.liquidglass.ui.theme.BrandRedDeep
import com.ultraguru.liquidglass.ui.theme.LocalGlassDarkTheme
import kotlin.math.sin

/* ─────────────────────────────────────────────────────────────────────
 * 液态玻璃视觉体系（宿主 UI 精致化）
 *
 * 设计语言：
 *  - AuroraBackground：深浅自适应的「液体极光」动效背景（漂浮色斑）
 *  - GlassCard：磨砂玻璃卡片（半透渐变体 + 顶缘高光描边 + 柔影）
 *  - 全部组件在彩色背景上保持可读性（暗色提亮 / 亮色压暗两套系数）
 * ──────────────────────────────────────────────────────────────────── */

/**
 * 液体极光背景：多个大尺寸色斑沿利萨如轨迹缓慢漂移。
 * 深色模式：品牌红 / 紫 / 青三斑；浅色模式：同色系低饱和。
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val dark = LocalGlassDarkTheme.current
    val transition = rememberInfiniteTransition(label = "aurora")
    val t1 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing)),
        label = "blob1"
    )
    val t2 by transition.animateFloat(
        initialValue = 0.33f, targetValue = 1.33f,
        animationSpec = infiniteRepeatable(tween(34000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blob2"
    )
    val t3 by transition.animateFloat(
        initialValue = 0.66f, targetValue = 1.66f,
        animationSpec = infiniteRepeatable(tween(42000, easing = LinearEasing)),
        label = "blob3"
    )
    val base = MaterialTheme.colorScheme.background
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(base)
        val w = size.width
        val h = size.height
        val a = if (dark) 1f else 0.55f
        // 斑1：品牌红 —— 左上区漂移
        drawBlob(
            center = Offset(
                x = w * (0.24f + 0.10f * lissajous(t1, 1.6f)),
                y = h * (0.20f + 0.07f * lissajous(t1 + 0.25f, 2.2f))
            ),
            radius = h * 0.42f,
            color = Color(0xFFE0524A),
            alpha = 0.20f * a
        )
        // 斑2：紫 —— 右侧漂移
        drawBlob(
            center = Offset(
                x = w * (0.82f + 0.09f * lissajous(t2, 2.0f)),
                y = h * (0.38f + 0.10f * lissajous(t2, 1.4f))
            ),
            radius = h * 0.46f,
            color = Color(0xFF7C4DFF),
            alpha = 0.15f * a
        )
        // 斑3：青 —— 底部漂移
        drawBlob(
            center = Offset(
                x = w * (0.45f + 0.12f * lissajous(t3, 1.3f)),
                y = h * (0.88f + 0.06f * lissajous(t3 + 0.5f, 1.8f))
            ),
            radius = h * 0.44f,
            color = Color(0xFF22B8CF),
            alpha = 0.13f * a
        )
        // 顶部辉光（模拟光源自上而下）
        drawRect(
            brush = Brush.verticalGradient(
                colors = if (dark) {
                    listOf(Color.White.copy(alpha = 0.045f), Color.Transparent)
                } else {
                    listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
                }
            )
        )
    }
}

/** 利萨如相位 → [-1, 1] 波形。 */
private fun lissajous(t: Float, freq: Float): Float = sin(t * 2f * Math.PI.toFloat() * freq)

private fun DrawScope.drawBlob(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/**
 * 磨砂玻璃卡片：半透渐变体 + 顶缘高光描边 + 柔影 + 圆角裁剪。
 * [onClick] 非空时渲染点击态（涟漪）；内容布局由调用方控制。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = LocalGlassDarkTheme.current
    val shape = RoundedCornerShape(cornerRadius)
    val body = if (dark) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.035f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.66f), Color.White.copy(alpha = 0.34f))
        )
    }
    val edge = if (dark) {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.05f))
        )
    } else {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.98f), Color.White.copy(alpha = 0.40f))
        )
    }
    val interaction = remember { MutableInteractionSource() }
    val base = modifier
        .shadow(elevation = elevation, shape = shape, clip = false)
        .clip(shape)
        .background(body)
        .border(width = 1.dp, brush = edge, shape = shape)
    val clickable = if (onClick != null) {
        base.clickable(
            interactionSource = interaction,
            indication = ripple(),
            onClick = onClick
        )
    } else base
    Box(clickable) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/** 星数徽标（玻璃化）。 */
@Composable
fun StarsBadge(stars: Int, modifier: Modifier = Modifier) {
    val dark = LocalGlassDarkTheme.current
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = Color(0xFFFFC24B)
            )
            Text(
                "$stars",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 状态小胶囊（已安装 / 未安装 / 可更新 / License …；玻璃化）。 */
@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    val dark = LocalGlassDarkTheme.current
    val bg = when {
        accent -> BrandRed.copy(alpha = if (dark) 0.26f else 0.14f)
        dark -> Color.White.copy(alpha = 0.08f)
        else -> Color.White.copy(alpha = 0.55f)
    }
    val fg = if (accent) {
        if (dark) Color(0xFFFFB4A9) else BrandRedDeep
    } else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = bg,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (accent) BrandRed.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (accent) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = fg
                )
                Spacer(Modifier.size(4.dp))
            }
            Text(text, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.Medium)
        }
    }
}

/** 「关于 / 设置」导航行（玻璃化）。 */
@Composable
fun NavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        elevation = 8.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(BrandRedDeep, BrandRed)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
