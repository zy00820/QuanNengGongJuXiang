package com.qngxj.toolbox.util

import android.content.Context
import com.qngxj.toolbox.data.LicenseCodes
import java.security.MessageDigest

/**
 * 离线会员激活管理。
 * - 设备代码：基于 Android ID + 设备属性 SHA-256 生成
 * - 激活码：内置 50 Lite + 50 Pro（仅存哈希），每个仅可使用1次
 * - 未激活时，Shizuku 相关功能不可用
 *
 * 注：纯离线方案，单设备维度防重放；跨设备重放无法在无服务器下彻底防止。
 */
object LicenseManager {

    const val TIER_NONE = 0
    const val TIER_LITE = 1
    const val TIER_PRO = 2

    private fun sha256Upper(input: String): String {
        val raw = input.trim().uppercase().replace(" ", "")
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).fold(StringBuilder()) { acc, b ->
            acc.append("%02X".format(b))
        }.toString()
    }

    fun deviceCode(ctx: Context): String = Prefs.deviceCode(ctx)

    fun currentTier(ctx: Context): Int = Prefs.tier(ctx)

    fun isMember(ctx: Context): Boolean = Prefs.isMember(ctx)
    fun isPro(ctx: Context): Boolean = Prefs.isPro(ctx)

    /** 校验 Shizuku 相关功能是否可用（需至少 Lite 会员） */
    fun canUseShizuku(ctx: Context): Boolean = Prefs.tier(ctx) >= TIER_LITE

    data class ActivateResult(val success: Boolean, val tier: Int, val message: String)

    fun activate(ctx: Context, code: String): ActivateResult {
        val codeSha = sha256Upper(code)
        // 已使用过？
        if (Prefs.usedCodes(ctx).contains(codeSha)) {
            return ActivateResult(false, Prefs.tier(ctx), "该激活码已被使用")
        }
        val tier = LicenseCodes.tierOf(codeSha)
        if (tier == 0) {
            return ActivateResult(false, Prefs.tier(ctx), "激活码无效或已被使用")
        }
        // Pro 可向下兼容 Lite；若已是 Pro 不允许降级
        val current = Prefs.tier(ctx)
        if (current >= tier) {
            return ActivateResult(false, current, "当前已是更高等级会员")
        }
        Prefs.markUsed(ctx, codeSha)
        Prefs.setTier(ctx, tier)
        Prefs.setActivatedCode(ctx, code.trim().uppercase())
        return ActivateResult(true, tier, "激活成功")
    }
}
