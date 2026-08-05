package com.qngxj.toolbox.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import java.io.File

/**
 * Shizuku 集成（SDK 内置，无需用户额外下载库）。
 * 真实授权检测：监听 Shizuku 状态与权限回调。
 *
 * V1.1.7 起：内置 Shizuku 服务端 APK（assets/shizuku.apk），
 * 在已 root 设备上可通过 app_process 直接启动服务，无需安装 Shizuku 应用。
 */
object ShizukuUtils {

    /** 内置 Shizuku APK 在 assets 中的文件名 */
    private const val BUNDLED_APK_NAME = "shizuku.apk"

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

    /**
     * 通过 Shizuku 执行 shell 命令（需已授权）。
     * @param cmd 命令字符串（如 "settings put global extreme_mode 1"）
     * @return Pair(success, output)，success 为是否执行成功
     */
    fun execCommand(cmd: String): Pair<Boolean, String> {
        return try {
            if (!checkPermission()) return Pair(false, "Shizuku 未授权")
            // 通过 IShizukuService 直接调用 newProcess（shell 权限执行）
            val binder = Shizuku.getBinder() ?: return Pair(false, "Shizuku 服务不可用")
            val wrapped = ShizukuBinderWrapper(binder)
            val service = IShizukuService.Stub.asInterface(wrapped)
            val remoteProcess = service.newProcess(arrayOf("sh", "-c", cmd), null, null)
            // 读取输出与错误流（ParcelFileDescriptor → FileInputStream）
            val input = readPfd(remoteProcess.inputStream)
            val err = readPfd(remoteProcess.errorStream)
            val code = remoteProcess.waitFor()
            val out = if (input.isNotEmpty()) input else err
            Pair(code == 0, out)
        } catch (e: Exception) {
            Pair(false, e.message ?: "执行异常")
        }
    }

    /** 将 ParcelFileDescriptor 的内容读为字符串 */
    private fun readPfd(pfd: android.os.ParcelFileDescriptor): String {
        return try {
            val fis = java.io.FileInputStream(pfd.fileDescriptor)
            fis.use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            ""
        } finally {
            try { pfd.close() } catch (_: Exception) {}
        }
    }

    // ==================== 内置 Shizuku 服务端（V1.1.7） ====================

    /**
     * 将内置 Shizuku APK 从 assets 释放到应用私有目录。
     * @return 释放后的 APK 文件路径，失败返回 null
     */
    fun extractBundledApk(ctx: Context): String? {
        return try {
            val outFile = File(ctx.filesDir, BUNDLED_APK_NAME)
            // 已存在且大小一致则跳过
            val assetSize = ctx.assets.openFd(BUNDLED_APK_NAME).use { it.length }
            if (outFile.exists() && outFile.length() == assetSize) {
                return outFile.absolutePath
            }
            ctx.assets.open(BUNDLED_APK_NAME).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 通过 Root 启动内置 Shizuku 服务端（无需安装 Shizuku 应用）。
     *
     * 原理：通过 su 执行 app_process，以 APK 作为 classpath 启动
     * moe.shizuku.starter.ServiceStarter，由其初始化 rikka.shizuku.server.ShizukuService。
     *
     * @param ctx 上下文
     * @return Pair(success, message)
     */
    fun startServiceViaRoot(ctx: Context): Pair<Boolean, String> {
        // 1. 检测 root
        if (!RootUtils.isRooted()) {
            return Pair(false, "设备未 Root，无法直接启动服务\n请使用一键安装内置 Shizuku 后通过 adb 激活")
        }
        // 2. 释放内置 APK
        val apkPath = extractBundledApk(ctx)
            ?: return Pair(false, "内置 Shizuku APK 释放失败")
        // 3. 构造启动命令（优先 app_process64）
        val cmd = buildString {
            append("APPPROCESS=/system/bin/app_process;")
            append("[ -e /system/bin/app_process64 ] && APPPROCESS=/system/bin/app_process64;")
            append("nohup \$APPPROCESS -Djava.class.path=$apkPath /system/bin ")
            append("--nice-name=shizuku_server moe.shizuku.starter.ServiceStarter ")
            append(">/dev/null 2>&1 &")
        }
        // 4. 执行
        val (ok, out) = RootUtils.execRoot(cmd)
        return if (ok) {
            // 等待服务就绪
            Thread.sleep(800)
            Pair(true, "Shizuku 服务启动命令已执行")
        } else {
            Pair(false, "启动失败：${if (out.isNotEmpty()) out else "su 执行异常"}")
        }
    }

    /**
     * 一键安装内置 Shizuku APK（无 Root 设备的回退方案）。
     * 触发系统安装器，用户确认后安装 Shizuku 应用。
     */
    fun installBundledApk(ctx: Context): Boolean {
        return try {
            val apkPath = extractBundledApk(ctx) ?: return false
            val apkFile = File(apkPath)
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 打开已安装的 Shizuku 应用（若存在）。
     */
    fun openShizukuApp(ctx: Context): Boolean {
        return try {
            val intent = ctx.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
}
