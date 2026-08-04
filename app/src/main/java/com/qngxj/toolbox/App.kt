package com.qngxj.toolbox

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.qngxj.toolbox.util.Prefs

class App : MultiDexApplication() {

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
