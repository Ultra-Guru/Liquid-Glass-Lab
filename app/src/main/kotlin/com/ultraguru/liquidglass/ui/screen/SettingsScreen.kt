package com.ultraguru.liquidglass.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraguru.liquidglass.GlassLabApp
import com.ultraguru.liquidglass.data.Catalog
import com.ultraguru.liquidglass.data.MirrorPresets
import com.ultraguru.liquidglass.data.SettingsStore
import com.ultraguru.liquidglass.data.ThemeMode
import com.ultraguru.liquidglass.plugin.PluginRegistry
import com.ultraguru.liquidglass.ui.components.GlassCard
import com.ultraguru.liquidglass.ui.components.StatusChip
import com.ultraguru.liquidglass.ui.theme.BrandRed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenUpdates: () -> Unit = {}
) {
    val app = GlassLabApp.get()
    val settings by app.settings.state.collectAsState()
    val installed by PluginRegistry.installed.collectAsState()
    val diagnostics by PluginRegistry.diagnostics.collectAsState()
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    var adoptMsg by remember { mutableStateOf<String?>(null) }

    // 镜像测速状态：预设 id → 延迟结果（null = 未测 / 测试中由 testing 标记）
    var speedResults by remember { mutableStateOf<Map<String, MirrorPresets.SpeedResult>>(emptyMap()) }
    var testing by remember { mutableStateOf(false) }
    var customDraft by remember(settings.mirrorBase) {
        mutableStateOf(
            MirrorPresets.match(settings.mirrorBase)?.let { "" } ?: settings.mirrorBase
        )
    }

    fun runSpeedTest() {
        scope.launch {
            testing = true
            speedResults = emptyMap()
            val results = MirrorPresets.presets.map { preset ->
                async { MirrorPresets.speedTest(preset) }
            }.awaitAll()
            speedResults = results.associateBy { it.preset.id }
            testing = false
        }
    }

    fun applyMirror(base: String) {
        app.settings.update { it.copy(mirrorBase = base.trim().ifBlank { SettingsStore.DEFAULT_MIRROR }) }
    }

    fun applyFastest() {
        val best = speedResults.values
            .filter { it.latencyMs != null }
            .minByOrNull { it.latencyMs!! } ?: return
        applyMirror(best.preset.base)
    }

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
                    subtitle = "仅在发现新版本 / 插件可更新时于列表页横幅提示",
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
                Button(
                    onClick = onOpenUpdates,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("打开更新中心（App 增量升级 · 插件一键更新）")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "App 更新优先走 vcdiff 增量补丁（下载量约为全量 1/6）；插件安装/更新不经系统安装器，装完即生效。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── 镜像源 ──
        item {
            Section("镜像源") {
                val currentPreset = MirrorPresets.match(settings.mirrorBase)
                // 预设单选列表（含测速结果展示）
                MirrorPresets.presets.forEach { preset ->
                    val result = speedResults[preset.id]
                    val selected = currentPreset?.id == preset.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { applyMirror(preset.base) }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(preset.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                preset.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        when {
                            testing && result == null -> {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            }
                            result != null && result.latencyMs != null -> {
                                StatusChip(
                                    "${result.latencyMs}ms" + (result.versionName?.let { " · v$it" } ?: ""),
                                    accent = true
                                )
                            }
                            result != null -> {
                                Text(
                                    "超时",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { runSpeedTest() }, enabled = !testing) {
                        if (testing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(6.dp))
                        } else {
                            Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                        }
                        Text("测速")
                    }
                    OutlinedButton(
                        onClick = { applyFastest() },
                        enabled = !testing && speedResults.values.any { it.latencyMs != null }
                    ) { Text("用最快") }
                }
                Spacer(Modifier.height(10.dp))
                // 自定义镜像输入（全镜像：清单与资产同源）
                Text(
                    "自定义镜像（全镜像，清单与资产同源）",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = customDraft,
                    onValueChange = { customDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("清单基地址（指向仓库根，以 / 结尾）") },
                    supportingText = {
                        Text("例：https://your-proxy.example.com/Ultra-Guru/Liquid-Glass-Lab/（留空恢复官方源）")
                    },
                    singleLine = true
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        applyMirror(customDraft)
                    }) { Text("保存自定义") }
                    OutlinedButton(onClick = {
                        customDraft = ""
                        applyMirror(SettingsStore.DEFAULT_MIRROR)
                    }) { Text("恢复官方源") }
                }
                if (settings.isCustomMirror()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "当前生效：${settings.mirrorBase}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandRed
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "预设镜像只镜像清单（json）；APK / 补丁资产仍由 GitHub Release 端点提供（叠加网络加速）。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
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
                    OutlinedButton(onClick = onOpenUpdates) { Text("去更新中心安装/更新") }
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
