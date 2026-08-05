package com.qngxj.toolbox.ui.toolbox

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.qngxj.toolbox.R

class ToolboxFragment : Fragment() {

    private lateinit var rv: RecyclerView

    private data class Tool(val title: String, val desc: String, val icon: Int, val action: () -> Unit)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        rv = inflater.inflate(R.layout.fragment_toolbox, container, false) as RecyclerView
        return rv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = view.context
        val tools = listOf(
            Tool(getString(R.string.tool_app_manage), getString(R.string.tool_app_manage_desc), R.drawable.ic_tool_apps) {
                startActivity(Intent(ctx, AppListActivity::class.java))
            },
            Tool(getString(R.string.tool_apk_extract), getString(R.string.tool_app_manage_desc), R.drawable.ic_tool_apk) {
                val intent = Intent(ctx, AppListActivity::class.java)
                intent.putExtra(AppListActivity.EXTRA_MODE, AppListActivity.MODE_EXTRACT)
                startActivity(intent)
            },
            Tool(getString(R.string.tool_system_shortcut), getString(R.string.tool_system_shortcut_desc), R.drawable.ic_tool_shortcut) {
                showShortcuts()
            },
            Tool(getString(R.string.tool_vendor_xiaomi), getString(R.string.tool_vendor_xiaomi_desc), R.drawable.ic_vendor_xiaomi) {
                val intent = Intent(ctx, VendorToolsActivity::class.java)
                intent.putExtra(VendorToolsActivity.EXTRA_VENDOR, VendorToolsActivity.VENDOR_XIAOMI)
                startActivity(intent)
            },
            Tool(getString(R.string.tool_vendor_vivo), getString(R.string.tool_vendor_vivo_desc), R.drawable.ic_vendor_vivo) {
                val intent = Intent(ctx, VendorToolsActivity::class.java)
                intent.putExtra(VendorToolsActivity.EXTRA_VENDOR, VendorToolsActivity.VENDOR_VIVO)
                startActivity(intent)
            },
            Tool(getString(R.string.tool_vendor_huawei), getString(R.string.tool_vendor_huawei_desc), R.drawable.ic_vendor_huawei) {
                val intent = Intent(ctx, VendorToolsActivity::class.java)
                intent.putExtra(VendorToolsActivity.EXTRA_VENDOR, VendorToolsActivity.VENDOR_HUAWEI)
                startActivity(intent)
            },
            Tool(getString(R.string.tool_vendor_oppo), getString(R.string.tool_vendor_oppo_desc), R.drawable.ic_vendor_oppo) {
                val intent = Intent(ctx, VendorToolsActivity::class.java)
                intent.putExtra(VendorToolsActivity.EXTRA_VENDOR, VendorToolsActivity.VENDOR_OPPO)
                startActivity(intent)
            },
            Tool(getString(R.string.tool_vendor_samsung), getString(R.string.tool_vendor_samsung_desc), R.drawable.ic_vendor_samsung) {
                val intent = Intent(ctx, VendorToolsActivity::class.java)
                intent.putExtra(VendorToolsActivity.EXTRA_VENDOR, VendorToolsActivity.VENDOR_SAMSUNG)
                startActivity(intent)
            },
            Tool(getString(R.string.tool_shizuku), getString(R.string.shizuku_bundled_desc), R.drawable.ic_tool_shizuku) {
                startActivity(Intent(ctx, com.qngxj.toolbox.ui.shizuku.ShizukuManagerActivity::class.java))
            }
        )

        val span = if (resources.configuration.screenWidthDp >= 600) 2 else 1
        rv.layoutManager = GridLayoutManager(ctx, span)
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tool, parent, false)
                return object : RecyclerView.ViewHolder(v) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val t = tools[position]
                holder.itemView.findViewById<TextView>(R.id.tv_title).text = t.title
                holder.itemView.findViewById<TextView>(R.id.tv_desc).text = t.desc
                holder.itemView.findViewById<ImageView>(R.id.iv_icon).setImageResource(t.icon)
                holder.itemView.setOnClickListener { t.action() }
            }

            override fun getItemCount(): Int = tools.size
        }
    }

    private fun showShortcuts() {
        val ctx = rv.context
        val items = arrayOf(
            getString(R.string.shortcut_settings),
            getString(R.string.shortcut_developer),
            getString(R.string.shortcut_hardware)
        )
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.tool_system_shortcut)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> launch(android.provider.Settings.ACTION_SETTINGS)
                    1 -> launch(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    2 -> launchHardwareDetection()
                }
            }
            .show()
    }

    private fun launch(action: String) {
        try {
            startActivity(Intent(action))
        } catch (e: Exception) {
            Snackbar.make(rv, "无法跳转：$action", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun launchHardwareDetection() {
        // 优先尝试设备信息页（关于手机，含硬件参数），兼容全品牌
        try {
            startActivity(Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
            } catch (e2: Exception) {
                Snackbar.make(rv, "无法跳转硬件检测", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
