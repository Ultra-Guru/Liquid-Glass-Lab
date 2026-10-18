package com.ultraguru.liquidglass.plugin

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.util.Log
import android.view.View
import com.ultraguru.liquidglass.plugin.api.GlassPlugin
import dalvik.system.DexClassLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/** 已安装插件的落盘记录（持久化在 SharedPreferences 的 JSON 索引里）。 */
@Serializable
data class InstalledPluginInfo(
    val id: String,
    val versionCode: Int,
    val versionName: String,
    val fileName: String,
    val sha256: String,
    val installedAtMillis: Long,
    val signatureTrusted: Boolean = false
)

/** 插件安装过程状态（每个插件一条，驱动 UI）。 */
sealed interface InstallState {
    data object Idle : InstallState
    data class Downloading(val percent: Int) : InstallState
    data object Verifying : InstallState
    data class Installing(val percent: Int) : InstallState
    data class Done(val versionName: String) : InstallState
    data class Failed(val reason: String) : InstallState
}

/** 运行期加载好的插件句柄。 */
class LoadedPlugin(
    val entry: GlassPlugin,
    val classLoader: DexClassLoader,
    val info: InstalledPluginInfo,
    val resources: Resources
) {
    val meta get() = entry.meta
    val scenes get() = entry.scenes
}

/**
 * 插件注册表 & 加载引擎。
 *
 * 隔离模型：每插件一个 [DexClassLoader]，parent = 宿主 PathClassLoader。
 * Compose 运行时 / kotlinx / plugin-api 由宿主提供（插件侧 compileOnly）；
 * 玻璃库代码打进插件 APK（插件侧 implementation）——因此插件 APK 小而自治，
 * 且库与库之间的类空间完全隔离（同名类互不可见）。
 */
object PluginRegistry {

    private const val TAG = "PluginRegistry"
    private const val PREFS = "plugin_registry"
    private const val KEY_INDEX = "installed_index"
    private const val META_KEY = "com.ultraguru.liquidglass.PLUGIN_ENTRY"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private lateinit var appContext: Context
    private lateinit var prefs: android.content.SharedPreferences

    private val _installed = MutableStateFlow<Map<String, InstalledPluginInfo>>(emptyMap())
    val installed: StateFlow<Map<String, InstalledPluginInfo>> = _installed.asStateFlow()

    private val _installStates = MutableStateFlow<Map<String, InstallState>>(emptyMap())
    val installStates: StateFlow<Map<String, InstallState>> = _installStates.asStateFlow()

    /** 诊断日志（设置页可查看）。 */
    private val _diag = MutableStateFlow<List<String>>(emptyList())
    val diagnostics: StateFlow<List<String>> = _diag.asStateFlow()

    private val loaders = ConcurrentHashMap<String, LoadedPlugin>()

    fun diag(line: String) {
        Log.d(TAG, line)
        _diag.value = (_diag.value + "${System.currentTimeMillis() % 100000}: $line").takeLast(300)
    }

    fun bootstrap(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        reloadIndexFromDisk()
    }

    // ── 目录布局 ────────────────────────────────────────────────────────

    fun pluginsRoot(): File = File(appContext.filesDir, "plugins").apply { mkdirs() }

    fun apkFileFor(id: String, versionCode: Int): File =
        File(pluginsRoot(), "plugin-$id-$versionCode.apk")

    fun libDirFor(id: String): File = File(pluginsRoot(), "$id-lib")

    fun downloadsDir(): File = File(appContext.filesDir, "downloads").apply { mkdirs() }

    // ── 索引持久化 ──────────────────────────────────────────────────────

