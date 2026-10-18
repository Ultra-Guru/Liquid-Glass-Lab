package com.ultraguru.liquidglass.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ultraguru.liquidglass.GlassLabApp
import com.ultraguru.liquidglass.data.Catalog
import com.ultraguru.liquidglass.update.UpdateCenter
import com.ultraguru.liquidglass.update.ChangelogEntry
import com.ultraguru.liquidglass.ui.theme.BrandRed
import com.ultraguru.liquidglass.ui.theme.BrandRedDeep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = GlassLabApp.get()
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    var changelog by remember { mutableStateOf<List<ChangelogEntry>>(emptyList()) }
    var changelogMsg by remember { mutableStateOf<String?>(null) }

    // App 更新执行态（此处执行下载/合成/安装）
    var updateState by remember { mutableStateOf<UpdateCenter.AppState>(UpdateCenter.AppState.Idle) }
    var readyApk by remember { mutableStateOf<java.io.File?>(null) }

    val settings = app.settings.state.value

    val versionInfo = remember {
        runCatching {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val pi = pm.getPackageInfo(context.packageName, 0)
            pi.versionName to pi.versionCode
        }.getOrDefault("?" to 1)
    }

    LaunchedEffect(Unit) {
        scope.launch {
            runCatching {
                UpdateCenter.fetchChangelog(settings.mirrorBase)
            }.onSuccess { changelog = it }
                .onFailure { changelogMsg = "更新日志加载失败：${it.message}" }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 48.dp, bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                Text("关于", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(84.dp)
                            .background(
                                Brush.linearGradient(listOf(BrandRedDeep, BrandRed)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧊", style = MaterialTheme.typography.headlineLarge)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("液态玻璃实验室", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Liquid Glass Lab",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "v${versionInfo.first}（code ${versionInfo.second}）· minAndroid 13 · 单仓库 vcdiff 增量更新",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    updateState = UpdateCenter.AppState.Checking("检查更新…")
                                    runCatching {
                                        UpdateCenter.downloadAppUpdate(
                                            context = context,
                                            currentVersionCode = versionInfo.second,
                                            mirrorBase = settings.mirrorBase,
                                            onState = { updateState = it }
                                        )
                                    }.onSuccess { apk ->
                                        readyApk = apk
                                        updateState = UpdateCenter.AppState.ReadyToInstall(
                                            apk, UpdateCenter.fetchAppManifest(settings.mirrorBase),
                                            viaPatch = true
                                        )
                                        UpdateCenter.launchApkInstaller(context, apk)
                                    }.onFailure {
                                        updateState = if (it.message?.contains("already up to date") == true) {
                                            UpdateCenter.AppState.UpToDate(versionInfo.first)
                                        } else {
                                            UpdateCenter.AppState.Failed(it.message ?: "未知错误")
                                        }
                                    }
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = BrandRed)
                        ) { Text("检查更新") }
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ultra-Guru/Liquid-Glass-Lab"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }) { Text("项目仓库 ↗") }
                    }
                    when (val st = updateState) {
                        is UpdateCenter.AppState.Downloading -> {
                            Spacer(Modifier.height(8.dp))
                            Text("下载中（${st.from}）${st.percent}%", style = MaterialTheme.typography.labelMedium)
                            LinearProgressIndicator(
                                progress = { st.percent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is UpdateCenter.AppState.Patching -> {
                            Spacer(Modifier.height(8.dp))
                            Text("vcdiff 合成中 ${st.percent}%", style = MaterialTheme.typography.labelMedium)
                            LinearProgressIndicator(
                                progress = { st.percent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is UpdateCenter.AppState.ReadyToInstall -> {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "已就绪：${st.file.name}\n已拉起系统安装器（如未弹出请点击重装）",
                                style = MaterialTheme.typography.labelMedium
                            )
                            TextButton(onClick = {
                                UpdateCenter.launchApkInstaller(context, st.file)
                            }) { Text("再次拉起安装器") }
                        }
                        is UpdateCenter.AppState.UpToDate -> {
                            Spacer(Modifier.height(8.dp))
                            Text("已是最新版本 v${st.versionName}", style = MaterialTheme.typography.labelMedium)
                        }
                        is UpdateCenter.AppState.Failed -> {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "更新失败：${st.reason}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is UpdateCenter.AppState.Checking -> Text(st.message, style = MaterialTheme.typography.labelMedium)
                        UpdateCenter.AppState.Idle -> {}
                    }
                }
            }
        }

        item {
            Text(
                "开源致谢（五大顶级液态玻璃库）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        items(Catalog.libraries.size) { i ->
            val e = Catalog.libraries[i]
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                ListItem(
                    overlineContent = { Text("${e.stars}★ · ${e.license}", style = MaterialTheme.typography.labelSmall) },
                    headlineContent = { Text(e.title, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("by ${e.author} · ${e.coordinate}") },
                    trailingContent = {
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(e.repoUrl))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }) { Text("↗") }
                    }
                )
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("许可", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "本项目 Apache-2.0。五个插件各自内嵌对应开源库（遵守其原协议：Apache-2.0 / MIT），" +
                            "插件 APK 由 CI 自动构建并附 sha256 校验。VCDIFF 解码器移植自同组织" +
                            " Android-Guru-Agent（Apache-2.0）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                "更新日志",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        if (changelog.isEmpty()) {
            item {
                Text(
                    changelogMsg ?: "（暂无日志 —— 首个 Release 后由 CI 写入 changelog.json）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(changelog.size) { i ->
            val c = changelog[i]
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "v${c.versionName}",
                            fontWeight = FontWeight.Bold,
                            color = BrandRed
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            c.date ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    c.notes.forEach { n ->
                        Text("· $n", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
