package com.qngxj.toolbox.ui.mine

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qngxj.toolbox.R
import com.qngxj.toolbox.databinding.ActivityNotifyBinding
import com.qngxj.toolbox.util.NotificationPermission
import com.qngxj.toolbox.util.Prefs

class NotifySettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotifyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotifyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        title = getString(R.string.title_notify)

        // 初始化开关状态：通知权限已授权 && 用户偏好开启
        val permGranted = NotificationPermission.isGranted(this)
        binding.switchUpdate.isEnabled = permGranted
        binding.switchUpdate.isChecked = permGranted && Prefs.notifyUpdate(this)

        // 若通知权限未授权，显示提示
        if (!permGranted) {
            binding.tvNotifyHint.text = "通知权限未开启，点击下方按钮前往系统设置开启"
            binding.tvNotifyHint.visibility = android.view.View.VISIBLE
            binding.btnOpenNotifySettings.visibility = android.view.View.VISIBLE
        } else {
            binding.tvNotifyHint.visibility = android.view.View.GONE
            binding.btnOpenNotifySettings.visibility = android.view.View.GONE
        }

        binding.switchUpdate.setOnCheckedChangeListener { _, v ->
            if (v && !NotificationPermission.isGranted(this)) {
                // 开启通知但无权限：先申请权限（安卓 13+）
                binding.switchUpdate.isChecked = false
                NotificationPermission.requestIfNeeded(this)
            } else {
                Prefs.setNotifyUpdate(this, v)
            }
        }

        binding.btnOpenNotifySettings.setOnClickListener {
            openNotificationSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        // 从系统设置返回后刷新权限状态
        val permGranted = NotificationPermission.isGranted(this)
        binding.switchUpdate.isEnabled = permGranted
        if (permGranted) {
            binding.tvNotifyHint.visibility = android.view.View.GONE
            binding.btnOpenNotifySettings.visibility = android.view.View.GONE
            binding.switchUpdate.isChecked = Prefs.notifyUpdate(this)
        } else {
            binding.tvNotifyHint.visibility = android.view.View.VISIBLE
            binding.btnOpenNotifySettings.visibility = android.view.View.VISIBLE
            binding.switchUpdate.isChecked = false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        NotificationPermission.handleResult(requestCode, grantResults) { granted ->
            if (granted) {
                Prefs.setNotifyUpdate(this, true)
                binding.switchUpdate.isEnabled = true
                binding.switchUpdate.isChecked = true
                binding.tvNotifyHint.visibility = android.view.View.GONE
                binding.btnOpenNotifySettings.visibility = android.view.View.GONE
                Toast.makeText(this, "通知权限已开启", Toast.LENGTH_SHORT).show()
            } else {
                binding.switchUpdate.isChecked = false
                binding.switchUpdate.isEnabled = false
                Toast.makeText(this, "通知权限被拒绝，可在系统设置中手动开启", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 跳转到系统应用通知设置页（兼容安卓 8+）。
     */
    private fun openNotificationSettings() {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // 兜底：跳转到应用详情页
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "无法打开通知设置", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
