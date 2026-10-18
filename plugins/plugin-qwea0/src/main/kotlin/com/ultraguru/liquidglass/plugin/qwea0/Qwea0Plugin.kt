package com.ultraguru.liquidglass.plugin.qwea0

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView
import com.ultraguru.liquidglass.plugin.api.GlassPlugin
import com.ultraguru.liquidglass.plugin.api.PluginMeta
import com.ultraguru.liquidglass.plugin.api.SceneIds
import com.ultraguru.liquidglass.plugin.api.SceneMeta

/**
 * 插件 X/5 —— QWEA0 · Liquid-Glass-Android（com.github.QWEA0:liquidglass:v2.0.11）。
 *
 * 四大场景实现见 scenes/（深色栏 / 按钮 / 液体 / 生物拟态）。
 */
class Qwea0Plugin : GlassPlugin {

    override val meta = PluginMeta(
        id = "qwea0",
        title = "QWEA0 · Liquid-Glass-Android",
        subtitle = "SDF 折射 + 传感器视差",
        libraryCoordinate = "com.github.QWEA0:liquidglass:v2.0.11",
        repoUrl = "https://github.com/QWEA0/Liquid-Glass-Android",
        license = "MIT",
        author = "QWEA0",
        versionCode = 1,
        versionName = "0.1.0",
        minHostVersionCode = 1
    )

    override val scenes = listOf(
        SceneMeta(SceneIds.DARK_BAR, "深色栏 UI", "顶栏 + 底部导航玻璃化（Cloudy 渐进模糊 / 折射）"),
        SceneMeta(SceneIds.BUTTONS, "按钮 UI", "玻璃按钮矩阵 + 按压反馈"),
        SceneMeta(SceneIds.LIQUID, "液体", "可拖拽弹性液滴"),
        SceneMeta(SceneIds.BIOMORPHIC, "生物拟态", "有机形态呼吸群落")
    )

    override fun initialize(context: Context) = Unit

    override fun createScene(context: Context, sceneId: String): View =
        placeholder(context, sceneId)

    private fun placeholder(context: Context, sceneId: String): View =
        TextView(context).apply {
            text = "场景[$sceneId]\n实现填充中（阶段 B 并行开发）"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF100404.toInt())
        }
}
