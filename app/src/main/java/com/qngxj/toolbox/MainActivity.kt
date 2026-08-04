package com.qngxj.toolbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.qngxj.toolbox.databinding.ActivityMainBinding
import com.qngxj.toolbox.ui.firmware.FirmwareFragment
import com.qngxj.toolbox.ui.hardware.HardwareFragment
import com.qngxj.toolbox.ui.mine.MineFragment
import com.qngxj.toolbox.ui.toolbox.ToolboxFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.bottomNav.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_LABELED

        val homeFrag = HardwareFragment()
        val map = mapOf(
            R.id.nav_home to homeFrag,
            R.id.nav_toolbox to ToolboxFragment(),
            R.id.nav_firmware to FirmwareFragment(),
            R.id.nav_mine to MineFragment()
        )

        var current: Fragment? = null
        fun switch(target: Fragment, title: CharSequence) {
            val fm = supportFragmentManager
            val tx = fm.beginTransaction()
            if (current != null) tx.hide(current!!)
            if (!target.isAdded) tx.add(R.id.nav_host, target, target.javaClass.name)
            else tx.show(target)
            tx.commitAllowingStateLoss()
            current = target
            binding.toolbar.title = title
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val frag = map[item.itemId] ?: return@setOnItemSelectedListener false
            when (item.itemId) {
                R.id.nav_home -> switch(frag, getString(R.string.nav_home))
                R.id.nav_toolbox -> switch(frag, getString(R.string.nav_toolbox))
                R.id.nav_firmware -> switch(frag, getString(R.string.nav_firmware))
                R.id.nav_mine -> switch(frag, getString(R.string.nav_mine))
            }
            true
        }

        if (savedInstanceState == null) {
            switch(homeFrag, getString(R.string.nav_home))
            binding.bottomNav.selectedItemId = R.id.nav_home
        }
    }
}
