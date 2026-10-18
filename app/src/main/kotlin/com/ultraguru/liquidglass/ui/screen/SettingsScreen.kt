package com.ultraguru.liquidglass.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ultraguru.liquidglass.GlassLabApp
import com.ultraguru.liquidglass.data.Catalog
import com.ultraguru.liquidglass.data.SettingsStore
import com.ultraguru.liquidglass.data.ThemeMode
import com.ultraguru.liquidglass.plugin.InstallState
import com.ultraguru.liquidglass.plugin.PluginRegistry
import com.ultraguru.liquidglass.update.UpdateCenter
import com.ultraguru.liquidglass.update.AppVersionManifest
import com.ultraguru.liquidglass.ui.components.GlassCard
import com.ultraguru.liquidglass.ui.theme.BrandRed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = GlassLabApp.get()
    val settings by app.settings.state.collectAsState()
    val installed by PluginRegistry.installed.collectAsState()
    val installStates by PluginRegistry.installStates.collectAsState()
    val diagnostics by PluginRegistry.diagnostics.collectAsState()
    val context = LocalContext.current
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    var appUpdate by remember { mutableStateOf<AppVersionManifest?>(null) }
    var appUpdateBusy by remember { mutableStateOf(false) }
    var appUpdateMsg by remember { mutableStateOf<String?>(null) }
    var pluginUpdates by remember { mutableStateOf<Map<String, com.ultraguru.liquidglass.update.PluginRelease>>(emptyMap()) }
    var mirrorDraft by remember(settings.mirrorBase) { mutableStateOf(settings.mirrorBase) }
    var adoptMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 48.dp, bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        // ── 外观 ──
        item {
            Section("外观") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { app.settings.update { it.copy(themeMode = mode) } },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size)
                        ) {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "跟随系统"
                                    ThemeMode.DARK -> "深色"
                                    ThemeMode.LIGHT -> "浅色"
                                }
                            )
                        }
                    }
                }
                SettingSwitch(
                    title = "动态取色",
                    subtitle = "Material You（Android 12+ 壁纸取色）",
                    checked = settings.dynamicColor,
                    onChange = { v -> app.settings.update { it.copy(dynamicColor = v) } }
                )
            }
        }

        // ── 更新 ──
        item {
            Section("更新") {
                SettingSwitch(
                    title = "启动时静默检查更新",
                    subtitle = "仅在发现新版本时于列表页提示",
                    checked = settings.silentUpdateCheck,
                    onChange = { v -> app.settings.update { it.copy(silentUpdateCheck = v) } }
                )
                SettingSwitch(
                    title = "网络加速",
                    subtitle = "GitHub 直连失败时自动经公共加速镜像重试（sha256 校验兑底完整性）",
                    checked = settings.networkBoost,
                    onChange = { v -> app.settings.update { it.copy(networkBoost = v) } }
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                appUpdateBusy = true
                                appUpdateMsg = null
                                runCatching {
                                    UpdateCenter.peekAppUpdate(hostVersionCode(), settings.mirrorBase, settings.networkBoost)
                                }.onSuccess { m ->
                                    appUpdate = m
                                    appUpdateMsg = if (m == null) "已是最新版本" else "发现新版本 v${m.versionName}"
                                }.onFailure {
                                    appUpdateMsg = "检查失败：${it.message}"
                                }
                                appUpdateBusy = false
                            }
                        },
                        enabled = !appUpdateBusy
                    ) {
                        if (appUpdateBusy) {
                            CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp)
                        } else Text("检查 App 更新")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                runCatching { UpdateCenter.findPluginUpdates(settings.mirrorBase, settings.networkBoost) }
                                    .onSuccess { pluginUpdates = it }
                            }
                        }
                    ) { Text("检查插件更新") }
                }
                if (appUpdateMsg != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(appUpdateMsg!!, style = MaterialTheme.typography.bodySmall)
                }
                if (appUpdate != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "远端 v${appUpdate!!.versionName}（code ${appUpdate!!.versionCode}）可用 —— 前往「关于 → 检查更新」执行增量下载",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandRed
                    )
                }
                if (pluginUpdates.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "可更新插件：${pluginUpdates.keys.joinToString("、")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandRed
                    )
                    pluginUpdates.forEach { (id, rel) ->
                        ListItem(
                            headlineContent = { Text(Catalog.byId(id)?.title ?: id) },
                            supportingContent = { Text("本机 → v${rel.versionName}") },
                            trailingContent = {
                                val st = installStates[id]
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        UpdateCenter.updatePlugin(
                                            context = context,
                                            pluginId = id,
                                            mirrorBase = settings.mirrorBase,
                                            hostVersionCode = hostVersionCode(),
                                            onProgress = { PluginRegistry.setInstallState(id, it) },
                                            boost = settings.networkBoost
                                        )
                                    }
                                }, enabled = st !is InstallState.Downloading && st !is InstallState.Installing) {
                                    when (st) {
                                        is InstallState.Downloading -> Text("${st.percent}%")
                                        is InstallState.Installing -> Text("${st.percent}%")
                                        else -> Text("更新")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // ── 镜像源 ──
        item {
            Section("镜像源") {
                OutlinedTextField(
                    value = mirrorDraft,
                    onValueChange = { mirrorDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("清单基地址（version.json / plugins.json / patches.json）") },
                    supportingText = { Text("留空恢复官方源。资产 URL 会按此基地址解析相对路径。") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        app.settings.update { it.copy(mirrorBase = mirrorDraft.trim().ifBlank { SettingsStore.DEFAULT_MIRROR }) }
                    }) { Text("保存") }
                    OutlinedButton(onClick = {
                        mirrorDraft = SettingsStore.DEFAULT_MIRROR
                        app.settings.update { it.copy(mirrorBase = SettingsStore.DEFAULT_MIRROR) }
                    }) { Text("恢复官方源") }
                }
                if (settings.isCustomMirror()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "当前：自定义镜像（${settings.mirrorBase.take(48)}…）",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandRed
                    )
                }
            }
        }

        // ── 安全 ──
        item {
            Section("安全") {
                SettingSwitch(
                    title = "插件签名校验",
                    subtitle = "严格模式：插件 APK 签名须与宿主同源（自编译包关闭）",
                    checked = settings.verifyPluginSignature,
                    onChange = { v -> app.settings.update { it.copy(verifyPluginSignature = v) } }
                )
            }
        }

        // ── 插件管理 ──
        item {
            Section("插件管理（${installed.size}/5）") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        adoptMsg = "扫描中…"
                        GlassLabApp.get().adoptSystemPluginsNow { n ->
                            scope.launch { adoptMsg = if (n > 0) "已导入 $n 个系统已装插件" else "未发现新的系统已装插件" }
                        }
                    }) { Text("导入系统已装插件") }
                }
                if (adoptMsg != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(adoptMsg!!, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "从 Release 手动下载并系统安装的 plugin-*.apk，点上面的按钮（或回到 App 时自动）导入。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                if (installed.isEmpty()) {
                    Text(
                        "尚未安装任何插件 —— 回到列表逐库点「安装插件」",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                installed.forEach { (id, info) ->
                    ListItem(
                        headlineContent = { Text(Catalog.byId(id)?.title ?: id) },
                        supportingContent = { Text("v${info.versionName} · ${info.sha256.take(12)}…") },
                        trailingContent = {
                            OutlinedButton(onClick = { PluginRegistry.uninstall(id) }) { Text("卸载") }
                        }
                    )
                }
            }
        }

        // ── 诊断 ──
        item {
            Section("诊断日志") {
                val rom = "Android ${android.os.Build.VERSION.RELEASE}（API ${android.os.Build.VERSION.SDK_INT}）"
                val abi = android.os.Build.SUPPORTED_ABIS.joinToString("/")
                Text("设备：${android.os.Build.MODEL} · $rom · $abi", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.height(180.dp)) {
                    items(diagnostics.size) { i ->
                        Text(
                            diagnostics[diagnostics.size - 1 - i],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        elevation = 12.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun hostVersionCode(): Int =
    runCatching {
        val ctx = GlassLabApp.get()
        @Suppress("DEPRECATION")
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode
    }.getOrDefault(1)
