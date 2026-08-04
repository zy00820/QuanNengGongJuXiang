package com.qngxj.toolbox.util

import android.content.Context
import com.qngxj.toolbox.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 检查更新（联网，仅用户主动触发）。
 *
 * 默认查询 GitHub Releases API，解析 tag_name / assets 进行版本对比与下载。
 * 更新源格式（GET 返回 JSON，兼容 GitHub Releases）：
 * {
 *   "tag_name": "v1.1.3",
 *   "name": "全能工具箱 V1.1.3",
 *   "body": "更新说明",
 *   "assets": [{"browser_download_url": "https://...apk", "name": "app-release.apk", "size": 5603313}]
 * }
 *
 * 也兼容自定义 JSON 源：
 * { "versionName":"1.1.3","versionCode":1113,"url":"https://...apk","note":"..."}
 */
object UpdateChecker {

    // 默认更新源：本项目 GitHub 仓库 Releases（公开仓库无需鉴权即可读取）
    const val DEFAULT_UPDATE_URL =
        "https://api.github.com/repos/zy00820/QuanNengGongJuXiang/releases/latest"

    fun currentVersionName(): String = BuildConfig.VERSION_NAME
    fun currentVersionCode(): Int = BuildConfig.VERSION_CODE

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val versionName: String,
        val versionCode: Int,
        val downloadUrl: String,
        val note: String,
        val fileSize: Long,
        val error: String?
    )

    /**
     * 检查更新。回调在调用线程（内部已切到子线程，回调时通过 runOnUiThread 切回主线程）。
     */
    fun check(ctx: Context, callback: (UpdateInfo) -> Unit) {
        // 网络可用性预检
        if (!isNetworkAvailable(ctx)) {
            callback(UpdateInfo(false, "", 0, "", "", 0, "当前无网络连接"))
            return
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        val url = Prefs.updateUrl(ctx) ?: DEFAULT_UPDATE_URL
        Thread {
            try {
                val req = Request.Builder().url(url)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "QuanNengGongJuXiang/${BuildConfig.VERSION_NAME}")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        callback(UpdateInfo(false, "", 0, "", "", 0, "服务器返回 HTTP ${resp.code}"))
                        return@Thread
                    }
                    val body = resp.body?.string() ?: ""
                    if (body.isBlank()) {
                        callback(UpdateInfo(false, "", 0, "", "", 0, "服务器返回内容为空"))
                        return@Thread
                    }
                    val json = JSONObject(body)

                    // 兼容两种格式：GitHub Releases / 自定义 JSON
                    val tag = json.optString("tag_name", "").removePrefix("v").removePrefix("V")
                    val remoteName = json.optString("name", json.optString("versionName", tag)).trim()
                    val remoteCode = json.optInt("versionCode", 0)
                    val note = json.optString("body", json.optString("note", "")).trim()

                    // 解析下载链接与文件大小
                    var dlUrl = json.optString("url", "")
                    var size = 0L
                    if (dlUrl.isEmpty()) {
                        val assets = json.optJSONArray("assets")
                        if (assets != null && assets.length() > 0) {
                            // 优先选择 .apk 资源
                            var apkAsset: JSONObject? = null
                            for (i in 0 until assets.length()) {
                                val a = assets.optJSONObject(i) ?: continue
                                val name = a.optString("name", "")
                                if (name.endsWith(".apk", ignoreCase = true)) {
                                    apkAsset = a
                                    break
                                }
                            }
                            if (apkAsset == null) apkAsset = assets.optJSONObject(0)
                            dlUrl = apkAsset?.optString("browser_download_url", "") ?: ""
                            size = apkAsset?.optLong("size", 0L) ?: 0L
                        }
                    }
                    if (dlUrl.isEmpty()) {
                        dlUrl = json.optString("html_url", "")
                    }

                    // 版本对比：自定义源带 versionCode 时直接比较；GitHub Releases 无 versionCode，
                    // 用语义化版本号比较（tag_name 形如 "1.1.3"）
                    val hasUpdate = if (remoteCode > 0) {
                        remoteCode > currentVersionCode()
                    } else {
                        // GitHub Releases：用 tag_name（已去 v 前缀）或 name 与当前版本号比较
                        val remoteVer = if (tag.isNotEmpty()) tag else remoteName
                        isNewerVersion(remoteVer, currentVersionName())
                    }

                    callback(UpdateInfo(hasUpdate, remoteName, remoteCode, dlUrl, note, size, null))
                }
            } catch (e: Exception) {
                callback(UpdateInfo(false, "", 0, "", "", 0, e.message ?: "网络异常"))
            }
        }.start()
    }

    /**
     * 语义化版本比较：remote > current 返回 true。
     * 支持 "1.1.3"、"v1.1.3"、"1.1.3-beta" 等。
     */
    private fun isNewerVersion(remote: String, current: String): Boolean {
        fun v(s: String): List<Int> {
            val cleaned = s.replace(Regex("[^0-9.]"), "")
            return cleaned.split(".").mapNotNull { it.toIntOrNull() }
        }
        val r = v(remote)
        val c = v(current)
        if (r.isEmpty() || c.isEmpty()) return false
        for (i in 0 until maxOf(r.size, c.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a > b) return true
            if (a < b) return false
        }
        return false
    }

    /**
     * 网络可用性检测（不发起实际连接，仅检查活动网络状态）。
     */
    private fun isNetworkAvailable(ctx: Context): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager
            val active = cm.activeNetworkInfo
            active != null && active.isConnected
        } catch (e: Exception) {
            true // 检测失败不阻断，交由实际请求判定
        }
    }

    /**
     * 构造下载客户端（带较长超时，用于 APK 下载）。
     */
    fun downloadClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * 构造下载请求。
     */
    fun downloadRequest(url: String): Request = Request.Builder()
        .url(url)
        .header("User-Agent", "QuanNengGongJuXiang/${BuildConfig.VERSION_NAME}")
        .header("Accept", "*/*")
        .build()
}
