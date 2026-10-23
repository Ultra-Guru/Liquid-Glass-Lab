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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultraguru.liquidglass.GlassLabApp
import com.ultraguru.liquidglass.data.Catalog
import com.ultraguru.liquidglass.update.UpdateCenter
import com.ultraguru.liquidglass.update.ChangelogEntry
import com.ultraguru.liquidglass.ui.components.GlassCard
import com.ultraguru.liquidglass.ui.theme.BrandRed
import com.ultraguru.liquidglass.ui.theme.BrandRedDeep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenUpdates: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = GlassLabApp.get()
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    var changelog by remember { mutableStateOf<List<ChangelogEntry>>(emptyList()) }
    var changelogMsg by remember { mutableStateOf<String?>(null) }

    val settings by app.settings.state.collectAsState()

    val versionInfo = remember {
        runCatching {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val pi = pm.getPackageInfo(context.packageName, 0)
            (pi.versionName ?: "?") to pi.versionCode
        }.getOrDefault("?" to 1)
    }

    LaunchedEffect(Unit) {
        scope.launch {
            runCatching {
                UpdateCenter.fetchChangelog(settings.mirrorBase, settings.networkBoost)
            }.onSuccess { changelog = it }
                .onFailure { changelogMsg = "更新日志加载失败：${it.message}" }
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                Text("关于", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, elevation = 16.dp) {
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
                        androidx.compose.material3.Button(
                            onClick = onOpenUpdates,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = BrandRed)
                        ) {
                            Icon(Icons.Filled.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("前往更新中心")
                        }
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ultra-Guru/Liquid-Glass-Lab"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }) { Text("项目仓库 ↗") }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "App 增量升级（vcdiff 补丁优先）与插件安装 / 更新已统一在「更新中心」。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, elevation = 10.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "升级说明（老版本用户）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandRed
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "v0.1.1 起签名密钥统一为 meng4117222（CI 与本地构建指纹恒定，" +
                            "此后版本可正常覆盖升级 / 增量更新）。\n" +
                            "仍在使用 v0.1.0 的用户：签名与新版不同，系统会拒绝覆盖安装，" +
                            "请先卸载旧版（数据仅是插件索引，重装后点「安装」即恢复），" +
                            "再到 Release 下载最新 APK 安装 —— 仅此一次。",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
            GlassCard(cornerRadius = 20.dp, elevation = 10.dp) {
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
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, elevation = 10.dp) {
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
            GlassCard(cornerRadius = 20.dp, elevation = 10.dp) {
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
