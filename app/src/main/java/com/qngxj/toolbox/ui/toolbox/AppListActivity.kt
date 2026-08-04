package com.qngxj.toolbox.ui.toolbox

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.qngxj.toolbox.R
import com.qngxj.toolbox.databinding.ActivityAppListBinding
import com.qngxj.toolbox.util.DeviceUtils

class AppListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppListBinding
    private lateinit var adapter: AppListAdapter
    private var extractMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        extractMode = intent.getIntExtra(EXTRA_MODE, MODE_VIEW) == MODE_EXTRACT
        title = if (extractMode) getString(R.string.tool_apk_extract) else getString(R.string.title_app_list)

        adapter = AppListAdapter(this, extractMode) { item ->
            if (extractMode) {
                extractApk(item)
            } else {
                val i = Intent(this, AppDetailActivity::class.java)
                i.putExtra(AppDetailActivity.EXTRA_PKG, item.pkg)
                startActivity(i)
            }
        }
        binding.rv.layoutManager = LinearLayoutManager(this)
        binding.rv.adapter = adapter

        binding.search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        load()
    }

    private fun load() {
        binding.loading.visibility = View.VISIBLE
        binding.empty.visibility = View.GONE
        Thread {
            val apps = DeviceUtils.installedApps(this)
            runOnUiThread {
                binding.loading.visibility = View.GONE
                adapter.submit(apps)
                binding.empty.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
            }
        }.start()
    }

    private fun extractApk(item: DeviceUtils.AppItem) {
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

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_VIEW = 0
        const val MODE_EXTRACT = 1
    }
}
