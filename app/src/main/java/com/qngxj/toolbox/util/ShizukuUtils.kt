package com.qngxj.toolbox.util

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Shizuku 集成（SDK 内置，无需用户额外下载库）。
 * 真实授权检测：监听 Shizuku 状态与权限回调。
 *
 * 注意：Shizuku 服务的“启动”仍需用户通过 Shizuku 应用或 adb 激活，
 * 本 App 内置 SDK 与 Provider，负责检测与请求授权。
 */
object ShizukuUtils {

    const val SHIZUKU_REQUEST_CODE = 1001

    enum class State(val label: String) {
        UNKNOWN("未知"),
        NOT_RUNNING("未运行"),
        RUNNING_UNAUTHORIZED("运行中（未授权）"),
        AUTHORIZED("已授权")
    }

    fun isShizukuInstalled(ctx: Context): Boolean {
        return try {
            ctx.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun state(): State {
        return try {
            if (!Shizuku.pingBinder()) State.NOT_RUNNING
            else if (checkPermission()) State.AUTHORIZED
            else State.RUNNING_UNAUTHORIZED
        } catch (e: Exception) {
            State.NOT_RUNNING
        }
    }

    fun checkPermission(): Boolean {
        return if (!Shizuku.pingBinder()) false
        else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    /** 请求授权（异步，结果通过 listener 回调）。先注册监听再发起请求。 */
    fun requestPermission(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.addRequestPermissionResultListener(listener)
        try {
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
        } catch (e: Exception) {
            try { Shizuku.removeRequestPermissionResultListener(listener) } catch (_: Exception) {}
        }
    }

    fun addBinderReceivedListener(listener: Shizuku.OnBinderReceivedListener) {
        Shizuku.addBinderReceivedListenerSticky(listener)
    }

    fun addBinderDeadListener(listener: Shizuku.OnBinderDeadListener) {
        Shizuku.addBinderDeadListener(listener)
    }

    fun version(): String {
        return try {
            if (Shizuku.pingBinder()) "v" + Shizuku.getVersion() else "未运行"
        } catch (e: Exception) {
            "未知"
        }
    }
}
