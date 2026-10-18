package com.ultraguru.liquidglass.plugin.kyant0.scenes

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import kotlin.math.tanh

/**
 * 场景 1：深色栏 UI。
 * 顶栏 + 底部导航栏玻璃化（vibrancy + blur + lens 折射），
 * 内容滚动时从玻璃后流过，可直观看到折射随内容移动。
 */
@Composable
fun DarkBarScene() {
    DemoBackdropLayer {
        backdrop ->
        Box(Modifier.fillMaxSize()) {
            // ── 滚动内容（被玻璃覆盖） ──
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 96.dp, bottom = 120.dp, start = 20.dp, end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "内容在玻璃下方滚动\n注意顶栏 / 底栏的折射随内容实时变化",
                        color = Color(0xCCFFFFFF), fontSize = 13.sp, lineHeight = 19.sp
                    )
                }
                itemsIndexed(List(16) { it }) { i, _ ->
                    ContentCard(i)
                }
            }

            // ── 顶部玻璃栏 ──
            GlassBar(
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(64.dp)
            ) { padding ->
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
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
            GlassBar(
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .height(68.dp)
            ) { padding ->
                var selected by remember { mutableStateOf(0) }
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val items = listOf(
                        Icons.Filled.Home to "首页",
                        Icons.Filled.Search to "发现",
                        Icons.Filled.Star to "收藏",
                        Icons.Filled.Person to "我的"
                    )
                    items.forEachIndexed { i, (icon, label) ->
                        val active = selected == i
                        val tint = if (active) Color.White else Color(0x80FFFFFF)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selected = i }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(3.dp))
                            Text(label, color = tint, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

/** 玻璃栏容器：统一 effects 配方（vibrancy + blur + lens）。 */
@Composable
private fun GlassBar(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable (padding: Dp) -> Unit
) {
    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedCornerShape(28.dp) },
            effects = {
                vibrancy()
                blur(6.dp.toPx())
                lens(16.dp.toPx(), 32.dp.toPx())
            },
            onDrawSurface = { drawRect(GlassSurfaceTint) }
        )
    ) {
        content(0.dp)
    }
}

@Composable
private fun ContentCard(i: Int) {
    val hue = (i * 37) % 360
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(
                Color.hsv(hue.toFloat(), 0.42f, 0.30f).copy(alpha = 0.85f),
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text("滚动卡片 #$i", color = Color(0xE6FFFFFF), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

private fun Color.hsv(h: Float, s: Float, v: Float) = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
