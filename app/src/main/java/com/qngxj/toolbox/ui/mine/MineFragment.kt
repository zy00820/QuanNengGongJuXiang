package com.qngxj.toolbox.ui.mine

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.qngxj.toolbox.App
import com.qngxj.toolbox.BuildConfig
import com.qngxj.toolbox.R
import com.qngxj.toolbox.databinding.FragmentMineBinding
import com.qngxj.toolbox.ui.member.MemberActivity
import com.qngxj.toolbox.ui.update.UpdateActivity
import com.qngxj.toolbox.util.LicenseManager
import com.qngxj.toolbox.util.Prefs
import com.qngxj.toolbox.util.ShizukuUtils

class MineFragment : Fragment() {

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!
    private val permListener = rikka.shizuku.Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        activity?.runOnUiThread { refreshShizuku() }
        if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            activity?.runOnUiThread {
                Snackbar.make(binding.root, "Shizuku 已授权", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = view.context

        binding.tvVersion.text = "${getString(R.string.about_version)} ${BuildConfig.VERSION_NAME}"

        setupRow(binding.rowMember, R.drawable.ic_member, getString(R.string.mine_member))
        setupRow(binding.rowShizuku, R.drawable.ic_tool_shizuku, getString(R.string.mine_shizuku))
        setupRow(binding.rowDark, R.drawable.ic_dark, getString(R.string.mine_dark_mode))
        setupRow(binding.rowNotify, R.drawable.ic_notify, getString(R.string.mine_notify))
        setupRow(binding.rowUpdate, R.drawable.ic_update, getString(R.string.mine_update))
        setupRow(binding.rowAbout, R.drawable.ic_about, getString(R.string.mine_about))
        setupRow(binding.rowDev, R.drawable.ic_dev, getString(R.string.mine_developer))

        refreshMemberBadge()

        binding.cardMember.setOnClickListener { startActivity(Intent(ctx, MemberActivity::class.java)) }

        binding.cardShizuku.setOnClickListener {
            when (ShizukuUtils.state()) {
                ShizukuUtils.State.NOT_RUNNING -> {
                    // 服务未运行，跳转到内置 Shizuku 管理页启动服务
                    startActivity(Intent(ctx, com.qngxj.toolbox.ui.shizuku.ShizukuManagerActivity::class.java))
                }
                ShizukuUtils.State.RUNNING_UNAUTHORIZED -> {
                    if (!LicenseManager.canUseShizuku(ctx)) {
                        Snackbar.make(binding.root, getString(R.string.shizuku_need_member), Snackbar.LENGTH_LONG)
                            .setAction(getString(R.string.member_activate)) {
                                startActivity(Intent(ctx, MemberActivity::class.java))
                            }.show()
                        return@setOnClickListener
                    }
                    try { ShizukuUtils.requestPermission(permListener) } catch (e: Exception) {
                        Snackbar.make(binding.root, "请求授权失败", Snackbar.LENGTH_SHORT).show()
                    }
                }
                ShizukuUtils.State.AUTHORIZED -> Snackbar.make(binding.root, getString(R.string.shizuku_status_authorized), Snackbar.LENGTH_SHORT).show()
                else -> Snackbar.make(binding.root, getString(R.string.shizuku_status_unknown), Snackbar.LENGTH_SHORT).show()
            }
        }
        // 长按 Shizuku 卡片进入内置管理页
        binding.cardShizuku.setOnLongClickListener {
            startActivity(Intent(ctx, com.qngxj.toolbox.ui.shizuku.ShizukuManagerActivity::class.java))
            true
        }

        // 深色模式开关
        binding.rowDark.ivArrow.visibility = View.GONE
        val darkSwitch = binding.rowDark.switchWidget
        darkSwitch.visibility = View.VISIBLE
        darkSwitch.isChecked = Prefs.darkMode(ctx) == 2
        darkSwitch.setOnCheckedChangeListener { _, checked ->
            val mode = if (checked) 2 else 1
            Prefs.setDarkMode(ctx, mode)
            App.applyTheme(mode)
            (activity as? AppCompatActivity)?.delegate?.applyDayNight()
        }

        binding.rowNotify.root.setOnClickListener { startActivity(Intent(ctx, NotifySettingsActivity::class.java)) }
        binding.rowUpdate.root.setOnClickListener { startActivity(Intent(ctx, UpdateActivity::class.java)) }
        binding.rowAbout.root.setOnClickListener { startActivity(Intent(ctx, AboutActivity::class.java)) }
        binding.rowDev.root.setOnClickListener { startActivity(Intent(ctx, AboutActivity::class.java)) }

        ShizukuUtils.addBinderReceivedListener { activity?.runOnUiThread { refreshShizuku() } }
        refreshShizuku()
    }

    private fun setupRow(row: com.qngxj.toolbox.databinding.ItemSettingRowBinding, icon: Int, title: String) {
        row.ivIcon.setImageResource(icon)
        row.tvTitle.text = title
    }

    private fun refreshMemberBadge() {
        val ctx = binding.root.context
        val tier = LicenseManager.currentTier(ctx)
        val badge = binding.tvMemberBadge
        when (tier) {
            LicenseManager.TIER_LITE -> {
                badge.text = getString(R.string.member_status_lite)
                badge.setBackgroundColor(getColor(R.color.lite_badge))
            }
            LicenseManager.TIER_PRO -> {
                badge.text = getString(R.string.member_status_pro)
                badge.setBackgroundColor(getColor(R.color.pro_badge))
            }
            else -> {
                badge.text = getString(R.string.member_status_none)
                badge.setBackgroundColor(getColor(R.color.status_err))
            }
        }
        val sub = when (tier) {
            LicenseManager.TIER_LITE -> getString(R.string.member_status_lite)
            LicenseManager.TIER_PRO -> getString(R.string.member_status_pro)
            else -> getString(R.string.member_status_none)
        }
        binding.rowMember.tvSub.apply {
            text = sub
            visibility = View.VISIBLE
        }
    }

    private fun refreshShizuku() {
        val state = ShizukuUtils.state()
        val text = state.label + " · " + ShizukuUtils.version()
        binding.rowShizuku.tvSub.apply {
            this.text = text
            visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        refreshMemberBadge()
        refreshShizuku()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { rikka.shizuku.Shizuku.removeRequestPermissionResultListener(permListener) } catch (e: Exception) {}
        _binding = null
    }

    private fun getColor(c: Int): Int = androidx.core.content.ContextCompat.getColor(binding.root.context, c)
}
