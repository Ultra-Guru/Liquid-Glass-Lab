package com.ultraguru.liquidglass.plugin.cloudy

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
 * 插件 X/5 —— skydoves · Cloudy（com.github.skydoves:cloudy:1.0.0-alpha01）。
 *
 * 四大场景实现见 scenes/（深色栏 / 按钮 / 液体 / 生物拟态）。
 */
class CloudyPlugin : GlassPlugin {

    override val meta = PluginMeta(
        id = "cloudy",
        title = "skydoves · Cloudy",
        subtitle = "GPU 加速模糊 + 液态玻璃",
        libraryCoordinate = "com.github.skydoves:cloudy:1.0.0-alpha01",
        repoUrl = "https://github.com/skydoves/Cloudy",
        license = "Apache-2.0",
        author = "skydoves",
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
