package com.qngxj.toolbox.ui.firmware

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.qngxj.toolbox.R
import com.qngxj.toolbox.databinding.ActivityFirmwareDetailBinding

class FirmwareDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFirmwareDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirmwareDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        title = intent.getStringExtra("model") ?: getString(R.string.title_firmware_detail)

        val rows = listOf(
            "机型" to (intent.getStringExtra("model") ?: "-"),
            "固件版本" to (intent.getStringExtra("version") ?: "-"),
            "系统版本" to (intent.getStringExtra("android") ?: "-"),
            "分类" to (intent.getStringExtra("cat") ?: "-"),
            "发布日期" to (intent.getStringExtra("date") ?: "-")
        )
        binding.rows.removeAllViews()
        for ((k, v) in rows) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_info_row, binding.rows, false) as LinearLayout
            row.findViewById<TextView>(R.id.tv_key).text = k
            row.findViewById<TextView>(R.id.tv_value).text = v
            binding.rows.addView(row)
        }
    }
}
