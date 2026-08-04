package com.qngxj.toolbox.ui.mine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.qngxj.toolbox.R
import com.qngxj.toolbox.databinding.ActivityNotifyBinding
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

        binding.switchUpdate.isChecked = Prefs.notifyUpdate(this)
        binding.switchUpdate.setOnCheckedChangeListener { _, v ->
            Prefs.setNotifyUpdate(this, v)
        }
    }
}
