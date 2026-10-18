package com.ultraguru.liquidglass.plugin.cloudy

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.ultraguru.liquidglass.plugin.api.GlassPlugin
import com.ultraguru.liquidglass.plugin.api.PluginMeta
import com.ultraguru.liquidglass.plugin.api.SceneIds
import com.ultraguru.liquidglass.plugin.api.SceneMeta
import com.ultraguru.liquidglass.plugin.cloudy.scenes.BiomorphicScene
import com.ultraguru.liquidglass.plugin.cloudy.scenes.ButtonsScene
import com.ultraguru.liquidglass.plugin.cloudy.scenes.DarkBarScene
import com.ultraguru.liquidglass.plugin.cloudy.scenes.LiquidScene

/**
 * 插件 2/5 —— skydoves · Cloudy（com.github.skydoves:cloudy:1.0.0-alpha01）。
 *
 * 演示 sky/cloudy 背景模糊（渐进模糊）+ liquidGlass 透镜折射 +
 * 陀螺仪动态光源在四场景下的标准用法，实现见 scenes/。
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
        SceneMeta(
            SceneIds.DARK_BAR,
            "深色栏 UI",
            "顶栏 + 底部导航玻璃化 · 渐进模糊 Progressive（内容从玻璃后流过）"
        ),
        SceneMeta(
            SceneIds.BUTTONS,
            "按钮 UI",
            "cloudy 磨砂按钮 + liquidGlass 高光按钮 · 按压缩放反馈"
        ),
        SceneMeta(
            SceneIds.LIQUID,
            "液体",
            "可拖拽液滴 · liquidGlass 透镜折射 + 陀螺仪动态光源"
        ),
        SceneMeta(
            SceneIds.BIOMORPHIC,
            "生物拟态",
            "有机细胞群 · 磨砂玻璃裁剪 + 动态高光游动"
        )
    )

    override fun initialize(context: Context) = Unit

    override fun createScene(context: Context, sceneId: String): View =
        ComposeView(context).apply {
            setContent {
                when (sceneId) {
                    SceneIds.DARK_BAR -> DarkBarScene()
                    SceneIds.BUTTONS -> ButtonsScene()
                    SceneIds.LIQUID -> LiquidScene()
                    SceneIds.BIOMORPHIC -> BiomorphicScene()
                    else -> FallbackScene(sceneId)
                }
            }
        }
}

/** 未识别场景的兜底页。 */
@Composable
internal fun FallbackScene(sceneId: String) {
    Box(Modifier.fillMaxSize())
}
