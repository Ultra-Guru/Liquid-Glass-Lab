package com.ultraguru.liquidglass.plugin.qwea0

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
import com.ultraguru.liquidglass.plugin.qwea0.scenes.BiomorphicScene
import com.ultraguru.liquidglass.plugin.qwea0.scenes.ButtonsScene
import com.ultraguru.liquidglass.plugin.qwea0.scenes.DarkBarScene
import com.ultraguru.liquidglass.plugin.qwea0.scenes.LiquidScene

/**
 * 插件 4/5 —— QWEA0 · Liquid-Glass-Android（com.github.QWEA0:liquidglass:v2.0.11）。
 *
 * View 体系库（Kotlin + AGSL + NDK）：LiquidGlassView 是 FrameLayout 子类，
 * 捕获直接父容器 / backdropSource 的绘制内容做 SDF 折射 + 物理色散。
 * 场景实现见 scenes/：ComposeView 外壳 + AndroidView 挂 FrameLayout
 * （玻璃必须是背景视图的兄弟节点，这是本库的采样模型）。
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
        versionCode = BuildConfigLite.VERSION_CODE,
        versionName = BuildConfigLite.VERSION_NAME,
        minHostVersionCode = 1
    )

    override val scenes = listOf(
        SceneMeta(
            id = SceneIds.DARK_BAR,
            title = "深色栏 UI",
            subtitle = "顶栏 + 底部 LiquidGlassTabBar 玻璃化 · 内容从玻璃后流过（动态背景重捕获）"
        ),
        SceneMeta(
            id = SceneIds.BUTTONS,
            title = "按钮 UI",
            subtitle = "LiquidGlassButton / Fab / Chip 组件矩阵 · 内建 iOS26 按压液态形变"
        ),
        SceneMeta(
            id = SceneIds.LIQUID,
            title = "液体",
            subtitle = "可拖拽玻璃液滴 · 弹性拉伸 + 传感器高光 + 卫星滴黏连追赶"
        ),
        SceneMeta(
            id = SceneIds.BIOMORPHIC,
            title = "生物拟态",
            subtitle = "setSecondaryShape 水银 smin 融合 + 逐角圆角呼吸细胞群"
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
