package com.qngxj.toolbox.util

import android.content.Context
import com.qngxj.toolbox.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 检查更新（联网，仅用户主动触发）。
 * 默认查询 GitHub Releases；可在 prefs 中配置自定义更新源。
 *
 * 更新源格式（GET 返回 JSON）：
 * { "versionName":"1.1.3","versionCode":1113,"url":"https://...apk","note":"..."}
 */
object UpdateChecker {

    // 默认更新源（GitHub 仓库地址，用户后续提供后可在此替换）
    const val DEFAULT_UPDATE_URL =
        "https://api.github.com/repos/qngxj/QuanNengGongJuXiang/releases/latest"

    fun currentVersionName(): String = BuildConfig.VERSION_NAME
    fun currentVersionCode(): Int = BuildConfig.VERSION_CODE

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val versionName: String,
        val versionCode: Int,
        val downloadUrl: String,
        val note: String,
        val error: String?
    )

    fun check(ctx: Context, callback: (UpdateInfo) -> Unit) {
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
                        callback(UpdateInfo(false, "", 0, "", "", "HTTP ${resp.code}"))
                        return@Thread
                    }
                    val body = resp.body?.string() ?: ""
                    val json = JSONObject(body)
                    // GitHub releases 适配
                    val tag = json.optString("tag_name", "").removePrefix("v").removePrefix("V")
                    val name = json.optString("name", tag)
                    val note = json.optString("body", "")
                    var dlUrl = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null && assets.length() > 0) {
                        val asset = assets.optJSONObject(0)
                        dlUrl = asset?.optString("browser_download_url", "") ?: ""
                    }
                    if (dlUrl.isEmpty()) dlUrl = json.optString("html_url", "")
                    val remoteCode = try { tag.replace(".", "").toIntOrNull() ?: 0 } catch (e: Exception) { 0 }
                    val hasUpdate = remoteCode > currentVersionCode() || isNewerVersion(name, currentVersionName())
                    callback(UpdateInfo(hasUpdate, name, remoteCode, dlUrl, note, null))
                }
            } catch (e: Exception) {
                callback(UpdateInfo(false, "", 0, "", "", e.message ?: "网络异常"))
            }
        }.start()
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        fun v(s: String) = s.split(".").mapNotNull { it.toIntOrNull() }
        val r = v(remote.replace(Regex("[^0-9.]"), ""))
        val c = v(current.replace(Regex("[^0-9.]"), ""))
        for (i in 0 until maxOf(r.size, c.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a > b) return true
            if (a < b) return false
        }
        return false
    }
}
