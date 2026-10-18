package com.ultraguru.liquidglass.plugin.kyant0

import android.content.Context
import android.view.View
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.ultraguru.liquidglass.plugin.api.GlassPlugin
import com.ultraguru.liquidglass.plugin.api.PluginMeta
import com.ultraguru.liquidglass.plugin.api.SceneIds
import com.ultraguru.liquidglass.plugin.api.SceneMeta

/**
 * 插件 1/5 —— Kyant0/AndroidLiquidGlass（io.github.kyant0:backdrop:1.0.6）。
 *
 * 参考级插件实现：演示 layerBackdrop + drawBackdrop + effects DSL
 * （vibrancy/blur/lens 折射）在四场景下的标准用法。
 */
class Kyant0Plugin : GlassPlugin {

    override val meta = PluginMeta(
        id = "kyant0",
        title = "Kyant0 · AndroidLiquidGlass",
        subtitle = "Compose 液态玻璃标杆",
        libraryCoordinate = "io.github.kyant0:backdrop:1.0.6",
        repoUrl = "https://github.com/Kyant0/AndroidLiquidGlass",
        license = "Apache-2.0",
        author = "Kyant0",
        versionCode = BuildConfigLite.VERSION_CODE,
        versionName = BuildConfigLite.VERSION_NAME,
        minHostVersionCode = 1
    )

    override val scenes = listOf(
        SceneMeta(
            id = SceneIds.DARK_BAR,
            title = "深色栏 UI",
            subtitle = "顶栏 + 底部导航玻璃化 · 内容从玻璃后流过（vibrancy + blur + lens 折射）"
        ),
        SceneMeta(
            id = SceneIds.BUTTONS,
            title = "按钮 UI",
            subtitle = "胶囊玻璃按钮 + 按压 tanh 形变 + 开关与图标按钮"
        ),
        SceneMeta(
            id = SceneIds.LIQUID,
            title = "液体",
            subtitle = "可拖拽玻璃液滴 · 弹性跟随 · 色散折射（chromaticAberration）"
        ),
        SceneMeta(
            id = SceneIds.BIOMORPHIC,
            title = "生物拟态",
            subtitle = "有机极坐标形状 + 呼吸缩放 + Ambient 高光的玻璃细胞群"
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

// ── 公共小件 ──────────────────────────────────────────────────────────

/** 未识别场景的兜底页。 */
@Composable
internal fun FallbackScene(sceneId: String) {
    Box(Modifier.fillMaxSize())
}

/** 深色玻璃面板的统一表面色调（iOS 控制中心风）。 */
internal val GlassSurfaceTint = Color.Black.copy(alpha = 0.18f)

/** 版本号常量（与 build.gradle.kts 注入保持一致，避免引入 buildConfig 生成物）。 */
internal object BuildConfigLite {
    const val VERSION_CODE = 1
    const val VERSION_NAME = "0.1.0"
}
