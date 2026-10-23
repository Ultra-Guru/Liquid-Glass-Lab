package com.ultraguru.liquidglass.data

import com.ultraguru.liquidglass.net.Downloader
import com.ultraguru.liquidglass.update.AppVersionManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.math.roundToLong

/**
 * 预设镜像体系（设置页 / 更新中心共用）。
 *
 * 语义约定（与 [com.ultraguru.liquidglass.update.AssetRef.assetBaseFor] 耦合）：
 * - 官方源与 jsDelivr 系 CDN 只做**清单**（git 内容：version.json / plugins.json /
 *   patches.json / changelog.json）镜像；**资产**（APK / vcdiff）一律回落
 *   github.com Release 端点，叠加网络加速通道代理。
 * - 自定义镜像视为「全镜像」（清单与资产同源），适合自建反代用户。
 *
 * 测速：[speedTest] 对单个预设**直连**拉取 version.json 计时（不经加速通道），
 * 真实反映该源在本机网络下的质量。
 */
object MirrorPresets {

    /** 一个可选清单源。 */
    data class Preset(
        val id: String,
        val label: String,
        val base: String,
        val note: String
    )

    val OFFICIAL = Preset(
        id = "official",
        label = "官方 GitHub",
        base = SettingsStore.DEFAULT_MIRROR,
        note = "raw.githubusercontent.com · 权威源（国内直连可能较慢）"
    )

    val JSDELIVR = Preset(
        id = "jsdelivr",
        label = "jsDelivr CDN",
        base = "https://cdn.jsdelivr.net/gh/Ultra-Guru/Liquid-Glass-Lab@main/",
        note = "全球 CDN，国内通常可达"
    )

    val FASTLY = Preset(
        id = "fastly",
        label = "Fastly CDN",
        base = "https://fastly.jsdelivr.net/gh/Ultra-Guru/Liquid-Glass-Lab@main/",
        note = "jsDelivr 的 Fastly 节点 · 备选"
    )

    val GCORE = Preset(
        id = "gcore",
        label = "Gcore CDN",
        base = "https://gcore.jsdelivr.net/gh/Ultra-Guru/Liquid-Glass-Lab@main/",
        note = "jsDelivr 的 Gcore 节点 · 备选"
    )

    /** 预设列表（顺序即 UI 展示顺序）。 */
    val presets = listOf(OFFICIAL, JSDELIVR, FASTLY, GCORE)

    /** 匹配当前设置命中的预设（null = 自定义镜像）。 */
    fun match(mirrorBase: String): Preset? =
        presets.firstOrNull { it.base.trimEnd('/') == mirrorBase.trim().trimEnd('/') }

    /** 测速结果。 */
    data class SpeedResult(
        val preset: Preset,
        /** null = 失败。 */
        val latencyMs: Long?,
        /** 拉到的远端版本号（顺带验证清单可用）。 */
        val versionName: String?,
        val error: String? = null
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** 对单个预设直连拉 version.json 计时（不经加速通道）。 */
    suspend fun speedTest(preset: Preset): SpeedResult = withContext(Dispatchers.IO) {
        val t0 = System.nanoTime()
        runCatching {
            Downloader.fetchTextDirect(preset.base.trimEnd('/') + "/version.json")
        }.fold(
            onSuccess = { text ->
                val ms = ((System.nanoTime() - t0) / 1e6).roundToLong()
                val v = runCatching {
                    json.decodeFromString<AppVersionManifest>(text).versionName
                }.getOrNull()
                SpeedResult(preset, ms, v)
            },
            onFailure = { e ->
                SpeedResult(preset, null, null, e.message ?: "不可达")
            }
        )
    }
}
