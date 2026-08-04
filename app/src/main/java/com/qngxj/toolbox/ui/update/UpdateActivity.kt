package com.qngxj.toolbox.ui.update

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.qngxj.toolbox.BuildConfig
import com.qngxj.toolbox.R
import com.qngxj.toolbox.databinding.ActivityUpdateBinding
import com.qngxj.toolbox.util.UpdateChecker

class UpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateBinding
    private var downloadUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        title = getString(R.string.title_update)

        binding.tvCurrent.text = "当前版本：${UpdateChecker.currentVersionName()} (${UpdateChecker.currentVersionCode()})"

        binding.btnCheck.setOnClickListener {
            binding.progress.visibility = View.VISIBLE
            binding.tvResult.visibility = View.GONE
            binding.btnDownload.visibility = View.GONE
            binding.tvResult.text = getString(R.string.update_checking)
            binding.tvResult.visibility = View.VISIBLE
            UpdateChecker.check(this) { info ->
                runOnUiThread {
                    binding.progress.visibility = View.GONE
                    if (info.error != null) {
                        binding.tvResult.text = "${getString(R.string.update_net_error)}\n${info.error}"
                    } else if (info.hasUpdate) {
                        binding.tvResult.text = "${getString(R.string.update_found)}\n最新版本：${info.versionName}\n\n${info.note}"
                        downloadUrl = info.downloadUrl
                        binding.btnDownload.visibility = if (info.downloadUrl.isNotEmpty()) View.VISIBLE else View.GONE
                    } else {
                        binding.tvResult.text = getString(R.string.update_latest)
                    }
                    binding.tvResult.visibility = View.VISIBLE
                }
            }
        }

        binding.btnDownload.setOnClickListener {
            downloadUrl?.let { url ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "无法打开下载链接", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}
