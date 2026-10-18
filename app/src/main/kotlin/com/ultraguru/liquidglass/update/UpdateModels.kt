package com.ultraguru.liquidglass.update

import kotlinx.serialization.Serializable

/**
 * 更新体系数据契约（三个 json 的客户端侧镜像）。
 *
 * ```
 * 仓库根
 * ├── version.json   ← App 版本清单（versionName/Code + 全量 APK + 直连补丁）
 * ├── plugins.json   ← 插件清单（每插件：版本 + APK + 独立补丁链 + changelog）
 * ├── patches.json   ← App 跨版本补丁窗口（最近 N 个基底的直达补丁）
 * └── changelog.json ← 版本日志（关于页渲染）
 * ```
 *
 * 由 release.yml 在发版时生成并提交回 main；App「设置 → 检查更新」读取。
 *
 * ## 镜像语义
 * [AssetRef.url] 是 GitHub 规范绝对地址；[AssetRef.path] 是相对仓库的路径。
 * 默认（官方源）直接用 url；自定义镜像时优先 path 反射到镜像基地址 ——
 * 镜像只需镜像 `raw 清单 + Release 资产` 两个面即可完整工作。
 */

@Serializable
data class AssetRef(
    val url: String,
    val path: String? = null,
    val sizeBytes: Long = 0,
    val sha256: String
) {
    /** 依镜像设置解析实际下载地址。 */
    fun resolve(mirrorBase: String): String {
        val p = path?.takeIf { it.isNotBlank() } ?: return url
        if (mirrorBase.isBlank()) return url
        return mirrorBase.trimEnd('/') + "/" + p.trimStart('/')
    }
}

/** version.json 客户端模型。 */
@Serializable
data class AppVersionManifest(
    val schema: String = "liquid-glass-lab.version.v1",
    val versionName: String,
    val versionCode: Int,
    val tag: String? = null,
    val publishedAt: String? = null,
    /** null = 尚无发布（App 内显示「等待首个 Release」）。 */
    val download: AssetRef? = null,
    /** 相邻版本直连补丁（fromVersionCode == 当前安装 versionCode 时可用）。 */
    val patch: AssetRef? = null,
    val commitSha: String? = null,
    val signingCertSha256: String? = null,
    val minAndroid: Int = 33
)

/** plugins.json 客户端模型。 */
@Serializable
data class PluginsManifest(
    val schema: String = "liquid-glass-lab.plugins.v1",
    val generatedAt: String? = null,
    val tag: String? = null,
    val plugins: List<PluginRelease> = emptyList()
) {
    fun byId(id: String): PluginRelease? = plugins.firstOrNull { it.id == id }
}

@Serializable
data class PluginRelease(
    val id: String,
    val name: String,
    val versionName: String,
    val versionCode: Int,
    /** 宿主 versionCode 低于此值时插件拒绝安装（提示先升级宿主）。 */
    val minHostVersionCode: Int = 1,
    val apk: AssetRef? = null,
    /** 插件级独立补丁链（fromVersionCode → 本版），按 fromVersionCode 降序排布。 */
    val patches: List<AssetRef> = emptyList(),
    val changelog: List<String> = emptyList()
)

/** patches.json 客户端模型（App 多基底补丁窗口）。 */
@Serializable
data class PatchesIndex(
    val schema: String = "liquid-glass-lab.patches.v1",
    val tag: String? = null,
    val versionCode: Int = 0,
    val bases: List<AssetRef> = emptyList()
)

/** changelog.json 客户端模型。 */
@Serializable
data class ChangelogEntry(
    val versionCode: Int,
    val versionName: String,
    val tag: String? = null,
    val date: String? = null,
    val notes: List<String> = emptyList()
)
