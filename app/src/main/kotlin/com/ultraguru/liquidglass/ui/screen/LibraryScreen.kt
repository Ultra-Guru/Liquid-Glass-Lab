package com.ultraguru.liquidglass.ui.screen

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ultraguru.liquidglass.data.Catalog
import com.ultraguru.liquidglass.data.SettingsStore
import com.ultraguru.liquidglass.plugin.InstallState
import com.ultraguru.liquidglass.plugin.PluginRegistry
import com.ultraguru.liquidglass.update.UpdateCenter
import com.ultraguru.liquidglass.ui.components.GlassCard
import com.ultraguru.liquidglass.ui.components.StarsBadge
import com.ultraguru.liquidglass.ui.components.StatusChip
import com.ultraguru.liquidglass.ui.theme.BrandRed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 库详情页：未安装 → 显示「安装」；安装完成 → 自动展示四大演示场景 + 「启动」直达按钮。
 *
 * v2 修复链：
 *  - 安装走 UpdateCenter（资产 URL 已回落 github.com 规范源 + 加速通道）
 *  - 进入页面即尝试导入系统已装插件（用户手动 sideload 的 plugin-*.apk）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    entryId: String,
    onBack: () -> Unit,
    onOpenScene: (pluginId: String, sceneId: String, title: String) -> Unit
) {
    val entry = Catalog.byId(entryId) ?: run { onBack(); return }
    val context = LocalContext.current
    val installed by PluginRegistry.installed.collectAsState()
    val installStates by PluginRegistry.installStates.collectAsState()
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    var showUninstallConfirm by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // ① 系统已装插件导入（用户从 Release 手动安装的 plugin APK 立即识别）
    // ② 场景列表刷新（安装完成 / 导入成功后 installed 变化驱动 remember 重算）
    LaunchedEffect(entry.id) {
        withContext(Dispatchers.IO) {
            runCatching { PluginRegistry.adoptSystemInstalledPlugins() }
        }
    }

    val info = installed[entry.id]
    val state = installStates[entry.id]

    val pluginScenes = remember(info, loadError) {
        if (info != null) {
            runCatching { PluginRegistry.load(entry.id) }
                .map { it.scenes }
                .onFailure { loadError = it.message }
                .getOrNull()
        } else null
    }

    fun startInstall() {
        scope.launch {
            val settings = currentSettings()
            val err = UpdateCenter.updatePlugin(
                context = context,
                pluginId = entry.id,
                mirrorBase = settings.mirrorBase,
                hostVersionCode = hostVersionCode(),
                onProgress = { PluginRegistry.setInstallState(entry.id, it) },
                boost = settings.networkBoost
            )
            if (err != null) {
                PluginRegistry.setInstallState(entry.id, InstallState.Failed(err))
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 48.dp, bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // 顶部栏
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    "库详情",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            // 头部信息（玻璃卡）
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                Column(Modifier.padding(20.dp)) {
                    Text(entry.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        entry.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StarsBadge(entry.stars)
                        StatusChip(entry.license)
                        StatusChip("by ${entry.author}")
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(entry.description, style = MaterialTheme.typography.bodyMedium, lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "坐标：${entry.coordinate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(entry.repoUrl))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }) {
                        Text("打开 GitHub 仓库 ↗", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        item {
            // 安装 / 状态卡（玻璃卡）
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                elevation = 16.dp
            ) {
                Column(Modifier.padding(20.dp)) {
                    when {
                        state is InstallState.Downloading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.size(12.dp))
                                Text("下载中… ${state.percent}%", style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { state.percent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        state is InstallState.Installing -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.size(12.dp))
                                Text("安装中… ${state.percent}%", style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        state is InstallState.Verifying -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.size(12.dp))
                                Text("sha256 校验中…", style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        info == null -> {
                            Text(
                                "该插件未安装",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "插件 APK 由 GitHub Release 分发，安装后本地隔离加载。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { startInstall() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text(if (state is InstallState.Failed) "重试安装" else "安装插件")
                            }
                            if (state is InstallState.Failed) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "失败：${state.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF4CFF9B), CircleShape)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    "已安装 v${info.versionName}（code ${info.versionCode}）",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            if (!info.signatureTrusted) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "⚠ 签名与宿主不同源（自编译包）——功能不受影响",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            // 启动直达按钮：装完即可一键进入演示 UI
                            if (!pluginScenes.isNullOrEmpty()) {
                                Button(
                                    onClick = {
                                        val first = pluginScenes.first()
                                        onOpenScene(entry.id, first.id, first.title)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text("启动演示")
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { startInstall() }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("检查更新")
                                }
                                OutlinedButton(onClick = { showUninstallConfirm = true }) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("卸载")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 装完自动展示四项场景
        if (pluginScenes != null) {
            item {
                Text(
                    "演示场景（${entry.title}）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
            items(pluginScenes.size) { idx ->
                val scene = pluginScenes[idx]
                GlassCard(
                    cornerRadius = 20.dp,
                    elevation = 10.dp,
                    onClick = { onOpenScene(entry.id, scene.id, scene.title) }
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .background(BrandRed.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${idx + 1}",
                                    color = BrandRed,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        },
                        headlineContent = { Text(scene.title, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(scene.subtitle) }
                    )
                }
            }
        } else if (info != null && loadError != null) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "插件加载失败",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            loadError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // 未安装时预告四大场景
            item {
                Text(
                    "安装后可用（四大对比场景）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
            items(4) { idx ->
                val titles = listOf(
                    "深色栏 UI" to "Dark bar · 顶栏 + 底部导航玻璃化",
                    "按钮 UI" to "Buttons · 按钮矩阵 + 按压反馈",
                    "液体" to "Liquid · 可拖拽弹性液滴",
                    "生物拟态" to "Biomorphic · 有机形态呼吸群落"
                )
                GlassCard(cornerRadius = 20.dp, elevation = 8.dp) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(titles[idx].first, fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            Text("${titles[idx].second}\n预告：${entry.sceneNotes.values.toList().getOrNull(idx) ?: ""}")
                        },
                        leadingContent = {
                            Text(
                                "${idx + 1}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }
        }
    }

    if (showUninstallConfirm) {
        AlertDialog(
            onDismissRequest = { showUninstallConfirm = false },
            title = { Text("卸载插件？") },
            text = { Text("将移除 ${entry.title} 的插件 APK 与运行时数据。") },
            confirmButton = {
                TextButton(onClick = {
                    PluginRegistry.uninstall(entry.id)
                    loadError = null
                    showUninstallConfirm = false
                }) { Text("卸载", color = BrandRed) }
            },
            dismissButton = { TextButton(onClick = { showUninstallConfirm = false }) { Text("取消") } }
        )
    }
}

private fun hostVersionCode(): Int =
    runCatching {
        val ctx = com.ultraguru.liquidglass.GlassLabApp.get()
        @Suppress("DEPRECATION")
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode
    }.getOrDefault(1)

private fun currentSettings(): com.ultraguru.liquidglass.data.SettingsStore.Settings =
    com.ultraguru.liquidglass.GlassLabApp.get().settings.state.value
