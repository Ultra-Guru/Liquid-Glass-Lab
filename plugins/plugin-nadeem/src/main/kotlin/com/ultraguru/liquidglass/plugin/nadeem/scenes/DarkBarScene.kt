package com.ultraguru.liquidglass.plugin.nadeem.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nadeemiqbal.liquidglass.GlassCard
import io.github.nadeemiqbal.liquidglass.GlassNavBar
import io.github.nadeemiqbal.liquidglass.liquidGlassSource
import io.github.nadeemiqbal.liquidglass.rememberLiquidGlassState

/**
 * 场景 1：深色栏 UI（nadeem / liquid-glass）。
 *
 * GlassNavBar（顶部，对滚动内容做背景模糊）+ 底部 GlassCard 胶囊导航。
 * 背景源 = liquidGlassSource 挂在「渐变 + 滚动列表」整个 Box 上：
 * 列表滚动时 GraphicsLayer 逐帧重录，玻璃条的模糊 / 饱和度提升 /
 * 折射随内容实时更新（库的采样模型：source 与玻璃共享同一 state）。
 */
@Composable
internal fun DarkBarScene() {
    val state = rememberLiquidGlassState()
    var selected by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        // ── 背景源：渐变 + 网格 + 光斑 + 滚动卡片（一起录进采样层） ──
        Box(
            Modifier
                .fillMaxSize()
                .liquidGlassSource(state)
        ) {
            DarkBackdropCanvas(Modifier.fillMaxSize())

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 92.dp, start = 20.dp, end = 20.dp, bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "内容在玻璃下方滚动\n注意顶栏与底部导航的模糊 / 折射随内容实时变化",
                        color = Color(0xCCFFFFFF),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
                items(16) { i ->
                    ContentCard(i)
                }
            }
        }

        // ── 顶部玻璃栏（GlassNavBar） ──
        GlassNavBar(
            state = state,
            windowInsets = WindowInsets(0, 0, 0, 0), // 宿主工具栏已避开状态栏
            tint = DemoGlassTint,
            blurRadius = 18.dp,
            saturation = 1.5f,
            refraction = 0.35f,
            grain = 0.03f
        ) {
            Text(
                "Liquid Glass",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
            Spacer(Modifier.weight(1f))
            Text("33%", color = Color(0xB3FFFFFF), fontSize = 12.sp)
        }

        // ── 底部胶囊导航（GlassCard） ──
        GlassCard(
            state = state,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            tint = DemoGlassTint,
            blurRadius = 16.dp,
            refraction = 0.4f,
            grain = 0.03f,
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val tabs = listOf(
                    Icons.Filled.Home to "首页",
                    Icons.Filled.Search to "发现",
                    Icons.Filled.Star to "收藏",
                    Icons.Filled.Person to "我的"
                )
                tabs.forEachIndexed { i, (icon, label) ->
                    NavItem(icon, label, active = selected == i) { selected = i }
                }
            }
        }
    }
}

/** 底部导航单项：图标 + 小字，选中提亮。 */
@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val tint = if (active) Color.White else Color(0x80FFFFFF)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = tint, fontSize = 10.sp)
    }
}

/** 滚动内容卡片：彩色圆角块 + 文字。 */
@Composable
private fun ContentCard(i: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(hsvColor((i * 37f) % 360f, 0.42f, 0.32f))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            "滚动卡片 #$i",
            color = Color(0xE6FFFFFF),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** HSV → Compose Color。 */
internal fun hsvColor(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
