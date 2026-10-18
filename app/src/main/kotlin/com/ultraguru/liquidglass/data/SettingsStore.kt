package com.ultraguru.liquidglass.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 主题模式。 */
enum class ThemeMode { SYSTEM, DARK, LIGHT }

/**
 * 应用设置（SharedPreferences 持久化 + StateFlow 驱动 Compose）。
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("glass_lab_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(readAll())
    val state: StateFlow<Settings> = _state.asStateFlow()

    data class Settings(
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val dynamicColor: Boolean = true,
        /** 清单获取基地址（version.json / plugins.json / patches.json）；资产 URL 优先按镜像解析相对路径。 */
        val mirrorBase: String = DEFAULT_MIRROR,
        /** 严格模式：插件 APK 签名必须与宿主一致（自编译 debug 包会警告降级）。 */
        val verifyPluginSignature: Boolean = true,
        /** 启动时静默检查更新。 */
        val silentUpdateCheck: Boolean = true
    ) {
        fun isCustomMirror(): Boolean = mirrorBase.trimEnd('/') != DEFAULT_MIRROR
    }

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_state.value)
        prefs.edit()
            .putString("themeMode", next.themeMode.name)
            .putBoolean("dynamicColor", next.dynamicColor)
            .putString("mirrorBase", next.mirrorBase)
            .putBoolean("verifyPluginSignature", next.verifyPluginSignature)
            .putBoolean("silentUpdateCheck", next.silentUpdateCheck)
            .apply()
        _state.value = next
    }

    private fun readAll(): Settings = Settings(
        themeMode = runCatching {
            ThemeMode.valueOf(prefs.getString("themeMode", ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM),
        dynamicColor = prefs.getBoolean("dynamicColor", true),
        mirrorBase = prefs.getString("mirrorBase", DEFAULT_MIRROR)?.takeIf { it.isNotBlank() } ?: DEFAULT_MIRROR,
        verifyPluginSignature = prefs.getBoolean("verifyPluginSignature", true),
        silentUpdateCheck = prefs.getBoolean("silentUpdateCheck", true)
    )

    companion object {
        /** 官方清单源（raw 快照，随发版更新）。 */
        const val DEFAULT_MIRROR =
            "https://raw.githubusercontent.com/Ultra-Guru/Liquid-Glass-Lab/main/"

        /** GitHub Release 资产的规范基地址（解析相对 path 用）。 */
        const val GITHUB_BASE =
            "https://github.com/Ultra-Guru/Liquid-Glass-Lab/"
    }
}
