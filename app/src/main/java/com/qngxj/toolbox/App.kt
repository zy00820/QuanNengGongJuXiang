package com.qngxj.toolbox

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.qngxj.toolbox.util.Prefs
import rikka.shizuku.ShizukuProvider

class App : Application() {

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        // 必须在 ContentProvider.onCreate 之前禁用 Sui 自动初始化，
        // 否则 ShizukuProvider.onCreate 会调用 Sui.init() 触发 transact，
        // 在未安装 Sui/Riru 的设备上可能被系统安全策略直接杀进程导致启动崩溃。
        // minSdk 29 使用 ART 运行时，多 DEX 跨包类引用安全。
        runCatching { ShizukuProvider.disableAutomaticSuiInitialization() }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        applyTheme(Prefs.darkMode(this))
    }

    companion object {
        lateinit var instance: App
            private set

        fun applyTheme(mode: Int) {
            when (mode) {
                1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}
