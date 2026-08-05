package com.qngxj.toolbox.ui.shizuku

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.qngxj.toolbox.R
import com.qngxj.toolbox.ui.member.MemberActivity
import com.qngxj.toolbox.util.LicenseManager
import com.qngxj.toolbox.util.RootUtils
import com.qngxj.toolbox.util.ShizukuUtils
import rikka.shizuku.Shizuku

/**
 * 内置 Shizuku 管理页（V1.1.7）。
 *
 * - 已 Root 设备：一键通过 app_process 启动 Shizuku 服务端，无需安装任何软件
 * - 未 Root 设备：一键安装内置 Shizuku APK，再通过 adb 激活
 *
 * 启动服务/安装/打开应用为基础设施，不限制会员；
 * 请求授权后的 Shizuku 高级功能使用仍需会员（LicenseManager.canUseShizuku）。
 */
class ShizukuManagerActivity : AppCompatActivity() {

    private lateinit var rootContainer: View
    private var binderReceivedListener: Shizuku.OnBinderReceivedListener? = null
    private var binderDeadListener: Shizuku.OnBinderDeadListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shizuku_manager)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rootContainer = findViewById(android.R.id.content)

        // 状态卡片
        findViewById<View>(R.id.card_start_root).setOnClickListener { startViaRoot() }
        findViewById<View>(R.id.card_install).setOnClickListener { installBundled() }
        findViewById<View>(R.id.card_auth).setOnClickListener { requestAuth() }
        findViewById<View>(R.id.card_open_app).setOnClickListener { openApp() }

        // 注册 Shizuku binder 监听以实时刷新状态
        binderReceivedListener = Shizuku.OnBinderReceivedListener { runOnUiThread { refreshStatus() } }
        binderDeadListener = Shizuku.OnBinderDeadListener { runOnUiThread { refreshStatus() } }
        ShizukuUtils.addBinderReceivedListener(binderReceivedListener!!)
        ShizukuUtils.addBinderDeadListener(binderDeadListener!!)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        binderReceivedListener?.let {
            try { Shizuku.removeBinderReceivedListener(it) } catch (_: Exception) {}
        }
        binderDeadListener?.let {
            try { Shizuku.removeBinderDeadListener(it) } catch (_: Exception) {}
        }
    }

    private fun refreshStatus() {
        val rooted = RootUtils.isRooted()
        findViewById<android.widget.TextView>(R.id.tv_root_status).text =
            "${getString(R.string.shizuku_root_status)}：${if (rooted) getString(R.string.shizuku_rooted) else getString(R.string.shizuku_not_rooted)}"

        val state = ShizukuUtils.state()
        findViewById<android.widget.TextView>(R.id.tv_service_status).text =
            "${getString(R.string.shizuku_service_status)}：${state.label}"
    }

    /** 一键启动服务（Root） */
    private fun startViaRoot() {
        val snackbar = Snackbar.make(rootContainer, getString(R.string.shizuku_starting), Snackbar.LENGTH_INDEFINITE)
        snackbar.show()
        Thread {
            val (ok, msg) = ShizukuUtils.startServiceViaRoot(this)
            runOnUiThread {
                snackbar.dismiss()
                Snackbar.make(rootContainer, msg, Snackbar.LENGTH_LONG).show()
                if (ok) {
                    // 延迟刷新状态
                    rootContainer.postDelayed({ refreshStatus() }, 1000)
                }
            }
        }.start()
    }

    /** 一键安装内置 Shizuku APK */
    private fun installBundled() {
        val ok = ShizukuUtils.installBundledApk(this)
        if (!ok) {
            Snackbar.make(rootContainer, getString(R.string.shizuku_install_fail), Snackbar.LENGTH_SHORT).show()
        }
    }

    /** 请求 Shizuku 授权（需会员） */
    private fun requestAuth() {
        if (!LicenseManager.canUseShizuku(this)) {
            Snackbar.make(rootContainer, getString(R.string.shizuku_need_member), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.member_activate)) {
                    startActivity(Intent(this, MemberActivity::class.java))
                }.show()
            return
        }
        val state = ShizukuUtils.state()
        if (state == ShizukuUtils.State.NOT_RUNNING) {
            Snackbar.make(rootContainer, getString(R.string.shizuku_no_manager), Snackbar.LENGTH_LONG).show()
            return
        }
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                runOnUiThread {
                    if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        Snackbar.make(rootContainer, getString(R.string.shizuku_status_authorized), Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(rootContainer, "授权被拒绝", Snackbar.LENGTH_SHORT).show()
                    }
                    refreshStatus()
                }
                try { Shizuku.removeRequestPermissionResultListener(this) } catch (_: Exception) {}
            }
        }
        ShizukuUtils.requestPermission(listener)
    }

    /** 打开 Shizuku 应用 */
    private fun openApp() {
        val ok = ShizukuUtils.openShizukuApp(this)
        if (!ok) {
            Snackbar.make(rootContainer, getString(R.string.shizuku_app_not_installed), Snackbar.LENGTH_SHORT).show()
        }
    }
}
