package com.ultraguru.liquidglass.plugin.api

import android.content.Context
import android.view.View

/**
 * 液态玻璃插件契约（v1）。
 *
 * ## 插件是什么
 * 一个插件 = 一个独立 APK（`plugins/<id>/` 模块构建产物），内嵌某个开源液态玻璃
 * 库的演示实现。宿主 App 通过 `DexClassLoader` 隔离加载——插件与插件、插件与
 * 宿主之间类空间互不污染：每个插件有独立 ClassLoader，Compose 运行时等公共
 * 基础设施由宿主以 parent-delegation 方式提供。
 *
 * ## 入口发现
 * 插件 AndroidManifest.xml 的 `<application>` 内声明：
 * ```xml
 * <meta-data android:name="com.ultraguru.liquidglass.PLUGIN_ENTRY"
 *            android:value="com.ultraguru.liquidglass.plugin.xxx.XxxPlugin"/>
 * ```
 * 宿主读取该 meta-data 反射实例化实现类（必须有无参构造器）。
 *
 * ## 生命周期
 * 1. 宿主 `load()` 成功后调用 [initialize]（一次性，传插件资源上下文）；
 * 2. [meta] / [scenes] 为宿主提供列表数据；
 * 3. 每次进入演示页调用 [createScene] 返回一个全屏 View（Compose 插件用
 *    `ComposeView` 包装；View 体系插件直接返回自定义 View）；
 * 4. 宿主卸载插件或进程结束时调用 [dispose]。
 *
 * ## 场景规约（四项固定演示，跨插件可横向对比）
 * 见 [SceneIds]：深色栏 UI / 按钮UI / 液体 / 生物拟态。
 */
interface GlassPlugin {

    /** 插件静态元信息（展示与更新校验用）。 */
    val meta: PluginMeta

    /** 本插件提供的演示场景（固定四项，顺序即展示顺序）。 */
    val scenes: List<SceneMeta>

    /**
     * 插件初始化。[context] 是宿主传入的「插件资源上下文」—— getResources()
     * 已重定向到插件 APK 自身资源，插件应当缓存该 context 用于后续资源访问。
     */
    fun initialize(context: Context)

    /**
     * 创建演示场景根 View。
     *
     * @param context 宿主 Activity context（已包装为插件资源上下文）
     * @param sceneId 场景 ID，见 [SceneIds]
     * @return 全屏场景根 View；宿主负责挂 ViewTreeLifecycleOwner 等树属性
     */
    fun createScene(context: Context, sceneId: String): View

    /** 释放资源（宿主卸载插件时调用；进程死亡则不会调用）。 */
    fun dispose() {}
}

/** 插件静态元信息 —— 与 plugins.json 清单字段一一对应（宿主更新链路的比对基准）。 */
data class PluginMeta(
    /** 插件 ID，同时是 Gradle 模块名后缀与文件名（如 `kyant0`）。 */
    val id: String,
    /** 展示名（如 `Kyant0 · AndroidLiquidGlass`）。 */
    val title: String,
    /** 一句话定位。 */
    val subtitle: String,
    /** 集成的开源库坐标（如 `io.github.kyant0:backdrop:1.0.6`）。 */
    val libraryCoordinate: String,
    /** 开源仓库地址。 */
    val repoUrl: String,
    /** 开源协议（Apache-2.0 / MIT …）。 */
    val license: String,
    /** 上游作者署名。 */
    val author: String,
    /** 本插件 versionCode —— 与构建插件 APK 的 versionCode 一致。 */
    val versionCode: Int,
    /** 本插件 versionName。 */
    val versionName: String,
    /** 最低宿主 versionCode（宿主低于此值时插件拒绝安装）。 */
    val minHostVersionCode: Int
)

/** 演示场景元信息。 */
data class SceneMeta(
    /** 场景 ID（见 [SceneIds]）。 */
    val id: String,
    /** 中文展示名。 */
    val title: String,
    /** 一句话说明本场景考察该库的什么能力。 */
    val subtitle: String
)

/** 四大固定演示场景（五个插件统一实现，供横向对比）。 */
object SceneIds {
    /** 深色栏 UI：深色渐变背景 + 顶部玻璃栏 + 底部玻璃导航，滚动内容从玻璃后流过。 */
    const val DARK_BAR = "dark_bar"

    /** 按钮 UI：玻璃按钮矩阵（主/次/图标/开关/FAB），按压形变反馈。 */
    const val BUTTONS = "buttons"

    /** 液体：可拖拽玻璃液滴，弹性跟随 + 背景流动。 */
    const val LIQUID = "liquid"

    /** 生物拟态：有机形态（花瓣/细胞）呼吸群落的玻璃化。 */
    const val BIOMORPHIC = "biomorphic"

    val ALL = listOf(DARK_BAR, BUTTONS, LIQUID, BIOMORPHIC)
}

/** 宿主 ↔ 插件的共享常量。 */
object PluginConstants {
    /** 插件入口 meta-data 键。 */
    const val ENTRY_META_KEY = "com.ultraguru.liquidglass.PLUGIN_ENTRY"

    /** plugins.json 的 schema 版本。 */
    const val PLUGINS_SCHEMA = "liquid-glass-lab.plugins.v1"

    /** version.json 的 schema 版本（app 更新清单）。 */
    const val VERSION_SCHEMA = "liquid-glass-lab.version.v1"
}
