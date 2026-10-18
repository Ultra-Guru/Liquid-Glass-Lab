package com.ultraguru.liquidglass.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ultraguru.liquidglass.net.Downloader
import com.ultraguru.liquidglass.plugin.InstallState
import com.ultraguru.liquidglass.plugin.PluginRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 更新中心：App 自身 + 插件的两条更新链路。
 *
 * ## App（单仓库 vcdiff 链）
 * 检查 version.json →（patch.fromVersionCode == 本机 versionCode）下载补丁
 * + 以运行中 APK 为源 VCDIFF 合成 → sha256 校验 → 系统安装器覆盖安装；
 * 无直连补丁时查 patches.json 窗口；再退化为全量 APK。
 *
 * ## 插件（更复杂的独立链）
 * 每插件在 plugins.json 有独立 versionCode + 补丁链：下载（全量或补丁合成）
 * → sha256 → 落位安装（DexClassLoader 重载）→ 即时生效，全程不经系统安装器。
 */
object UpdateCenter {

    private val json = Json { ignoreUnknownKeys = true }

    /** App 更新流程状态。 */
    sealed interface AppState {
        data object Idle : AppState
        data class Checking(val message: String) : AppState
        data class Downloading(val percent: Int, val from: String) : AppState
        data class Patching(val percent: Int) : AppState
        data class ReadyToInstall(val file: File, val manifest: AppVersionManifest, val viaPatch: Boolean) : AppState
        data class UpToDate(val versionName: String) : AppState
        data class Failed(val reason: String) : AppState
    }

    // ── 清单获取 ──────────────────────────────────────────────────────

    suspend fun fetchAppManifest(mirrorBase: String): AppVersionManifest = withContext(Dispatchers.IO) {
        json.decodeFromString(Downloader.fetchText(mirrorBase.trimEnd('/') + "/version.json"))
    }

    suspend fun fetchPluginsManifest(mirrorBase: String): PluginsManifest = withContext(Dispatchers.IO) {
        json.decodeFromString(Downloader.fetchText(mirrorBase.trimEnd('/') + "/plugins.json"))
    }

    suspend fun fetchPatchesIndex(mirrorBase: String): PatchesIndex? = withContext(Dispatchers.IO) {
        runCatching {
            json.decodeFromString<PatchesIndex>(Downloader.fetchText(mirrorBase.trimEnd('/') + "/patches.json"))
        }.getOrNull()
    }

    suspend fun fetchChangelog(mirrorBase: String): List<ChangelogEntry> = withContext(Dispatchers.IO) {
        runCatching {
            json.decodeFromString<List<ChangelogEntry>>(Downloader.fetchText(mirrorBase.trimEnd('/') + "/changelog.json"))
        }.getOrDefault(emptyList())
    }

    // ── App 更新链 ───────────────────────────────────────────────────

    /**
     * 为当前安装的 App 解析远端版本信息（供设置页展示「发现新版本」）。
     * 返回 null = 已是最新。
     */
    suspend fun peekAppUpdate(
        currentVersionCode: Int,
        mirrorBase: String
    ): AppVersionManifest? = withContext(Dispatchers.IO) {
        val manifest = fetchAppManifest(mirrorBase)
        if (manifest.versionCode > currentVersionCode) manifest else null
    }

    /**
     * 下载 App 升级包（补丁优先，自动合成），返回可安装 APK 文件。
     * [onState] 汇报进度；抛出异常 = 失败。
     */
    suspend fun downloadAppUpdate(
        context: Context,
        currentVersionCode: Int,
        mirrorBase: String,
        onState: (AppState) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val manifest = fetchAppManifest(mirrorBase)
        if (manifest.versionCode <= currentVersionCode) {
            onState(AppState.UpToDate(manifest.versionName))
            throw IllegalStateException("already up to date")
        }

        val updatesDir = File(context.filesDir, "updates").apply { mkdirs() }
        val target = File(updatesDir, "liquidglass-${manifest.versionName}-${manifest.versionCode}.apk")
        val currentApk = File(context.applicationInfo.sourceDir)

        // 1) 相邻直连补丁
        val directPatch = manifest.patch
        if (directPatch != null && directPatch.url.isNotBlank() && patchFrom(directPatch) == currentVersionCode) {
            onState(AppState.Downloading(0, "增量补丁"))
            val patchFile = File(updatesDir, "patch_to_${manifest.versionCode}.vcdiff")
            Downloader.download(directPatch.resolve(mirrorBase), patchFile) { p ->
                onState(AppState.Downloading(p, "增量补丁"))
            }
            onState(AppState.Patching(0))
            applyVcdiff(currentApk, patchFile, target, directPatch) { p -> onState(AppState.Patching(p)) }
            patchFile.delete()
            return@withContext target
        }

        // 2) 窗口补丁（patches.json 多基底）
        val window = fetchPatchesIndex(mirrorBase)
            ?.bases
            ?.firstOrNull { patchFrom(it) == currentVersionCode }
        if (window != null && window.url.isNotBlank()) {
            onState(AppState.Downloading(0, "窗口补丁"))
            val patchFile = File(updatesDir, "patch_w_${manifest.versionCode}.vcdiff")
            Downloader.download(window.resolve(mirrorBase), patchFile) { p ->
                onState(AppState.Downloading(p, "窗口补丁"))
            }
            onState(AppState.Patching(0))
            applyVcdiff(currentApk, patchFile, target, window) { p -> onState(AppState.Patching(p)) }
            patchFile.delete()
            return@withContext target
        }

        // 3) 全量
        val full = manifest.download ?: throw IllegalStateException("远端版本更高但无可用资产")
        onState(AppState.Downloading(0, "全量 APK"))
        Downloader.download(full.resolve(mirrorBase), target) { p ->
            onState(AppState.Downloading(p, "全量 APK"))
        }
        val sha = PluginRegistry.sha256Of(target)
        if (!sha.equals(full.sha256, ignoreCase = true)) {
            target.delete()
            throw IllegalStateException("全量 APK sha256 校验失败")
        }
        target
    }

