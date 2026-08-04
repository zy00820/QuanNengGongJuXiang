package com.qngxj.toolbox.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * 通知权限工具（适配安卓 13+ POST_NOTIFICATIONS 运行时权限）。
 *
 * - 安卓 13 (API 33) 之前：通知默认开启，无需运行时申请
 * - 安卓 13+：需申请 POST_NOTIFICATIONS 权限，用户可随时在系统设置中关闭
 */
object NotificationPermission {

    const val REQUEST_CODE_POST_NOTIFICATIONS = 1001

    /**
     * 是否已拥有通知权限。
     * - API < 33：通知通道默认开启，返回 true
     * - API >= 33：检查 POST_NOTIFICATIONS 运行时权限
     */
    fun isGranted(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // 安卓 13 以下：通过 NotificationManagerCompat 判断
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        }
    }

    /**
     * 请求通知权限（仅安卓 13+ 需要）。
     * 应在 Activity 中调用，回调通过 onRequestPermissionsResult 处理。
     *
     * @return true 表示已发起请求（需等待回调），false 表示无需申请或已授权
     */
    fun requestIfNeeded(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false // 低版本无需运行时申请
        }
        if (isGranted(activity)) {
            return false // 已授权
        }
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE_POST_NOTIFICATIONS
        )
        return true
    }

    /**
     * 处理权限申请回调。
     *
     * @param requestCode 请求码
     * @param grantResults 授权结果
     * @param onResult 回调：true=已授权，false=被拒绝
     */
    fun handleResult(
        requestCode: Int,
        grantResults: IntArray,
        onResult: (Boolean) -> Unit
    ) {
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            onResult(granted)
        }
    }
}
