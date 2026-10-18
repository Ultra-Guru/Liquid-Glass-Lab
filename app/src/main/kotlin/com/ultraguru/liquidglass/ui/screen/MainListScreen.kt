package com.ultraguru.liquidglass.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ultraguru.liquidglass.GlassLabApp
import com.ultraguru.liquidglass.data.Catalog
import com.ultraguru.liquidglass.plugin.InstallState
import com.ultraguru.liquidglass.plugin.PluginRegistry
import com.ultraguru.liquidglass.ui.components.GlassCard
import com.ultraguru.liquidglass.ui.components.NavRow
import com.ultraguru.liquidglass.ui.components.StarsBadge
import com.ultraguru.liquidglass.ui.components.StatusChip
import com.ultraguru.liquidglass.ui.theme.BrandRed
import com.ultraguru.liquidglass.ui.theme.BrandRedDeep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 主界面（用户指定结构）：
 * ① 五大液态玻璃库列表（kyant0 第一）
 * ② 倒数第二「关于」
 * ③ 最后「设置」
 *
 * v2 精致化：磨砂玻璃卡片 + 液体极光背景（AppRoot 层），透明叠加。
 */
@Composable
fun MainListScreen(
    onOpenLibrary: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val installed by PluginRegistry.installed.collectAsState()
    val installStates by PluginRegistry.installStates.collectAsState()
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
    }

    // 进入主列表即尝试导入系统已装插件（用户从 Release 手动装的插件 APK）
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching { PluginRegistry.adoptSystemInstalledPlugins() }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 48.dp, bottom = 32.dp
        )
    ) {
        // 头部标题（渐变玻璃字）
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp)) {
                Text(
                    "液态玻璃实验室",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        brush = Brush.horizontalGradient(listOf(BrandRed, Color(0xFFFF9A8E)))
                    )
                )
                Text(
                    "Liquid Glass Lab · v"
                        + versionName
                        + if (context.packageName.endsWith(".debug")) " (debug)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "五大顶级开源液态玻璃库 · 插件化隔离集成 · 增量更新",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 五大库
        items(Catalog.libraries, key = { it.id }) { entry ->
            LibraryCard(
                order = Catalog.libraries.indexOf(entry) + 1,
                title = entry.title,
                subtitle = entry.subtitle,
                stars = entry.stars,
                license = entry.license,
                installState = installStates[entry.id],
                installed = installed[entry.id] != null,
                onClick = { onOpenLibrary(entry.id) }
            )
        }

        // 倒数第二：关于
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(8.dp))
            NavRow(
                icon = Icons.Outlined.Info,
                title = "关于",
                subtitle = "版本信息 · 开源致谢 · 更新日志",
                onClick = onOpenAbout
            )
        }

        // 最后：设置
        item(span = { GridItemSpan(maxLineSpan) }) {
            NavRow(
                icon = Icons.Outlined.Settings,
                title = "设置",
                subtitle = "主题 · 检查更新 · 插件管理 · 镜像源 · 诊断",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun LibraryCard(
    order: Int,
    title: String,
    subtitle: String,
    stars: Int,
    license: String,
    installed: Boolean,
    installState: InstallState?,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        elevation = 14.dp,
        onClick = onClick
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            Brush.linearGradient(
                                if (installed) listOf(BrandRedDeep, BrandRed)
                                else listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$order",
                        color = if (installed) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StarsBadge(stars)
                StatusChip(license)
                val stateChip = when (installState) {
                    is InstallState.Downloading -> StatusChip("下载中 ${installState.percent}%", accent = true)
                    is InstallState.Installing -> StatusChip("安装中 ${installState.percent}%", accent = true)
                    is InstallState.Verifying -> StatusChip("校验中", accent = true)
                    is InstallState.Failed -> StatusChip("安装失败")
                    is InstallState.Done -> StatusChip("已装 v${installState.versionName}", accent = true)
                    InstallState.Idle -> null
                    null -> null
                }
                stateChip ?: when {
                    installed -> StatusChip("已安装", accent = true)
                    else -> StatusChip("未安装")
                }
            }
        }
    }
}
