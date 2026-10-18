package com.ultraguru.liquidglass.plugin.kyant0.scenes

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

/**
 * 场景 1：深色栏 UI。
 * 顶栏 + 底部导航栏玻璃化（vibrancy + blur + lens 折射 + 轻度色散），
 * 内容滚动时从玻璃后流过，可直观看到折射随内容移动。
 * 底栏选中项带弹性滑动的玻璃胶囊（透镜中的透镜）。
 */
@Composable
fun DarkBarScene() {
    DemoBackdropLayer { backdrop ->
        Box(Modifier.fillMaxSize()) {
            // ── 滚动内容（被玻璃覆盖）──
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    top = 96.dp, bottom = 120.dp, start = 20.dp, end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "内容在玻璃下方滚动\n注意顶栏 / 底栏的折射与色散随内容实时变化",
                        color = Color(0xCCFFFFFF), fontSize = 13.sp, lineHeight = 19.sp
                    )
                }
                itemsIndexed(List(16) { it }) { i, _ ->
                    ContentCard(i)
                }
            }

            // ── 顶部玻璃栏 ──
            TopGlassBar(
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC24B), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Liquid Glass", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Spacer(Modifier.weight(1f))
                    Text("33%", color = Color(0xB3FFFFFF), fontSize = 12.sp)
                    Spacer(Modifier.width(14.dp))
                    Icon(Icons.Filled.Search, null, tint = Color(0xE6FFFFFF), modifier = Modifier.size(20.dp))
                }
            }

            // ── 底部玻璃导航 ──
            GlassNav(
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .height(68.dp)
            )
        }
    }
}

/** 顶部玻璃栏：连续曲率方形（squircle）+ 三层表面 + 边缘光。 */
@Composable
private fun TopGlassBar(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier.liquidGlass(
            backdrop = backdrop,
            shape = { RoundedRectangle(28.dp) },
            blurRadius = 5.dp,
            refractionHeight = 14.dp,
            refractionAmount = 30.dp,
            chromaticAberration = true,
            surface = {
                roundedGlassSurface(28.dp, Color.Black.copy(alpha = 0.20f), specularAlpha = 0.32f)
                roundedGlassRim(28.dp, width = 1.25.dp, alpha = 0.5f)
            }
        )
    ) {
        content()
    }
}

/** 底部玻璃导航：选中项后面是一颗弹性滑动的玻璃胶囊。 */
@Composable
private fun GlassNav(
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(0) }
    Box(
        modifier.liquidGlass(
            backdrop = backdrop,
            shape = { RoundedRectangle(30.dp) },
            blurRadius = 5.dp,
            refractionHeight = 14.dp,
            refractionAmount = 30.dp,
            chromaticAberration = true,
            surface = {
                roundedGlassSurface(30.dp, Color.Black.copy(alpha = 0.20f), specularAlpha = 0.32f)
                roundedGlassRim(30.dp, width = 1.25.dp, alpha = 0.5f)
            }
        )
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val slot = (maxWidth - 8.dp) / 4f
            val slide by animateDpAsState(
                targetValue = slot * selected.toFloat(),
                animationSpec = spring(dampingRatio = 0.68f, stiffness = 520f),
                label = "navSlide"
            )
            // 选中胶囊 —— 玻璃上的透镜（更小折射半径，读出「放大镜」层次感）
            Box(
                Modifier
                    .offset(x = 4.dp + slide)
                    .width(slot - 8.dp)
                    .height(50.dp)
                    .align(Alignment.CenterStart)
                    .liquidGlass(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        blurRadius = 2.dp,
                        refractionHeight = 10.dp,
                        refractionAmount = 22.dp,
                        surface = {
                            val radius = (size.minDimension * 0.5f).toDp()
                            roundedGlassSurface(
                                radius,
                                Color.White.copy(alpha = 0.12f),
                                specularAlpha = 0.38f,
                                bottomReflectionAlpha = 0.04f
                            )
                            roundedGlassRim(radius, width = 1.dp, alpha = 0.55f)
                        }
                    )
            )
            Row(Modifier.fillMaxSize()) {
                val items = listOf(
                    Icons.Filled.Home to "首页",
                    Icons.Filled.Search to "发现",
                    Icons.Filled.Star to "收藏",
                    Icons.Filled.Person to "我的"
                )
                items.forEachIndexed { i, (icon, label) ->
                    val active = selected == i
                    val tint = if (active) Color.White else Color(0x80FFFFFF)
                    val iconScale by animateFloatAsState(
                        targetValue = if (active) 1.14f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
                        label = "navIcon$i"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selected = i }
                    ) {
                        Icon(
                            icon, null,
                            tint = tint,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(label, color = tint, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ── 滚动内容卡片：高对比 + 多色彩，让玻璃的折射/提艳「读得出来」 ──────────

@Composable
private fun ContentCard(i: Int) {
    when (i % 4) {
        0 -> HueCard(i)
        1 -> StatCard()
        2 -> PhotoCard()
        else -> SlimCard(i)
    }
}

@Composable
private fun CardShell(
    height: Dp,
    brush: Brush,
    content: @Composable BoxScope.() -> Unit
) {
    val shape: Shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(brush, shape)
            .border(0.6.dp, Color.White.copy(alpha = 0.10f), shape),
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}

/** 彩色渐变卡片：色彩样本从玻璃后流过，vibrancy 提艳明显。 */
@Composable
private fun HueCard(i: Int) {
    val hue = (i * 47f) % 360f
    CardShell(
        height = 72.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.hsv(hue, 0.58f, 0.36f).copy(alpha = 0.94f),
                Color.hsv((hue + 28f) % 360f, 0.62f, 0.22f).copy(alpha = 0.94f)
            )
        )
    ) {
        Text(
            "滚动卡片 #$i",
            color = Color(0xF2FFFFFF), fontSize = 15.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
    }
}

/** 大数字卡片：高亮暖色渐变 + 粗白字，玻璃边缘扫过时折射最明显。 */
@Composable
private fun StatCard() {
    CardShell(
        height = 88.dp,
        brush = Brush.linearGradient(listOf(Color(0xE6FF8A3D), Color(0xE6FF3D81)))
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("98%", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("今日进度 · 高对比内容", color = Color(0xCCFFFFFF), fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .width(64.dp)
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.30f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.98f)
                        .height(6.dp)
                        .background(Color.White, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

/** 照片式卡片：青→蓝→紫的照片感渐变 + 亮白样本块。 */
@Composable
private fun PhotoCard() {
    CardShell(
        height = 88.dp,
        brush = Brush.linearGradient(listOf(Color(0xE62EC4B6), Color(0xE63E7CB1), Color(0xE67C5CBF)))
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(17.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text("风景 · 明亮样本", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("给玻璃折射提供高对比光源", color = Color(0xCCFFFFFF), fontSize = 12.sp)
            }
        }
    }
}

/** 窄色条卡片：紧凑节奏。 */
@Composable
private fun SlimCard(i: Int) {
    val hue = (i * 83f) % 360f
    CardShell(
        height = 64.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.hsv(hue, 0.50f, 0.30f).copy(alpha = 0.90f),
                Color.hsv((hue + 40f) % 360f, 0.55f, 0.18f).copy(alpha = 0.90f)
            )
        )
    ) {
        Text(
            "色彩样本 #$i",
            color = Color(0xE6FFFFFF), fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
    }
}
