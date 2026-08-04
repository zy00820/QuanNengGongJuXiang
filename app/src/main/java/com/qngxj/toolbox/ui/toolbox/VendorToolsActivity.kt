package com.qngxj.toolbox.ui.toolbox

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.qngxj.toolbox.R
import com.qngxj.toolbox.util.ShizukuUtils

/**
 * 厂商工具板块：展示各品牌专属功能入口。
 *
 * - 小米板块：强开极致模式（Shizuku 写入系统属性）、安全中心、省电、权限管理
 * - vivo 板块：i 管家、省电、权限管理
 * - 华为板块：手机管家、电池、权限管理
 * - OPPO 板块：手机管家、电池、权限管理
 * - 三星板块：设备维护、电池、权限管理
 *
 * 各入口均为本地 Intent 跳转，无联网/无后台；非该品牌机型会提示兼容性。
 */
class VendorToolsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VENDOR = "vendor"
        const val VENDOR_XIAOMI = "xiaomi"
        const val VENDOR_VIVO = "vivo"
        const val VENDOR_HUAWEI = "huawei"
        const val VENDOR_OPPO = "oppo"
        const val VENDOR_SAMSUNG = "samsung"
    }

    private lateinit var rv: RecyclerView

    /** 功能项：title/desc/icon，action 为点击行为 */
    private data class VendorItem(
        val title: String,
        val desc: String,
        val icon: Int,
        val action: (ctx: Context, view: View) -> Unit
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendor_tools)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        val tvHint = findViewById<TextView>(R.id.tv_hint)
        rv = findViewById(R.id.rv)

        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val vendor = intent.getStringExtra(EXTRA_VENDOR) ?: VENDOR_XIAOMI
        val (titleRes, items) = buildItems(vendor)
        toolbar.title = getString(titleRes)

        // 品牌匹配提示
        val brand = android.os.Build.BRAND?.lowercase() ?: ""
        val matched = when (vendor) {
            VENDOR_XIAOMI -> brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")
            VENDOR_VIVO -> brand.contains("vivo") || brand.contains("iqoo")
            VENDOR_HUAWEI -> brand.contains("huawei") || brand.contains("honor")
            VENDOR_OPPO -> brand.contains("oppo") || brand.contains("realme") || brand.contains("oneplus")
            VENDOR_SAMSUNG -> brand.contains("samsung")
            else -> false
        }
        tvHint.text = if (matched) "" else getString(R.string.vendor_not_supported)

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = VendorAdapter(items)
    }

    private fun buildItems(vendor: String): Pair<Int, List<VendorItem>> {
        return when (vendor) {
            VENDOR_XIAOMI -> Pair(R.string.tool_vendor_xiaomi, listOf(
                VendorItem(
                    getString(R.string.vendor_xiaomi_extreme_mode),
                    getString(R.string.vendor_xiaomi_extreme_mode_desc),
                    R.drawable.ic_vendor_xiaomi
                ) { ctx, view -> enableXiaomiExtremeMode(ctx, view) },
                VendorItem(
                    getString(R.string.vendor_xiaomi_security),
                    "MIUI 安全中心",
                    R.drawable.ic_vendor_xiaomi
                ) { ctx, _ -> launchComponent(ctx, "com.miui.securitycenter", "com.miui.securitycenter.MainActivity") },
                VendorItem(
                    getString(R.string.vendor_xiaomi_battery),
                    getString(R.string.vendor_xiaomi_battery),
                    R.drawable.ic_vendor_xiaomi
                ) { ctx, _ -> launchComponent(ctx, "com.miui.powerkeeper", "com.miui.powerkeeper.ui.PowerKeeperMainActivity") },
                VendorItem(
                    getString(R.string.vendor_xiaomi_permission),
                    getString(R.string.vendor_xiaomi_permission),
                    R.drawable.ic_vendor_xiaomi
                ) { ctx, _ -> launchAction(ctx, "miui.intent.action.APP_PERM_EDITOR") },
                VendorItem(
                    getString(R.string.vendor_developer),
                    "开发者选项",
                    R.drawable.ic_tool_shortcut
                ) { ctx, _ -> launchAction(ctx, android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) },
                VendorItem(
                    getString(R.string.vendor_settings),
                    "系统设置",
                    R.drawable.ic_tool_shortcut
                ) { ctx, _ -> launchAction(ctx, android.provider.Settings.ACTION_SETTINGS) }
            ))
            VENDOR_VIVO -> Pair(R.string.tool_vendor_vivo, listOf(
                VendorItem(
                    getString(R.string.vendor_vivo_imanager),
                    "vivo i 管家",
                    R.drawable.ic_vendor_vivo
                ) { ctx, _ -> launchComponent(ctx, "com.iqoo.secure", "com.iqoo.secure.MainActivity") },
                VendorItem(
                    getString(R.string.vendor_vivo_battery),
                    getString(R.string.vendor_vivo_battery),
                    R.drawable.ic_vendor_vivo
                ) { ctx, _ -> launchComponent(ctx, "com.vivo.abe", "com.vivo.abe.BatteryManagerActivity") },
                VendorItem(
                    getString(R.string.vendor_vivo_permission),
                    getString(R.string.vendor_vivo_permission),
                    R.drawable.ic_vendor_vivo
                ) { ctx, _ -> launchAction(ctx, "vivo.intent.action.permissions") },
                VendorItem(
                    getString(R.string.vendor_developer),
                    "开发者选项",
                    R.drawable.ic_tool_shortcut
                ) { ctx, _ -> launchAction(ctx, android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) },
                VendorItem(
                    getString(R.string.vendor_settings),
                    "系统设置",
                    R.drawable.ic_tool_shortcut
                ) { ctx, _ -> launchAction(ctx, android.provider.Settings.ACTION_SETTINGS) }
            ))
            VENDOR_HUAWEI -> Pair(R.string.tool_vendor_huawei, listOf(
                VendorItem(
                    getString(R.string.vendor_huawei_manager),
                    "华为手机管家",
                    R.drawable.ic_vendor_huawei
                ) { ctx, _ -> launchComponent(ctx, "com.huawei.systemmanager", "com.huawei.systemmanager.mainscreen.MainScreenActivity") },
                VendorItem(
                    getString(R.string.vendor_huawei_battery),
                    getString(R.string.vendor_huawei_battery),
                    R.drawable.ic_vendor_huawei
                ) { ctx, _ -> launchAction(ctx, "huawei.intent.action.BATTERY_MANAGER") },
                VendorItem(
                    getString(R.string.vendor_huawei_permission),
                    getString(R.string.vendor_huawei_permission),
                    R.drawable.ic_vendor_huawei
                ) { ctx, _ -> launchComponent(ctx, "com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity") },
                VendorItem(
                    getString(R.string.vendor_developer),
                    "开发者选项",
                    R.drawable.ic_tool_shortcut
                ) { ctx, _ -> launchAction(ctx, android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) },
                VendorItem(
                    getString(R.string.vendor_settings),
                    "系统设置",
                    R.drawable.ic_tool_shortcut
                ) { ctx, _ -> launchAction(ctx, android.provider.Settings.ACTION_SETTINGS) }
            ))
            VENDOR_OPPO -> Pair(R.string.tool_vendor_oppo, listOf(
                VendorItem(
                    getString(R.string.vendor_oppo_manager),
                    "ColorOS 手机管家",
                    R.drawable.ic_vendor_oppo
                ) { ctx, _ -> launchComponent(ctx, "com.coloros.safecenter", "com.coloros.safecenter.permission.PermissionActivity") },
                VendorItem(
                    getString(R.string.vendor_oppo_battery),
                    getString(R.string.vendor_oppo_battery),
                    R.drawable.ic_vendor_oppo
                ) { ctx, _ -> launchComponent(ctx, "com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgage.PowerUsageSummaryActivity") },
                VendorItem(
                    getString(R.string.vendor_oppo_permission),
                    getString(R.string.vendor_oppo_permission),
                    R.drawable.ic_vendor_oppo
                ) { ctx, _ -> launchComponent(ctx, "com.coloros.safecenter", "com.coloros.safecenter.permission.PermissionActivity") },
                VendorItem(
                    getString(R.string.vendor_developer),
                    "开发者选项",
                    R.drawable.ic_tool_shortcut
                ) { ctx, _ -> launchAction(ctx, android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) },
                VendorItem(
                    getString(R.string.vendor_settings),
                    "系统设置",
                    R.drawable.ic_tool_shortcut
                ) { ctx, _ -> launchAction(ctx, android.provider.Settings.ACTION_SETTINGS) }
            ))
            VENDOR_SAMSUNG -> Pair(R.string.tool_vendor_samsung, listOf(
                VendorItem(
                    getString(R.string.vendor_samsung_maintenance),
                    "Samsung Device Care",
                    R.drawable.ic_vendor_samsung
                ) { ctx, _ -> launchComponent(ctx, "com.samsung.android.sm", "com.samsung.android.sm.ui.SamsungDeviceCareActivity") },
                VendorItem(
                    getString(R.string.vendor_samsung_battery),
                    getString(R.string.vendor_samsung_battery),
                    R.drawable.ic_vendor_samsung
                ) { ctx, _ -> launchComponent(ctx, "com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity") },
                VendorItem(
                    getString(R.string.vendor_samsung_permission),
                    getString(R.string.vendor_samsung_permission),
                    R.drawable.ic_vendor_samsung
                ) { ctx, _ -> launchComponent(ctx, "com.samsung.android.permission", "com.samsung.android.permission.PermissionActivity") },
                VendorItem(
                    getString(R.string.vendor_developer),
                    "开发者选项",
                    R.drawable.ic_tool_shortcut
                ) { ctx, _ -> launchAction(ctx, android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) },
                VendorItem(
                    getString(R.string.vendor_settings),
                    "系统设置",
                    R.drawable.ic_tool_shortcut
                ) { ctx, _ -> launchAction(ctx, android.provider.Settings.ACTION_SETTINGS) }
            ))
            else -> Pair(R.string.tool_vendor_xiaomi, emptyList())
        }
    }

    /**
     * 小米强开极致模式：通过 Shizuku 以 shell 权限写入系统属性。
     *
     * MIUI 极致模式相关属性：
     * - persist.sys.miui_extreme_mode：极致模式开关
     * - persist.sys.miui_perf_mode：性能模式
     *
     * 需 Shizuku 已授权（会员功能）。
     */
    private fun enableXiaomiExtremeMode(ctx: Context, view: View) {
        // 先检查 Shizuku 授权
        if (!ShizukuUtils.checkPermission()) {
            Snackbar.make(view, getString(R.string.vendor_xiaomi_extreme_fail), Snackbar.LENGTH_LONG).show()
            return
        }
        val snackbar = Snackbar.make(view, getString(R.string.vendor_xiaomi_extreme_running), Snackbar.LENGTH_INDEFINITE)
        snackbar.show()

        Thread {
            // 写入极致模式相关属性
            val cmds = listOf(
                "settings put global miui_extreme_mode 1",
                "settings put secure miui_extreme_mode 1",
                "setprop persist.sys.miui_extreme_mode 1",
                "setprop persist.sys.miui_perf_mode 1"
            )
            var lastOk = false
            var lastOut = ""
            for (c in cmds) {
                val (ok, out) = ShizukuUtils.execCommand(c)
                if (ok) { lastOk = true; break }
                lastOut = out
            }
            runOnUiThread {
                snackbar.dismiss()
                if (lastOk) {
                    Snackbar.make(view, getString(R.string.vendor_xiaomi_extreme_success), Snackbar.LENGTH_LONG).show()
                } else {
                    val msg = if (lastOut.isNotEmpty()) lastOut else getString(R.string.vendor_xiaomi_extreme_fail)
                    Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /** 通过 ComponentName 启动指定 Activity，失败时提示 */
    private fun launchComponent(ctx: Context, pkg: String, cls: String) {
        try {
            val intent = Intent().apply {
                component = ComponentName(pkg, cls)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            try {
                // 回退：直接打开该包名
                val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(intent)
                } else {
                    Snackbar.make(rv, getString(R.string.vendor_jump_fail), Snackbar.LENGTH_SHORT).show()
                }
            } catch (e2: Exception) {
                Snackbar.make(rv, getString(R.string.vendor_jump_fail), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    /** 通过 Action 启动，失败时提示 */
    private fun launchAction(ctx: Context, action: String) {
        try {
            val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            Snackbar.make(rv, getString(R.string.vendor_jump_fail), Snackbar.LENGTH_SHORT).show()
        }
    }

    private inner class VendorAdapter(private val items: List<VendorItem>) :
        RecyclerView.Adapter<VendorAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.iv_icon)
            val title: TextView = v.findViewById(R.id.tv_title)
            val desc: TextView = v.findViewById(R.id.tv_desc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_setting_row, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.title.text = item.title
            holder.desc.text = item.desc
            holder.icon.setImageResource(item.icon)
            // 隐藏 item_setting_row 中的 switch，显示箭头
            holder.itemView.findViewById<View>(R.id.switch_widget).visibility = View.GONE
            holder.itemView.findViewById<View>(R.id.iv_arrow).visibility = View.VISIBLE
            holder.itemView.setOnClickListener { item.action(holder.itemView.context, holder.itemView) }
        }

        override fun getItemCount(): Int = items.size
    }
}