    /** 拉起系统安装器（App 覆盖安装）。 */
    fun launchApkInstaller(context: Context, apk: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    // ── 插件更新链 ───────────────────────────────────────────────────

    /**
     * 检查全部插件更新（不下载）。
     * 返回 (插件id → PluginRelease) 中 versionCode 高于本机的条目。
     */
    suspend fun findPluginUpdates(mirrorBase: String): Map<String, PluginRelease> =
        withContext(Dispatchers.IO) {
            val manifest = fetchPluginsManifest(mirrorBase)
            val installed = PluginRegistry.installed.value
            manifest.plugins.filter { rel ->
                val local = installed[rel.id]
                local == null || rel.versionCode > local.versionCode
            }.associateBy { it.id }
        }

    /**
     * 执行单个插件更新（全量或补丁合成）。
     * 返回 null = 成功；非 null = 失败原因。
     */
    suspend fun updatePlugin(
        context: Context,
        pluginId: String,
        mirrorBase: String,
        hostVersionCode: Int,
        onProgress: (InstallState) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        try {
            val manifest = fetchPluginsManifest(mirrorBase)
            val rel = manifest.byId(pluginId) ?: return@withContext "plugins.json 中不存在 [$pluginId]"
            if (rel.versionCode <= (PluginRegistry.installed.value[pluginId]?.versionCode ?: 0)) {
                return@withContext "已是最新版本"
            }
            if (hostVersionCode < rel.minHostVersionCode) {
                return@withContext "需要先升级宿主 App（要求 ≥ v${rel.minHostVersionCode}）"
            }
            val apkRef = rel.apk ?: return@withContext "该插件当前版本无 APK 资产"

            val tempFull = File(PluginRegistry.downloadsDir(), "plugin-$pluginId-full.apk")
            val current = PluginRegistry.installed.value[pluginId]

            // 插件补丁链：fromVersionCode 与本机精确匹配才走增量
            val patch = rel.patches.firstOrNull { patchFrom(it) == current?.versionCode }
            val target = if (patch != null && patch.url.isNotBlank()) {
                onProgress(InstallState.Downloading(0))
                val patchFile = File(PluginRegistry.downloadsDir(), "plugin-$pluginId-patch.vcdiff")
                Downloader.download(patch.resolve(mirrorBase), patchFile) { p ->
                    onProgress(InstallState.Downloading(p))
                }
                onProgress(InstallState.Installing(5))
                val source = File(PluginRegistry.pluginsRoot(), current!!.fileName)
                applyVcdiff(source, patchFile, tempFull, patch) { p ->
                    onProgress(InstallState.Installing((p * 0.6f).toInt().coerceIn(5, 60)))
                }
                patchFile.delete()
                tempFull
            } else {
                onProgress(InstallState.Downloading(0))
                Downloader.download(apkRef.resolve(mirrorBase), tempFull) { p ->
                    onProgress(InstallState.Downloading(p))
                }
                tempFull
            }

            onProgress(InstallState.Verifying)
            val err = PluginRegistry.installFromDownload(pluginId, target, apkRef.sha256)
            if (err != null) onProgress(InstallState.Failed(err))
            err
        } catch (e: Exception) {
            val msg = "插件更新失败：${e.message}"
            onProgress(InstallState.Failed(msg))
            msg
        }
    }

    // ── VCDIFF 应用 ───────────────────────────────────────────────────

    /** 从资产命名解析补丁基底版本（patch_..._v{N}_to_... 或 file 命名 from_{N}）。 */
    fun patchFrom(asset: AssetRef): Int? {
        val candidates = listOfNotNull(asset.url, asset.path)
        for (c in candidates) {
            Regex("""_v?(\d+)_to_""").find(c)?.let { return it.groupValues[1].toIntOrNull() }
            Regex("""_from_(\d+)\.""").find(c)?.let { return it.groupValues[1].toIntOrNull() }
        }
        return null
    }

    private fun applyVcdiff(
        source: File,
        patch: File,
        output: File,
        expected: AssetRef,
        onProgress: (Int) -> Unit
    ) {
        val decoder = VcdiffDecoder()
        output.outputStream().use { /* touch */ }
        decoder.decode(source, patch.readBytes(), output, expected.sizeBytes, object : VcdiffDecoder.ProgressListener {
            override fun onProgress(decodedBytes: Long) {
                if (expected.sizeBytes > 0) {
                    onProgress(((decodedBytes * 100) / expected.sizeBytes).toInt().coerceIn(0, 100))
                }
            }
        })
        val sha = PluginRegistry.sha256Of(output)
        if (!sha.equals(expected.sha256, ignoreCase = true)) {
            output.delete()
            throw IllegalStateException("VCDIFF 合成结果 sha256 不匹配（补丁损坏或基底不符）")
        }
    }
}
