package com.qngxj.toolbox.ui.member

import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.qngxj.toolbox.R
import com.qngxj.toolbox.databinding.ActivityMemberBinding
import com.qngxj.toolbox.util.LicenseManager
import com.qngxj.toolbox.util.ShizukuUtils
import rikka.shizuku.Shizuku

class MemberActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemberBinding
    private val permListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        refreshShizuku()
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(binding.root, "Shizuku 已授权", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        title = getString(R.string.title_member)

        binding.tvDeviceCode.text = LicenseManager.deviceCode(this)

        binding.btnCopy.setOnClickListener {
            val cm = getSystemService(android.content.ClipboardManager::class.java)
            cm?.setPrimaryClip(ClipData.newPlainText("device_code", binding.tvDeviceCode.text))
            Snackbar.make(binding.root, R.string.copy_success, Snackbar.LENGTH_SHORT).show()
        }

        binding.btnActivate.setOnClickListener {
            val code = binding.etCode.text?.toString() ?: ""
            if (code.isBlank()) {
                Snackbar.make(binding.root, getString(R.string.member_input_code), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val result = LicenseManager.activate(this, code)
            if (result.success) {
                binding.etCode.setText("")
                Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
            }
            refreshStatus()
            refreshShizuku()
        }

        binding.cardShizuku.setOnClickListener {
            if (!LicenseManager.canUseShizuku(this)) {
                Snackbar.make(binding.root, getString(R.string.shizuku_need_member), Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            when (ShizukuUtils.state()) {
                ShizukuUtils.State.NOT_RUNNING -> Snackbar.make(binding.root, getString(R.string.shizuku_no_manager), Snackbar.LENGTH_LONG).show()
                ShizukuUtils.State.RUNNING_UNAUTHORIZED -> {
                    try { ShizukuUtils.requestPermission(permListener) } catch (e: Exception) {
                        Snackbar.make(binding.root, "请求授权失败", Snackbar.LENGTH_SHORT).show()
                    }
                }
                ShizukuUtils.State.AUTHORIZED -> Snackbar.make(binding.root, getString(R.string.shizuku_status_authorized), Snackbar.LENGTH_SHORT).show()
                else -> Snackbar.make(binding.root, getString(R.string.shizuku_status_unknown), Snackbar.LENGTH_SHORT).show()
            }
        }

        ShizukuUtils.addBinderReceivedListener { runOnUiThread { refreshShizuku() } }
        refreshStatus()
        refreshShizuku()
    }

    private fun refreshStatus() {
        val tier = LicenseManager.currentTier(this)
        when (tier) {
            LicenseManager.TIER_LITE -> {
                binding.tvStatus.text = getString(R.string.member_status_lite)
                binding.tvStatusDesc.text = getString(R.string.member_lite_desc)
            }
            LicenseManager.TIER_PRO -> {
                binding.tvStatus.text = getString(R.string.member_status_pro)
                binding.tvStatusDesc.text = getString(R.string.member_pro_desc)
            }
            else -> {
                binding.tvStatus.text = getString(R.string.member_status_none)
                binding.tvStatusDesc.text = getString(R.string.shizuku_need_member)
            }
        }
    }

    private fun refreshShizuku() {
        val state = ShizukuUtils.state()
        binding.tvShizukuStatus.text = "${state.label} · ${ShizukuUtils.version()}"
        binding.tvShizukuHint.text = if (LicenseManager.canUseShizuku(this))
            "已解锁，可点击请求/查看授权" else getString(R.string.shizuku_need_member)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        refreshShizuku()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { Shizuku.removeRequestPermissionResultListener(permListener) } catch (e: Exception) {}
    }
}
