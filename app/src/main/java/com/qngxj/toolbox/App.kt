package com.qngxj.toolbox

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.qngxj.toolbox.util.Prefs
import rikka.shizuku.ShizukuProvider

class App : Application() {

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        // ShizukuProvider 在 Application.onCreate 之前实例化，其 onCreate 默认会尝试 Sui 初始化。
        // 本应用内置 Shizuku 服务端，不使用 Sui，在此提前禁用以避免启动时潜在崩溃。
        try {
            ShizukuProvider.disableAutomaticSuiInitialization()
        } catch (_: Throwable) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        applyTheme(Prefs.darkMode(this))
        // Shizuku SDK 通过 manifest 中的 ShizukuProvider 自动绑定，无需手动初始化
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
