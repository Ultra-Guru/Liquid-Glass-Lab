package com.ultraguru.liquidglass.plugin.cloudy.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.cloudy.CloudyProgressive
import com.skydoves.cloudy.cloudy

/**
 * 场景 1（Cloudy）：深色栏 UI。
 *
 * - 背景源挂 `Modifier.sky(sky)`（渐变 + 滚动卡片一起被录制）；
 * - 顶部玻璃栏 `cloudy(sky, progressive = TopToBottom)`：靠屏幕外一侧全模糊、
 *   向内渐进淡出（iOS 26 风格）；
 * - 底部导航 `cloudy(sky, progressive = BottomToTop)`：同理反向；
 * - 内容滚动时从玻璃后流过，模糊实时跟踪（SkyFrameDriver）。
 */
@Composable
fun DarkBarScene() {
    DemoSkyLayer(
        source = {
            RichGradient()
            ScrollingCards()
        }
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            // ── 顶部玻璃栏（渐进模糊：顶部最实，向下淡出） ──
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .cloudy(
                        sky = LocalSky.current,
                        radius = 24,
                        progressive = CloudyProgressive.TopToBottom(),
                        tint = Color.Black.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(28.dp)
                    )
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

            // ── 底部玻璃导航（渐进模糊：底部最实，向上淡出） ──
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .height(68.dp)
                    .cloudy(
                        sky = LocalSky.current,
                        radius = 24,
                        progressive = CloudyProgressive.BottomToTop(),
                        tint = Color.Black.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(30.dp)
                    )
            ) {
                var selected by remember { mutableStateOf(0) }
                Row(
                    Modifier.fillMaxSize(),
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

/** 被玻璃覆盖的滚动内容（放进 sky 源里，卡片会真实地从玻璃后流过）。 */
@Composable
private fun ScrollingCards() {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 112.dp, bottom = 132.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "内容在玻璃下方滚动\n注意顶栏 / 底栏的渐进模糊随内容实时变化",
                color = Color(0xCCFFFFFF),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
        itemsIndexed(List(16) { it }) { i, _ ->
            ContentCard(i)
        }
    }
}

@Composable
private fun ContentCard(i: Int) {
    val hue = (i * 37) % 360
    val hsv = floatArrayOf(hue.toFloat(), 0.42f, 0.30f)
    val color = Color(android.graphics.Color.HSVToColor(hsv))
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(color.copy(alpha = 0.85f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text("滚动卡片 #$i", color = Color(0xE6FFFFFF), fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
