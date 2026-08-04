package com.qngxj.toolbox.util

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * APK 下载器：流式下载到应用缓存目录，支持进度回调。
 *
 * 下载完成后通过 [installApk] 触发系统安装意图（需 FileProvider 配置）。
 */
object ApkDownloader {

    /**
     * 下载进度回调。
     * @param percent 0-100，-1 表示无法计算总大小
     * @param downloadedBytes 已下载字节数
     * @param totalBytes 总字节数（未知为 -1）
     */
    data class Progress(val percent: Int, val downloadedBytes: Long, val totalBytes: Long)

    /**
     * 下载结果。
     */
    sealed class Result {
        data class Success(val file: File) : Result()
        data class Failed(val message: String) : Result()
    }

    /**
     * 下载 APK 到 [Context]'s externalCacheDir/downloads 目录。
     * @param url APK 下载地址
     * @param fileName 保存的文件名（如 "app-release.apk"）
     * @param onProgress 进度回调（在子线程）
     * @param onComplete 完成回调（在子线程）
     */
    fun download(
        ctx: Context,
        client: OkHttpClient,
        url: String,
        fileName: String,
        onProgress: (Progress) -> Unit,
        onComplete: (Result) -> Unit
    ) {
        Thread {
            var input: InputStream? = null
            var fos: FileOutputStream? = null
            try {
                val req = Request.Builder().url(url)
                    .header("User-Agent", "QuanNengGongJuXiang")
                    .header("Accept", "*/*")
                    .build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) {
                    onComplete(Result.Failed("下载失败：HTTP ${resp.code}"))
                    return@Thread
                }
                val body = resp.body ?: run {
                    onComplete(Result.Failed("下载失败：响应体为空"))
                    return@Thread
                }
                val total = body.contentLength() // -1 if unknown
                val dir = File(ctx.externalCacheDir, "updates").apply { mkdirs() }
                val outFile = File(dir, fileName)
                if (outFile.exists()) outFile.delete()

                input = body.byteStream()
                fos = FileOutputStream(outFile)
                val buffer = ByteArray(8192)
                var downloaded = 0L
                var lastReport = 0L
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    fos.write(buffer, 0, bytes)
                    downloaded += bytes
                    // 每 200KB 上报一次进度，避免过度刷新 UI
                    if (downloaded - lastReport >= 200 * 1024 || (total > 0 && downloaded == total)) {
                        lastReport = downloaded
                        val percent = if (total > 0) (downloaded * 100 / total).toInt() else -1
                        onProgress(Progress(percent, downloaded, total))
                    }
                }
                fos.flush()
                if (total > 0 && downloaded != total) {
                    onComplete(Result.Failed("下载不完整：${downloaded}/${total}"))
                    return@Thread
                }
                onComplete(Result.Success(outFile))
            } catch (e: Exception) {
                onComplete(Result.Failed(e.message ?: "下载异常"))
            } finally {
                try { input?.close() } catch (_: Exception) {}
                try { fos?.close() } catch (_: Exception) {}
            }
        }.start()
    }

    /**
     * 触发系统安装意图。调用方需确保已配置 FileProvider（authority = "${applicationId}.fileprovider"）。
     *
     * 兼容安卓 7.0 ~ 16：
     * - 安卓 7.0+：必须通过 FileProvider 共享文件
     * - 安卓 14+：安装未知来源应用需用户在系统设置中授权，系统会自动引导
     */
    fun installApk(ctx: Context, apkFile: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                apkFile
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            // 兜底：跳转到系统文件管理器
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                    type = "application/vnd.android.package-archive"
                    addCategory(android.content.Intent.CATEGORY_OPENABLE)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(android.content.Intent.createChooser(intent, "选择安装程序").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {
                // 最终兜底：静默忽略
            }
        }
    }

    /**
     * 清理旧的下载文件。
     */
    fun cleanOldDownloads(ctx: Context) {
        try {
            val dir = File(ctx.externalCacheDir, "updates")
            if (dir.exists()) dir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {
        }
    }
}
