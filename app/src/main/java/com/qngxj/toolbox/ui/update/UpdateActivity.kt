package com.qngxj.toolbox.ui.update

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.qngxj.toolbox.R
import com.qngxj.toolbox.databinding.ActivityUpdateBinding
import com.qngxj.toolbox.util.ApkDownloader
import com.qngxj.toolbox.util.UpdateChecker
import okhttp3.OkHttpClient
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class UpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateBinding
    private lateinit var downloadClient: OkHttpClient

    private var downloadUrl: String? = null
    private var downloadedFile: File? = null
    private var isDownloading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        title = getString(R.string.title_update)

        downloadClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        binding.tvCurrent.text =
            "当前版本：${UpdateChecker.currentVersionName()} (${UpdateChecker.currentVersionCode()})"

        binding.btnCheck.setOnClickListener {
            if (isDownloading) {
                Snackbar.make(binding.root, "正在下载中，请稍候", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startCheck()
        }

        binding.btnDownload.setOnClickListener {
            val url = downloadUrl
            if (url.isNullOrEmpty()) {
                Snackbar.make(binding.root, "无可用下载链接", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startDownload(url)
        }

        binding.btnInstall.setOnClickListener {
            val f = downloadedFile
            if (f != null && f.exists()) {
                ApkDownloader.installApk(this, f)
            } else {
                Snackbar.make(binding.root, "下载文件不存在，请重新下载", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            // 简单实现：仅隐藏进度并提示。OkHttp 同步调用无法强制中断，等待本次下载自然结束。
            isDownloading = false
            binding.layoutDownloadProgress.visibility = View.GONE
            binding.tvDownloadStatus.text = "已取消（等待后台下载线程结束）"
            Snackbar.make(binding.root, "已发起取消请求", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun startCheck() {
        binding.progress.visibility = View.VISIBLE
        binding.tvResult.visibility = View.GONE
        binding.btnDownload.visibility = View.GONE
        binding.btnInstall.visibility = View.GONE
        binding.layoutDownloadProgress.visibility = View.GONE
        binding.tvResult.text = getString(R.string.update_checking)
        binding.tvResult.visibility = View.VISIBLE

        UpdateChecker.check(this) { info ->
            runOnUiThread {
                binding.progress.visibility = View.GONE
                if (info.error != null) {
                    binding.tvResult.text = "${getString(R.string.update_net_error)}\n${info.error}"
                    binding.btnDownload.visibility = View.GONE
                } else if (info.hasUpdate) {
                    val sizeText = if (info.fileSize > 0) {
                        "\n文件大小：${formatSize(info.fileSize)}"
                    } else ""
                    binding.tvResult.text =
                        "${getString(R.string.update_found)}\n最新版本：${info.versionName}$sizeText\n\n${info.note}"
                    downloadUrl = info.downloadUrl
                    binding.btnDownload.visibility =
                        if (info.downloadUrl.isNotEmpty()) View.VISIBLE else View.GONE
                } else {
                    binding.tvResult.text = getString(R.string.update_latest)
                    binding.btnDownload.visibility = View.GONE
                }
                binding.tvResult.visibility = View.VISIBLE
            }
        }
    }

    private fun startDownload(url: String) {
        if (isDownloading) return
        isDownloading = true
        downloadedFile = null
        // 清理旧下载
        ApkDownloader.cleanOldDownloads(this)

        binding.btnDownload.visibility = View.GONE
        binding.btnInstall.visibility = View.GONE
        binding.layoutDownloadProgress.visibility = View.VISIBLE
        binding.btnCancel.visibility = View.VISIBLE
        binding.progressDownload.progress = 0
        binding.tvDownloadStatus.text = "下载中…"
        binding.tvDownloadPercent.text = "0%"
        binding.tvDownloadSize.text = ""

        val fileName = url.substringAfterLast('/', "app-release.apk")
        ApkDownloader.download(
            ctx = this,
            client = downloadClient,
            url = url,
            fileName = fileName.ifEmpty { "app-release.apk" },
            onProgress = { p ->
                runOnUiThread {
                    if (p.percent >= 0) {
                        binding.progressDownload.progress = p.percent
                        binding.tvDownloadPercent.text = "${p.percent}%"
                        binding.tvDownloadSize.text =
                            "${formatSize(p.downloadedBytes)} / ${formatSize(p.totalBytes)}"
                    } else {
                        // 总大小未知，仅显示已下载
                        binding.progressDownload.isIndeterminate = true
                        binding.tvDownloadPercent.text = "…"
                        binding.tvDownloadSize.text = "已下载：${formatSize(p.downloadedBytes)}"
                    }
                }
            },
            onComplete = { result ->
                runOnUiThread {
                    isDownloading = false
                    binding.btnCancel.visibility = View.GONE
                    binding.progressDownload.isIndeterminate = false
                    binding.layoutDownloadProgress.visibility = View.GONE
                    when (result) {
                        is ApkDownloader.Result.Success -> {
                            downloadedFile = result.file
                            binding.tvDownloadStatus.text = "下载完成"
                            binding.tvResult.text = "下载完成，点击下方按钮安装\n路径：${result.file.absolutePath}"
                            binding.tvResult.visibility = View.VISIBLE
                            binding.btnInstall.visibility = View.VISIBLE
                            // 直接触发安装
                            ApkDownloader.installApk(this, result.file)
                        }
                        is ApkDownloader.Result.Failed -> {
                            binding.tvDownloadStatus.text = "下载失败"
                            binding.tvResult.text = "下载失败：${result.message}"
                            binding.tvResult.visibility = View.VISIBLE
                            binding.btnDownload.visibility = View.VISIBLE
                            Snackbar.make(binding.root, "下载失败：${result.message}", Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var idx = 0
        while (size >= 1024 && idx < units.lastIndex) {
            size /= 1024
            idx++
        }
        return String.format(Locale.US, "%.1f %s", size, units[idx])
    }
}
