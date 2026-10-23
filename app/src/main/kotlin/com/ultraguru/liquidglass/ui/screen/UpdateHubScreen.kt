package com.ultraguru.liquidglass.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.sp
import com.ultraguru.liquidglass.GlassLabApp
import com.ultraguru.liquidglass.data.Catalog
import com.ultraguru.liquidglass.data.MirrorPresets
import com.ultraguru.liquidglass.plugin.InstallState
import com.ultraguru.liquidglass.plugin.PluginRegistry
import com.ultraguru.liquidglass.update.ChangelogEntry
import com.ultraguru.liquidglass.update.PreparedUpdate
import com.ultraguru.liquidglass.update.UpdateCenter
import com.ultraguru.liquidglass.update.UpdateOverview
import com.ultraguru.liquidglass.ui.components.GlassCard
import com.ultraguru.liquidglass.ui.components.StatusChip
import com.ultraguru.liquidglass.ui.theme.BrandRed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 更新中心（v0.1.3）：App 自身 + 五插件的一站式更新入口。
 *
 * - 进入即自动检查（version.json + plugins.json 双清单并发拉取）
 * - App 更新三段链路：增量补丁 → 窗口补丁 → 全量 APK（全自动选路），
 *   进度实时展示（下载 % / vcdiff 合成 % / 拉起系统安装器）
 * - 插件安装与更新同链路（未安装 = 首次安装；已装 = 增量/全量更新），逐个或一键全部
 * - 顶部显示当前清单源；点击「更换」跳设置页镜像区（预设 + 测速）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateHubScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val app = GlassLabApp.get()
    val settings by app.settings.state.collectAsState()
    val installed by PluginRegistry.installed.collectAsState()
    val installStates by PluginRegistry.installStates.collectAsState()
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    var overview by remember { mutableStateOf<UpdateOverview?>(null) }
    var checking by remember { mutableStateOf(false) }
    var checkMsg by remember { mutableStateOf<String?>(null) }
    var appState by remember { mutableStateOf<UpdateCenter.AppState>(UpdateCenter.AppState.Idle) }
    var prepared by remember { mutableStateOf<PreparedUpdate?>(null) }
    var remoteChangelog by remember { mutableStateOf<List<ChangelogEntry>>(emptyList()) }
    var updatingAll by remember { mutableStateOf(false) }

    val hostCode = remember { GlassLabApp.hostVersionCode() }
    val hostName = remember { GlassLabApp.hostVersionName() }

    // 与本机安装表实时比对的「需安装/更新」集合（装完一个即自动收敛）
    val pluginUpdates = remember(overview, installed) {
        overview?.pluginUpdatesAgainst(installed) ?: emptyMap()
    }

    fun refresh(silent: Boolean = false) {
        scope.launch {
            checking = true
            if (!silent) checkMsg = null
            appState = UpdateCenter.AppState.Idle
            runCatching {
                UpdateCenter.fetchOverview(hostCode, settings.mirrorBase, settings.networkBoost)
            }.onSuccess {
                overview = it
                // 远端更新要点（可失败，不阻塞主链）
                runCatching {
                    UpdateCenter.fetchChangelog(settings.mirrorBase, settings.networkBoost)
                }.onSuccess { list -> remoteChangelog = list }
            }.onFailure {
                if (!silent) checkMsg = "检查失败：${it.message}"
            }
            checking = false
        }
    }

    // 进入即查 + 导入系统已装插件（与主列表一致）
    LaunchedEffect(Unit) {
        refresh()
        withContext(Dispatchers.IO) {
            runCatching { PluginRegistry.adoptSystemInstalledPlugins() }
        }
    }

    fun startAppUpdate() {
        scope.launch {
            appState = UpdateCenter.AppState.Checking("正在获取更新…")
            runCatching {
                UpdateCenter.downloadAppUpdate(
                    context = context,
                    currentVersionCode = hostCode,
                    mirrorBase = settings.mirrorBase,
                    onState = { appState = it },
                    boost = settings.networkBoost
                )
            }.onSuccess { prep ->
                prepared = prep
                appState = UpdateCenter.AppState.ReadyToInstall(prep.file, prep.manifest, prep.viaPatch)
                UpdateCenter.launchApkInstaller(context, prep.file)
            }.onFailure { e ->
                appState = if (e.message?.contains("already up to date") == true) {
                    UpdateCenter.AppState.UpToDate(overview?.remoteApp?.versionName ?: hostName)
                } else {
                    UpdateCenter.AppState.Failed(e.message ?: "未知错误")
                }
            }
        }
    }

    fun startPlugin(id: String) {
        scope.launch {
            UpdateCenter.updatePlugin(
                context = context,
                pluginId = id,
                mirrorBase = settings.mirrorBase,
                hostVersionCode = hostCode,
                onProgress = { PluginRegistry.setInstallState(id, it) },
                boost = settings.networkBoost
            )
        }
    }

    fun updateAll() {
        val targets = pluginUpdates.keys.toList()
        if (targets.isEmpty() || updatingAll) return
        scope.launch {
            updatingAll = true
            withContext(Dispatchers.IO) {
                for (id in targets) {
                    // 单个失败不中断整批（失败态已进 installStates，UI 可见）
                    UpdateCenter.updatePlugin(
                        context = context,
                        pluginId = id,
                        mirrorBase = settings.mirrorBase,
                        hostVersionCode = hostCode,
                        onProgress = { PluginRegistry.setInstallState(id, it) },
                        boost = settings.networkBoost
                    )
                }
            }
            updatingAll = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 顶栏 ──
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Column(Modifier.weight(1f)) {
                    Text("更新中心", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { refresh() }, enabled = !checking) {
                    if (checking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "重新检查")
                    }
                }
            }
        }

        // ── 清单源行 ──
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val preset = MirrorPresets.match(settings.mirrorBase)
                Text(
                    "清单源：${preset?.label ?: "自定义镜像"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("更换 / 测速")
                }
            }
        }

        if (checkMsg != null) {
            item {
                Text(
                    checkMsg!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // ── App 更新卡 ──
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                elevation = 16.dp
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(Color(0xFFFF9A8E), BrandRed)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🧊", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text("液态玻璃实验室", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "本机 v$hostName（code $hostCode）" +
                                    (overview?.let { " → 远端 v${it.remoteApp.versionName}（code ${it.remoteApp.versionCode}）" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    when (val st = appState) {
                        is UpdateCenter.AppState.Downloading -> {
                            Text("下载中（${st.from}）${st.percent}%", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { st.percent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is UpdateCenter.AppState.Patching -> {
                            Text("vcdiff 合成中 ${st.percent}%", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { st.percent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is UpdateCenter.AppState.ReadyToInstall -> {
                            Text(
                                prepared?.routeNote ?: "已就绪",
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandRed
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "已拉起系统安装器（如未弹出请点下方按钮）",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                UpdateCenter.launchApkInstaller(context, st.file)
                            }) { Text("再次拉起安装器") }
                        }
                        is UpdateCenter.AppState.UpToDate -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).background(Color(0xFF4CFF9B), CircleShape))
                                Spacer(Modifier.size(8.dp))
                                Text("已是最新版本 v${st.versionName}", style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        is UpdateCenter.AppState.Failed -> {
                            Text(
                                "更新失败：${st.reason}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { startAppUpdate() }) { Text("重试") }
                        }
                        is UpdateCenter.AppState.Checking -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.size(10.dp))
                                Text(st.message, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        UpdateCenter.AppState.Idle -> {
                            val hasUpdate = overview?.appUpdateAvailable == true
                            Button(
                                onClick = { startAppUpdate() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = overview != null && !checking,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                            ) {
                                Icon(
                                    if (hasUpdate) Icons.Filled.CloudDownload else Icons.Filled.RocketLaunch,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(if (overview == null) "获取远端版本…" else if (hasUpdate) "立即更新（增量优先）" else "已是最新版本")
                            }
                        }
                    }

                    // 远端版本更新要点（changelog.json 匹配远端 versionCode）
                    val remoteNotes = overview?.let { ov ->
                        remoteChangelog.firstOrNull { it.versionCode == ov.remoteApp.versionCode }?.notes
                    }
                    if (overview?.appUpdateAvailable == true && !remoteNotes.isNullOrEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("本版更新要点：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        remoteNotes.take(4).forEach { n ->
                            Text("· $n", style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }

        // ── 插件区头 + 一键全部 ──
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "插件（已装 ${installed.size}/5）",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (pluginUpdates.isNotEmpty()) {
                        Text(
                            "${pluginUpdates.size} 个可安装/更新",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandRed
                        )
                    }
                }
                if (pluginUpdates.isNotEmpty()) {
                    Button(
                        onClick = { updateAll() },
                        enabled = !updatingAll,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                    ) {
                        if (updatingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.size(6.dp))
                            Text("更新中…")
                        } else {
                            Text("一键全部更新")
                        }
                    }
                }
            }
        }

        // ── 五插件行 ──
        items(Catalog.libraries.size) { idx ->
            val entry = Catalog.libraries[idx]
            val local = installed[entry.id]
            val remote = overview?.remotePlugins?.firstOrNull { it.id == entry.id }
            val st = installStates[entry.id]
            val busy = st is InstallState.Downloading || st is InstallState.Installing ||
                st is InstallState.Verifying

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                elevation = 10.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (local != null) BrandRed.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(11.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${idx + 1}",
                                color = if (local != null) BrandRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                when {
                                    local == null && remote == null -> "未安装"
                                    local == null -> "未安装 → 远端 v${remote!!.versionName}"
                                    remote == null -> "已装 v${local.versionName}"
                                    local.versionCode < remote.versionCode ->
                                        "v${local.versionName} → v${remote.versionName}"
                                    else -> "已装 v${local.versionName}（最新）"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        when {
                            st is InstallState.Downloading -> StatusChip("下载 ${st.percent}%", accent = true)
                            st is InstallState.Installing -> StatusChip("安装 ${st.percent}%", accent = true)
                            st is InstallState.Verifying -> StatusChip("校验中", accent = true)
                            local != null && (remote == null || local.versionCode >= remote.versionCode) ->
                                StatusChip("最新", accent = true)
                            local != null -> StatusChip("可更新", accent = true)
                            else -> StatusChip("未安装")
                        }
                    }

                    // 进度 / 失败信息
                    when (st) {
                        is InstallState.Downloading -> {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { st.percent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is InstallState.Failed -> {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "失败：${st.reason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {}
                    }

                    // 操作按钮（未装→安装；落后→更新；最新→无）
                    val canAct = overview != null && !busy && !updatingAll &&
                        (local == null || (remote != null && local.versionCode < remote.versionCode))
                    if (canAct) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { startPlugin(entry.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRed)
                        ) {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(if (local == null) "安装插件" else "更新插件")
                        }
                    }
                }
            }
        }

        // ── 说明卡 ──
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, elevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "更新机制说明",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "· App 更新：优先 vcdiff 增量补丁（下载量约为全量的 1/6），" +
                            "本机 APK 作源在设备上合成新包后交系统安装器覆盖，全程校验 sha256。\n" +
                            "· 插件更新：不经系统安装器，下载（或补丁合成）→ 校验 → 落位 → " +
                            "DexClassLoader 重载，装完即生效。\n" +
                            "· 下载均走「直连 → 加速通道」多通道容灾（可在设置关闭）。\n" +
                            "· 清单源与资产源分离：jsDelivr 系只镜像清单，APK 资产仍由 GitHub" +
                            "Release 端点（叠加加速）提供。",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
