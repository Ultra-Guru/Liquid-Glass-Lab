package com.ultraguru.liquidglass.plugin.qmdeve

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.ultraguru.liquidglass.plugin.api.GlassPlugin
import com.ultraguru.liquidglass.plugin.api.PluginMeta
import com.ultraguru.liquidglass.plugin.api.SceneIds
import com.ultraguru.liquidglass.plugin.api.SceneMeta
import com.ultraguru.liquidglass.plugin.qmdeve.scenes.BiomorphicScene
import com.ultraguru.liquidglass.plugin.qmdeve.scenes.ButtonsScene
import com.ultraguru.liquidglass.plugin.qmdeve.scenes.DarkBarScene
import com.ultraguru.liquidglass.plugin.qmdeve.scenes.LiquidScene

/**
 * 插件 3/5 —— QmDeve · AndroidLiquidGlassView（com.qmdeve.liquidglass:core:1.1.0）。
 *
 * 本插件是纯 View 体系：场景根直接返回自建 View 树（FrameLayout），
 * 不经 Compose —— GlassGlassView 的 bind/折射在原生 View 层完成。
 */
class QmdevePlugin : GlassPlugin {

    override val meta = PluginMeta(
        id = "qmdeve",
        title = "QmDeve · AndroidLiquidGlassView",
        subtitle = "View 体系真实折射",
        libraryCoordinate = "com.qmdeve.liquidglass:core:1.1.0",
        repoUrl = "https://github.com/QmDeve/AndroidLiquidGlassView",
        license = "MIT",
        author = "QmDeve",
        versionCode = 1,
        versionName = "0.1.0",
        minHostVersionCode = 1
    )

    override val scenes = listOf(
        SceneMeta(
            SceneIds.DARK_BAR,
            "深色栏 UI",
            "LiquidGlassView 玻璃栏 bind 滚动内容 · 折射随卡片流过"
        ),
        SceneMeta(
            SceneIds.BUTTONS,
            "按钮 UI",
            "玻璃按钮矩阵 · touchEffect 按压辉光与形变"
        ),
        SceneMeta(
            SceneIds.LIQUID,
            "液体",
            "可拖拽液滴 · draggable + elastic 弹簧物理"
        ),
        SceneMeta(
            SceneIds.BIOMORPHIC,
            "生物拟态",
            "玻璃细胞群 · setCornerRadius 实时形变 + 色散"
        )
    )

    override fun initialize(context: Context) = Unit

    override fun createScene(context: Context, sceneId: String): View =
        when (sceneId) {
            SceneIds.DARK_BAR -> DarkBarScene.create(context)
            SceneIds.BUTTONS -> ButtonsScene.create(context)
            SceneIds.LIQUID -> LiquidScene.create(context)
            SceneIds.BIOMORPHIC -> BiomorphicScene.create(context)
            else -> FallbackScene(context, sceneId)
        }
}

/** 未识别场景的兜底页（纯 View）。 */
internal fun FallbackScene(context: Context, sceneId: String): View =
    FrameLayout(context)
