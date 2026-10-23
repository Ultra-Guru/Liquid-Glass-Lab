package com.ultraguru.liquidglass.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.ultraguru.liquidglass.GlassLabApp
import com.ultraguru.liquidglass.ui.components.AuroraBackground
import com.ultraguru.liquidglass.ui.screen.AboutScreen
import com.ultraguru.liquidglass.ui.screen.LibraryScreen
import com.ultraguru.liquidglass.ui.screen.MainListScreen
import com.ultraguru.liquidglass.ui.screen.SceneHostScreen
import com.ultraguru.liquidglass.ui.screen.SettingsScreen
import com.ultraguru.liquidglass.ui.screen.UpdateHubScreen
import com.ultraguru.liquidglass.ui.theme.GlassLabTheme

/** 手工导航栈（无 navigation-compose 依赖，路径完全可控）。 */
sealed interface Screen {
    data object MainList : Screen
    data class Library(val id: String) : Screen
    data class Scene(val pluginId: String, val sceneId: String, val title: String) : Screen
    data object Updates : Screen
    data object Settings : Screen
    data object About : Screen
}

@Composable
fun AppRoot() {
    val settings by GlassLabApp.get().settings.state.collectAsState()
    GlassLabTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
        // 全局液体极光背景：所有页面透明叠在其上（场景页除外，插件自绘背景）
        Box(modifier = Modifier.fillMaxSize()) {
            AuroraBackground(modifier = Modifier.fillMaxSize().zIndex(0f))
            val stack = remember { mutableStateListOf<Screen>(Screen.MainList) }
            val current = stack.last()

            fun navigate(s: Screen) {
                if (stack.last() != s) stack.add(s)
            }

            BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

            Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
                AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        if (targetState != Screen.MainList) {
                            slideInHorizontally(tween(280)) { it / 3 } togetherWith
                                slideOutHorizontally(tween(280)) { -it / 4 }
                        } else {
                            slideInHorizontally(tween(280)) { -it / 4 } togetherWith
                                slideOutHorizontally(tween(280)) { it / 3 }
                        }
                    },
                    label = "nav"
                ) { screen ->
                    when (screen) {
                        Screen.MainList -> MainListScreen(
                            onOpenLibrary = { navigate(Screen.Library(it)) },
                            onOpenSettings = { navigate(Screen.Settings) },
                            onOpenAbout = { navigate(Screen.About) },
                            onOpenUpdates = { navigate(Screen.Updates) }
                        )
                        is Screen.Library -> LibraryScreen(
                            entryId = screen.id,
                            onBack = { stack.removeAt(stack.lastIndex) },
                            onOpenScene = { pluginId, sceneId, title ->
                                navigate(Screen.Scene(pluginId, sceneId, title))
                            }
                        )
                        is Screen.Scene -> SceneHostScreen(
                            pluginId = screen.pluginId,
                            sceneId = screen.sceneId,
                            title = screen.title,
                            onBack = { stack.removeAt(stack.lastIndex) }
                        )
                        Screen.Updates -> UpdateHubScreen(
                            onBack = { stack.removeAt(stack.lastIndex) },
                            onOpenSettings = { navigate(Screen.Settings) }
                        )
                        Screen.Settings -> SettingsScreen(
                            onBack = { stack.removeAt(stack.lastIndex) },
                            onOpenUpdates = { navigate(Screen.Updates) }
                        )
                        Screen.About -> AboutScreen(
                            onBack = { stack.removeAt(stack.lastIndex) },
                            onOpenUpdates = { navigate(Screen.Updates) }
                        )
                    }
                }
            }
        }
    }
}
