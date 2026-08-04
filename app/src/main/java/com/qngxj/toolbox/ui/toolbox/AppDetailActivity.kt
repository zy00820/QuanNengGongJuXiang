package com.qngxj.toolbox.ui.toolbox

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.qngxj.toolbox.R
import com.qngxj.toolbox.databinding.ActivityAppDetailBinding
import com.qngxj.toolbox.util.DeviceUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val pkg = intent.getStringExtra(EXTRA_PKG) ?: run { finish(); return }
        load(pkg)
    }

    private fun load(pkg: String) {
        Thread {
            val apps = DeviceUtils.installedApps(this).filter { it.pkg == pkg }
            val item = apps.firstOrNull() ?: run {
                runOnUiThread { Snackbar.make(binding.root, "未找到应用", Snackbar.LENGTH_SHORT).show() }
                return@Thread
            }
            runOnUiThread { bind(item) }
        }.start()
    }

    private fun bind(item: DeviceUtils.AppItem) {
        binding.ivIcon.setImageDrawable(try { packageManager.getApplicationIcon(item.pkg) } catch (e: Exception) { null })
        binding.tvName.text = item.name
        binding.tvPkg.text = item.pkg

        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val rows = listOf(
            "版本号" to "${item.versionName} (${item.versionCode})",
            "是否系统应用" to (if (item.systemApp) "是" else "否"),
            "目标 SDK" to "API ${item.targetSdk}",
            "最低 SDK" to "API ${item.minSdk}",
            "安装时间" to df.format(Date(item.installTime)),
            "更新时间" to df.format(Date(item.updateTime)),
            "APK 大小" to humanSize(item.sizeBytes),
            "APK 路径" to item.sourceDir
        )
        binding.rows.removeAllViews()
        for ((k, v) in rows) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_info_row, binding.rows, false) as LinearLayout
            row.findViewById<TextView>(R.id.tv_key).text = k
            row.findViewById<TextView>(R.id.tv_value).text = v
            binding.rows.addView(row)
        }

        binding.btnExtract.setOnClickListener { extract(item) }
        binding.btnOpen.setOnClickListener {
            val launch = packageManager.getLaunchIntentForPackage(item.pkg)
            if (launch != null) startActivity(launch)
            else Snackbar.make(binding.root, "无可启动入口", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun extract(item: DeviceUtils.AppItem) {
        try {
            val src = java.io.File(item.sourceDir)
            if (!src.exists()) {
                Snackbar.make(binding.root, R.string.extract_fail, Snackbar.LENGTH_SHORT).show()
                return
            }
            val outDir = java.io.File(getExternalFilesDir(null), "extracted_apk")
            if (!outDir.exists()) outDir.mkdirs()
            val safeName = item.pkg.replace("/", "_")
            val out = java.io.File(outDir, "${safeName}_${item.versionName}.apk")
            src.inputStream().use { input -> out.outputStream().use { input.copyTo(it) } }
            Snackbar.make(binding.root, getString(R.string.extract_success) + out.absolutePath, Snackbar.LENGTH_LONG)
                .setAction(R.string.copy_success) {
                    val cm = getSystemService(android.content.ClipboardManager::class.java)
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("apk", out.absolutePath))
                }.show()
        } catch (e: Exception) {
            Snackbar.make(binding.root, R.string.extract_fail, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun humanSize(b: Long): String {
        val mb = b / (1024.0 * 1024.0)
        return if (mb >= 1024) String.format("%.1f GB", mb / 1024) else String.format("%.0f MB", mb)
    }

    companion object {
        const val EXTRA_PKG = "pkg"
    }
}
