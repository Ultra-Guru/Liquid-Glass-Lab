package com.ultraguru.liquidglass.plugin.kyant0.scenes

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.min

/**
 * 共享「液态玻璃配方」—— 四场景统一调用，保证观感一致。
 *
 * 效果层（Modifier 扩展，均在 onDrawSurface 之前由 RenderEffect 链处理）：
 *  - [liquidGlass] vibrancy + 轻度色彩校正 + blur + lens 折射（depthEffect 默认开启）。
 *    注意：lens 仅支持圆角矩形族形状（vendor RoundedRectangle / Capsule /
 *    Compose CircleShape 等 CornerBasedShape），传入普通 Shape 会在运行时抛异常。
 *  - [softGlass]  无折射柔玻璃（有机形状专用 —— BlobShape 这类 Generic 轮廓
 *    不能配 lens，用 vibrancy + blur + 表面层补偿质感）。
 *
 * 表面层（onDrawSurface 内使用的 DrawScope 扩展）：
 *  - [roundedGlassSurface] 圆角玻璃三层表面：基础色调（顶部略亮）→ 顶部镜面高光
 *    （约前 30% 高度）→ 底部内反射；
 *  - [roundedGlassRim]     左上亮 → 右下暗的边缘光描边（内缩半个线宽，不越出形状）；
 *  - [circleGlassSurface] / [circleGlassRim] 圆形版本（液滴 / 圆形图标钮 / FAB）。
 */

// ── Modifier 配方 ─────────────────────────────────────────────────────

/**
 * 标准液态玻璃：vibrancy 提艳 + 轻度色彩校正 + 柔化 + lens 折射。
 * 库内建 45° 高光（highlight）默认关闭，边缘光改由 [roundedGlassRim] 等自绘，
 * 方向统一为「左上亮 → 右下暗」，避免两层高光叠加过曝。
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: Backdrop = LocalBackdrop.current,
    shape: () -> Shape,
    blurRadius: Dp = 2.dp,
    refractionHeight: Dp = 14.dp,
    refractionAmount: Dp = 30.dp,
    depthEffect: Boolean = true,
    chromaticAberration: Boolean = false,
    surface: (DrawScope.() -> Unit)? = null
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = shape,
    highlight = null,
    effects = {
        vibrancy()
        colorControls(brightness = 0.04f, contrast = 1.06f)
        blur(blurRadius.toPx())
        lens(
            refractionHeight = refractionHeight.toPx(),
            refractionAmount = refractionAmount.toPx(),
            depthEffect = depthEffect,
            chromaticAberration = chromaticAberration
        )
    },
    onDrawSurface = surface
)

/** 有机形状柔玻璃（无 lens 折射）：vibrancy + 色彩校正 + blur，保留库内建 Ambient 高光。 */
@Composable
fun Modifier.softGlass(
    backdrop: Backdrop = LocalBackdrop.current,
    shape: () -> Shape,
    blurRadius: Dp = 2.dp,
    surface: (DrawScope.() -> Unit)? = null
): Modifier = this.drawBackdrop(
    backdrop = backdrop,
    shape = shape,
    effects = {
        vibrancy()
        colorControls(brightness = 0.05f, contrast = 1.05f)
        blur(blurRadius.toPx())
    },
    onDrawSurface = surface
)

// ── 表面层：圆角玻璃 ──────────────────────────────────────────────────

/**
 * 圆角玻璃三层表面（在 onDrawSurface 中调用）：
 *  1. 基础色调 —— 竖向渐变，顶部比底部略亮（[base] 透明度放大 1.35 倍封顶 1）；
 *  2. 顶部镜面高光 —— 白色 0→30% 高度渐隐，模拟顶面反光；
 *  3. 底部内反射 —— 微弱白光，模拟玻璃下缘的环境光回弹。
 */
fun DrawScope.roundedGlassSurface(
    cornerRadius: Dp,
    base: Color,
    specularAlpha: Float = 0.30f,
    bottomReflectionAlpha: Float = 0.05f
) {
    val h = size.height
    val r = cornerRadius.toPx().coerceIn(0f, min(size.width, size.height) * 0.5f)
    val radius = CornerRadius(r, r)
    // 1. 基础色调
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(base.copy(alpha = (base.alpha * 1.35f).coerceAtMost(1f)), base),
            startY = 0f,
            endY = h
        ),
        cornerRadius = radius
    )
    // 2. 顶部镜面高光
    if (specularAlpha > 0f) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = specularAlpha), Color.Transparent),
                startY = 0f,
                endY = h * 0.30f
            ),
            cornerRadius = radius
        )
    }
    // 3. 底部内反射
    if (bottomReflectionAlpha > 0f) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = bottomReflectionAlpha)),
                startY = h * 0.72f,
                endY = h
            ),
            cornerRadius = radius
        )
    }
}

/** 边缘光描边：左上亮 → 右下暗的三段渐变，线宽内缩一半保证完全落在形状内。 */
fun DrawScope.roundedGlassRim(
    cornerRadius: Dp,
    width: Dp = 1.25.dp,
    alpha: Float = 0.5f
) {
    val w = size.width
    val h = size.height
    val stroke = width.toPx()
    val half = stroke * 0.5f
    val r = (cornerRadius.toPx() - half).coerceIn(0f, min(w, h) * 0.5f)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha),
                Color.White.copy(alpha = alpha * 0.42f),
                Color.White.copy(alpha = alpha * 0.08f)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        ),
        topLeft = Offset(half, half),
        size = Size((w - stroke).coerceAtLeast(stroke), (h - stroke).coerceAtLeast(stroke)),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(width = stroke)
    )
}

// ── 颜色工具 ─────────────────────────────────────────────────────────

/** HSV → Compose Color（Companion 扩展，支持 `Color.hsv(h, s, v)` 调用形态）。 */
fun Color.Companion.hsv(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))

// ── 表面层：圆形玻璃 ──────────────────────────────────────────────────

/** 圆形玻璃基底：左上偏心的径向渐变（受光面更亮）+ 左上→右下镜面高光。 */
fun DrawScope.circleGlassSurface(
    base: Color,
    specularAlpha: Float = 0.32f
) {
    val w = size.width
    val h = size.height
    val d = min(w, h)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(base.copy(alpha = (base.alpha * 1.6f).coerceAtMost(1f)), base),
            center = Offset(w * 0.38f, h * 0.32f),
            radius = d * 0.75f
        )
    )
    if (specularAlpha > 0f) {
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = specularAlpha), Color.Transparent),
                start = Offset(0f, 0f),
                end = Offset(w * 0.85f, h * 0.9f)
            )
        )
    }
}

/** 圆形边缘光描边：左上亮 → 右下暗，半径内缩半个线宽。 */
fun DrawScope.circleGlassRim(
    width: Dp = 1.25.dp,
    alpha: Float = 0.55f
) {
    val w = size.width
    val h = size.height
    val stroke = width.toPx()
    val half = stroke * 0.5f
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha),
                Color.White.copy(alpha = alpha * 0.45f),
                Color.White.copy(alpha = alpha * 0.10f)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        ),
        radius = (min(w, h) * 0.5f - half).coerceAtLeast(stroke),
        center = Offset(w * 0.5f, h * 0.5f),
        style = Stroke(width = stroke)
    )
}
