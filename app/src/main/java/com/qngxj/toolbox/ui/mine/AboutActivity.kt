package com.qngxj.toolbox.ui.mine

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.qngxj.toolbox.BuildConfig
import com.qngxj.toolbox.R
import com.qngxj.toolbox.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        title = getString(R.string.title_about)

        binding.tvVersion.text = "${getString(R.string.about_version)} ${BuildConfig.VERSION_NAME}"

        binding.rowDevLead.tvRole.text = "总开发"
        binding.rowDevLead.tvName.text = getString(R.string.about_dev_lead)

        binding.rowDevUi.tvRole.text = "UI 设计"
        binding.rowDevUi.tvName.text = getString(R.string.about_dev_ui)
    }
}