    private fun reloadIndexFromDisk() {
        val raw = prefs.getString(KEY_INDEX, null)
        val map: Map<String, InstalledPluginInfo> = if (raw != null) {
            runCatching {
                json.decodeFromString<Map<String, InstalledPluginInfo>>(raw)
            }.getOrDefault(emptyMap())
        } else emptyMap()
        // 索引与磁盘对账：文件丢了（用户清理 / 备份丢失）自动剔除索引
        val valid = map.filterValues { File(pluginsRoot(), it.fileName).exists() }
        if (valid.size != map.size) {
            diag("索引对账：${map.size - valid.size} 个插件文件缺失，已移除索引项")
            persistIndex(valid)
        }
        _installed.value = valid
    }

    private fun persistIndex(map: Map<String, InstalledPluginInfo>) {
        prefs.edit().putString(KEY_INDEX, json.encodeToString(map)).apply()
        _installed.value = map
    }

    // ── 安装 / 卸载（由 UpdateCenter 下载完成后调用）──────────────────

    fun setInstallState(id: String, state: InstallState) {
        _installStates.value = _installStates.value + (id to state)
    }

    /**
     * 把已下载、已通过 sha256 校验的插件 APK 落位安装。
     * 返回 null = 成功；非 null = 失败原因。
     */
    fun installFromDownload(id: String, apk: File, expectedSha256: String): String? {
        setInstallState(id, InstallState.Installing(10))
        return runCatching {
            val sha = sha256Of(apk)
            if (!sha.equals(expectedSha256, ignoreCase = true)) {
                apk.delete()
                return "sha256 不匹配（期望 ${expectedSha256.take(12)}…，实际 ${sha.take(12)}…）"
            }
            setInstallState(id, InstallState.Installing(40))

            // 预读清单与签名
            val pm = appContext.packageManager
            val pkgInfo = pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_META_DATA)
                ?: return "无法解析插件 APK（getPackageArchiveInfo = null）"
            val entryClass = pkgInfo.applicationInfo?.metaData?.getString(META_KEY)
                ?: return "插件清单缺少入口 meta-data（$META_KEY）"

            val trusted = isSignatureTrusted(apk)
            setInstallState(id, InstallState.Installing(60))

            // 卸旧装新（原子替换）
            unload(id)
            val versionName = pkgInfo.versionName ?: "?"
            val dest = apkFileFor(id, pkgInfo.versionCode)
            if (dest.exists()) dest.delete()
            if (!apk.renameTo(dest) && !apk.copyRecursivelyAndDelete(dest)) {
                return "移动 APK 到插件目录失败"
            }
            extractNativeLibs(id, dest)
            setInstallState(id, InstallState.Installing(85))

            val info = InstalledPluginInfo(
                id = id,
                versionCode = pkgInfo.versionCode,
                versionName = versionName,
                fileName = dest.name,
                sha256 = sha,
                installedAtMillis = System.currentTimeMillis(),
                signatureTrusted = trusted
            )
            val map = _installed.value + (id to info)
            persistIndex(map)

            // 立即试加载：装完就能用，加载失败立即反馈
            val loaded = load(id)
            diag("插件 [$id] v$versionName 安装成功（entry=$entryClass, trusted=$trusted, scenes=${loaded.scenes.size}）")
            setInstallState(id, InstallState.Done(versionName))
            null
        }.onFailure { e ->
            diag("插件 [$id] 安装异常：${e.message}")
            setInstallState(id, InstallState.Failed(e.message ?: "未知异常"))
        }.getOrElse { it.message ?: "未知异常" }
    }

    fun uninstall(id: String) {
        unload(id)
        val map = _installed.value
        map[id]?.let { info ->
            File(pluginsRoot(), info.fileName).delete()
            val index = map - id
            persistIndex(index)
        }
        libDirFor(id).deleteRecursively()
        setInstallState(id, InstallState.Idle)
        diag("插件 [$id] 已卸载")
    }

    private fun File.copyRecursivelyAndDelete(target: File): Boolean = runCatching {
        target.outputStream().use { out -> inputStream().use { it.copyTo(out) } }
        delete()
        true
    }.getOrDefault(false)

    // ── 加载 ───────────────────────────────────────────────────────────

    /** 加载（或复用缓存）已安装插件；失败抛异常并由调用方渲染错误态。 */
    fun load(id: String): LoadedPlugin {
        loaders[id]?.let { return it }
        val info = _installed.value[id] ?: throw IllegalArgumentException("插件 [$id] 未安装")

        val apk = File(pluginsRoot(), info.fileName)
        val pm = appContext.packageManager
        val pkgInfo = pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_META_DATA)
            ?: throw IllegalStateException("插件 APK 解析失败（文件损坏？）")
        pkgInfo.applicationInfo?.apply {
            sourceDir = apk.absolutePath
            publicSourceDir = apk.absolutePath
        }
        val entryClass = pkgInfo.applicationInfo?.metaData?.getString(META_KEY)
            ?: throw IllegalStateException("插件缺少入口 meta-data")

        val pluginRes: Resources = appContext.packageManager
            .getResourcesForApplication(pkgInfo.applicationInfo!!)

        val codeCache = File(appContext.codeCacheDir, "plugins/$id").apply { mkdirs() }
        val libSearch = nativeSearchPath(id)
        val classLoader = DexClassLoader(apk.absolutePath, codeCache.absolutePath, libSearch, this::class.java.classLoader)
        val pluginContext = PluginResourcesContext(appContext, pluginRes)

        @Suppress("UNCHECKED_CAST")
        val entry = Class.forName(entryClass, true, classLoader)
            .getDeclaredConstructor()
            .also { it.isAccessible = true }
            .newInstance() as GlassPlugin
        entry.initialize(pluginContext)

        return LoadedPlugin(entry, classLoader, info, pluginRes).also {
            loaders[id] = it
            diag("插件 [$id] v${it.meta.versionName} 加载成功（dex=${apk.name}）")
        }
    }

    private fun unload(id: String) {
        loaders.remove(id)?.let {
            runCatching { it.entry.dispose() }
        }
    }

    /** 场景 View 工厂：宿主把重定向资源后的 Context 交给插件。 */
    fun createSceneView(hostActivityContext: Context, id: String, sceneId: String): View {
        val loaded = load(id)
        val ctx = PluginResourcesContext(hostActivityContext, loaded.resources)
        return loaded.entry.createScene(ctx, sceneId)
    }

    // ── 工具 ───────────────────────────────────────────────────────────

    private fun nativeSearchPath(id: String): String? {
        val dirs = Build.SUPPORTED_ABIS
            .map { File(libDirFor(id), it) }
            .filter { it.isDirectory && it.listFiles()?.isNotEmpty() == true }
            .map { it.absolutePath }
        return dirs.joinToString(":").ifEmpty { null }
    }

    private fun extractNativeLibs(id: String, apk: File) {
        runCatching {
            java.util.zip.ZipFile(apk).use { zip ->
                val entries = zip.entries().asSequence().filter {
                    it.name.startsWith("lib/") && it.name.endsWith(".so")
                }
                for (e in entries) {
                    val out = File(libDirFor(id), e.name.removePrefix("lib/"))
                    out.parentFile?.mkdirs()
                    zip.getInputStream(e).use { input ->
                        out.outputStream().use { input.copyTo(it) }
                    }
                }
            }
        }.onFailure { diag("[$id] .so 提取跳过：${it.message}") }
    }

    /** 插件 APK 签名与宿主是否同源（严格模式下必须一致）。 */
    @SuppressLint("PackageManagerGetSignatures")
    fun isSignatureTrusted(apk: File): Boolean = runCatching {
        val pm = appContext.packageManager
        val pluginSigs = pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNATURES)
            ?.signatures?.takeIf { it.isNotEmpty() } ?: return@runCatching false
        val hostSigs = pm.getPackageInfo(appContext.packageName, PackageManager.GET_SIGNATURES)
            .signatures ?: return@runCatching false
        if (pluginSigs.isEmpty() || hostSigs.isEmpty()) return@runCatching false
        val md = MessageDigest.getInstance("SHA-256")
        pluginSigs.map { md.digest(it.toByteArray()).joinToString("") { b -> "%02x".format(b) } }
            .any { p -> hostSigs.any { h -> md.digest(h.toByteArray()).joinToString("") { b -> "%02x".format(b) } == p } }
    }.getOrDefault(false)

    fun sha256Of(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { b -> "%02x".format(b) }
    }

    // ── 系统已装插件导入 ──────────────────────────────────────────

    /** 五大插件的系统包名（与 AndroidManifest <queries> 声明一致）。 */
    private val PLUGIN_PACKAGE_IDS = listOf(
        "com.ultraguru.liquidglass.plugin.kyant0" to "kyant0",
        "com.ultraguru.liquidglass.plugin.cloudy" to "cloudy",
        "com.ultraguru.liquidglass.plugin.qmdeve" to "qmdeve",
        "com.ultraguru.liquidglass.plugin.qwea0" to "qwea0",
        "com.ultraguru.liquidglass.plugin.nadeem" to "nadeem"
    )

    /**
     * 扫描系统已安装包，发现本项目的插件 APK（用户从 Release 手动安装的）则导入注册表。
     *
     * 场景：用户直接下载 Release 里的 plugin-*.apk 用系统安装器装了，
     * 但宿主内部索引无从知晓 —— 导入后立即显示为「已安装」并可用。
     *
     * v3 修复：
     *  - Android 11+ 包可见性：宿主 Manifest 已声明 <queries>（5 个插件包名），
     *    按包名定点 getPackageInfo（比泛扫 getInstalledPackages 更确定）。
     *  - App onResume 自动重扫（GlassLabApp），装完插件回到 App 即生效。
     *
     * 完整性说明：系统安装器装包时已验签名；此处再把 APK 拷入私有目录交由
     * [installFromDownload] 解析落位（sha256 以拷贝件自算，信任链来自系统安装事实）。
     * 返回本次新导入的插件数。
     */
    fun adoptSystemInstalledPlugins(): Int {
        if (!::appContext.isInitialized) return 0
        val pm = appContext.packageManager
        var adopted = 0
        for ((pkgName, pluginId) in PLUGIN_PACKAGE_IDS) {
            val pi = runCatching {
                pm.getPackageInfo(pkgName, PackageManager.GET_META_DATA)
            }.getOrNull() ?: continue
            val sysCode = pi.longVersionCode.toInt()
            val local = _installed.value[pluginId]
            if (local != null && local.versionCode >= sysCode) continue
            val src = pi.applicationInfo?.sourceDir?.let { File(it) }?.takeIf { it.isFile } ?: continue
            val tmp = File(downloadsDir(), "plugin-$pluginId-sys.apk")
            val copied = runCatching { src.copyTo(tmp, overwrite = true) }.isSuccess
            if (!copied) {
                diag("插件 [$pluginId] 系统导入失败：无法读取 $src")
                continue
            }
            val err = installFromDownload(pluginId, tmp, sha256Of(tmp))
            if (err == null) {
                adopted++
                diag("插件 [$pluginId] 已从系统安装包导入（v${pi.versionName ?: sysCode}）")
            } else {
                diag("插件 [$pluginId] 系统导入失败：$err")
            }
        }
        return adopted
    }

    fun isInstalled(id: String): Boolean = _installed.value.containsKey(id)

    fun installedInfo(id: String): InstalledPluginInfo? = _installed.value[id]
}

/**
 * 插件资源上下文：base 为宿主，getResources() 重定向到插件 APK 资源。
 * 插件代码里所有 R.* 访问经由它解析。
 */
class PluginResourcesContext(
    base: Context,
    private val pluginResources: Resources
) : ContextWrapper(base) {
    override fun getResources(): Resources = pluginResources
}
