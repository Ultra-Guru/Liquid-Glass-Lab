package com.ultraguru.liquidglass

import android.app.Application
import com.ultraguru.liquidglass.data.SettingsStore
import com.ultraguru.liquidglass.plugin.PluginRegistry

class GlassLabApp : Application() {

    lateinit var settings: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        instance = this
        // 启动即扫描已安装插件（磁盘目录 → 注册表），主列表的状态源
        PluginRegistry.bootstrap(this)
    }

    companion object {
        @Volatile
        private var instance: GlassLabApp? = null

        fun get(): GlassLabApp =
            instance ?: error("GlassLabApp not created yet")
    }
}
