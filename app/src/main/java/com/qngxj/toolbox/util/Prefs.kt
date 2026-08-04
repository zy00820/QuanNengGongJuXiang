package com.qngxj.toolbox.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 轻量偏好封装。文件名 license.xml 独立，便于备份排除。
 */
object Prefs {
    private const val MAIN = "qngxj_prefs"
    private const val LICENSE = "license"

    private fun main(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(MAIN, Context.MODE_PRIVATE)

    private fun lic(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(LICENSE, Context.MODE_PRIVATE)

    // ---------- 深色模式 ----------
    const val DARK_MODE = "dark_mode" // 0=跟随系统 1=浅色 2=深色
    fun darkMode(ctx: Context): Int = main(ctx).getInt(DARK_MODE, 0)
    fun setDarkMode(ctx: Context, mode: Int) = main(ctx).edit().putInt(DARK_MODE, mode).apply()

    // ---------- 通知 ----------
    const val NOTIFY_UPDATE = "notify_update"
    fun notifyUpdate(ctx: Context): Boolean = main(ctx).getBoolean(NOTIFY_UPDATE, true)
    fun setNotifyUpdate(ctx: Context, v: Boolean) = main(ctx).edit().putBoolean(NOTIFY_UPDATE, v).apply()

    // ---------- 检查更新源 ----------
    const val PREF_UPDATE_URL = "update_url"
    fun updateUrl(ctx: Context): String? = main(ctx).getString(PREF_UPDATE_URL, null)
    fun setUpdateUrl(ctx: Context, url: String?) {
        val e = main(ctx).edit()
        if (url.isNullOrEmpty()) e.remove(PREF_UPDATE_URL) else e.putString(PREF_UPDATE_URL, url)
        e.apply()
    }

    // ---------- 会员 ----------
    const val KEY_TIER = "tier"            // 0=未激活 1=lite 2=pro
    const val KEY_DEVICE = "device_code"
    const val KEY_USED_CODES = "used_codes"
    const val KEY_ACTIVATED_CODE = "act_code"

    fun tier(ctx: Context): Int = lic(ctx).getInt(KEY_TIER, 0)
    fun setTier(ctx: Context, t: Int) = lic(ctx).edit().putInt(KEY_TIER, t).apply()

    fun deviceCode(ctx: Context): String =
        lic(ctx).getString(KEY_DEVICE, null) ?: run {
            val dc = DeviceUtils.generateDeviceCode(ctx)
            lic(ctx).edit().putString(KEY_DEVICE, dc).apply()
            dc
        }

    fun usedCodes(ctx: Context): MutableSet<String> =
        lic(ctx).getStringSet(KEY_USED_CODES, emptySet<String>())?.toMutableSet() ?: mutableSetOf()

    fun markUsed(ctx: Context, codeSha: String) {
        val s = usedCodes(ctx)
        s.add(codeSha)
        lic(ctx).edit().putStringSet(KEY_USED_CODES, s).apply()
    }

    fun activatedCode(ctx: Context): String? = lic(ctx).getString(KEY_ACTIVATED_CODE, null)
    fun setActivatedCode(ctx: Context, code: String) =
        lic(ctx).edit().putString(KEY_ACTIVATED_CODE, code).apply()

    fun isMember(ctx: Context): Boolean = tier(ctx) > 0
    fun isPro(ctx: Context): Boolean = tier(ctx) >= 2
}
