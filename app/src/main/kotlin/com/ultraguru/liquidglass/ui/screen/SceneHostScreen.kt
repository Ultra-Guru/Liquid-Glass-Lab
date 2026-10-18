package com.ultraguru.liquidglass.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.ultraguru.liquidglass.plugin.PluginRegistry

/**
 * 场景宿主页：把插件的 View 树嵌入 Compose。
 *
 * 关键 interop：插件返回的（常为 ComposeView 的）根 View 需要宿主的
 * LifecycleOwner / SavedStateRegistryOwner 才能启动 Compose 组合 ——
 * 在 AndroidView 工厂里显式挂上（经典 interop 配方）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneHostScreen(
    pluginId: String,
    sceneId: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedStateOwner = LocalSavedStateRegistryOwner.current
    var error by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    val sceneView = remember(pluginId, sceneId) {
        runCatching {
            PluginRegistry.createSceneView(context, pluginId, sceneId)
        }.getOrElse { e ->
            error = "${e.javaClass.simpleName}: ${e.message}"
            null
        }
    }

    DisposableEffect(sceneView) {
        onDispose {
            // 离开页面即销毁插件组合，防止跨场景的泄漏
            (sceneView as? android.view.ViewGroup)?.removeAllViews()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Surface(
            color = Color(0xB3000000),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        "插件：$pluginId · 场景：$sceneId",
                        color = Color(0x99FFFFFF),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.size(8.dp))
            }
        }

        Box(Modifier.fillMaxSize()) {
            if (sceneView != null) {
                AndroidView(
                    factory = { ctx ->
                        sceneView.apply {
                            setViewTreeLifecycleOwner(lifecycleOwner)
                            setViewTreeSavedStateRegistryOwner(savedStateOwner)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("场景加载失败", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.size(8.dp))
                    Text(error ?: "未知错误", color = Color(0x99FFFFFF), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
