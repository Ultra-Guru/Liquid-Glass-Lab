package com.ultraguru.liquidglass

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import com.ultraguru.liquidglass.data.SettingsStore
import com.ultraguru.liquidglass.plugin.PluginRegistry
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GlassLabApp : Application() {

    lateinit var settings: SettingsStore
        private set

    /** 系统插件导入的后台单线程（磁盘拷贝 + 索引写入，串行即可）。 */
    private lateinit var adoptExecutor: ExecutorService

    /** onResume 节流：4 秒内不重复扫描。 */
    @Volatile
    private var lastAdoptAt = 0L

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        instance = this
        // 启动即扫描已安装插件（磁盘目录 → 注册表），主列表的状态源
        PluginRegistry.bootstrap(this)

        adoptExecutor = Executors.newSingleThreadExecutor { r ->
            Thread(r, "plugin-adopt").apply { isDaemon = true }
        }

        // 关键修复：用户在系统安装器装完插件 APK 回到 App 的那一刻，
        // 自动把系统已装插件导入注册表 —— 无需手动刷新，状态立即变「已安装」。
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastAdoptAt < 4_000L) return
                lastAdoptAt = now
                adoptExecutor.execute {
                    runCatching {
                        val n = PluginRegistry.adoptSystemInstalledPlugins()
                        if (n > 0) PluginRegistry.diag("onResume 自动导入 $n 个系统已装插件")
                    }
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    /** 手动触发一次系统插件导入（设置页按钮；后台执行）。 */
    fun adoptSystemPluginsNow(onDone: (Int) -> Unit = {}) {
        adoptExecutor.execute {
            val n = runCatching { PluginRegistry.adoptSystemInstalledPlugins() }.getOrDefault(0)
            onDone(n)
        }
    }

    companion object {
        @Volatile
        private var instance: GlassLabApp? = null

        fun get(): GlassLabApp =
            instance ?: error("GlassLabApp not created yet")

        /** 当前宿主 versionCode（各屏共用）。 */
        fun hostVersionCode(): Int = runCatching {
            val ctx = get()
            @Suppress("DEPRECATION")
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode
        }.getOrDefault(1)

        /** 当前宿主 versionName（各屏共用）。 */
        fun hostVersionName(): String = runCatching {
            val ctx = get()
            @Suppress("DEPRECATION")
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
        }.getOrDefault("?")
    }
}
