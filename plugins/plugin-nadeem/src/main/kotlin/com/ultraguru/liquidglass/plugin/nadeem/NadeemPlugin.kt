package com.ultraguru.liquidglass.plugin.nadeem

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
import com.ultraguru.liquidglass.plugin.nadeem.scenes.BiomorphicScene
import com.ultraguru.liquidglass.plugin.nadeem.scenes.ButtonsScene
import com.ultraguru.liquidglass.plugin.nadeem.scenes.DarkBarScene
import com.ultraguru.liquidglass.plugin.nadeem.scenes.LiquidScene

/**
 * 插件 5/5 —— NadeemIqbal · liquid-glass（io.github.nadeemiqbal:liquid-glass:0.2.3）。
 *
 * Compose 库：liquidGlassSource(state) 标记背景层 + Modifier.liquidGlass /
 * GlassCard / GlassButton / GlassNavBar 组件做玻璃面；三档质量自动降级
 * （Full / Medium / Fallback），refraction 走 AGSL（API 33+）。
 * 场景实现见 scenes/。
 */
class NadeemPlugin : GlassPlugin {

    override val meta = PluginMeta(
        id = "nadeem",
        title = "NadeemIqbal · liquid-glass",
        subtitle = "组件化 Glass UI",
        libraryCoordinate = "io.github.nadeemiqbal:liquid-glass:0.2.3",
        repoUrl = "https://github.com/NadeemIqbal/liquid-glass",
        license = "Apache-2.0",
        author = "Nadeem Iqbal",
        versionCode = BuildConfigLite.VERSION_CODE,
        versionName = BuildConfigLite.VERSION_NAME,
        minHostVersionCode = 1
    )

    override val scenes = listOf(
        SceneMeta(
            id = SceneIds.DARK_BAR,
            title = "深色栏 UI",
            subtitle = "GlassNavBar 顶栏 + GlassCard 底部导航 · 背景模糊随滚动内容更新"
        ),
        SceneMeta(
            id = SceneIds.BUTTONS,
            title = "按钮 UI",
            subtitle = "GlassButton 组件 + liquidGlass 修饰符 · 按压弹性形变矩阵"
        ),
        SceneMeta(
            id = SceneIds.LIQUID,
            title = "液体",
            subtitle = "可拖拽液滴 · spring 跟随 · refraction 折射 + 尾随卫星滴"
        ),
        SceneMeta(
            id = SceneIds.BIOMORPHIC,
            title = "生物拟态",
            subtitle = "GlassCard 有机形状呼吸群落 · DynamicSheen 边缘光"
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

/** 版本号常量（与 build.gradle.kts 注入保持一致，避免引入 buildConfig 生成物）。 */
internal object BuildConfigLite {
    const val VERSION_CODE = 1
    const val VERSION_NAME = "0.1.0"
}
