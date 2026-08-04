package com.qngxj.toolbox.util

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.StatFs
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import java.security.MessageDigest

/**
 * 设备硬件信息采集（纯本地读取，不联网、不上报）。
 */
object DeviceUtils {

    fun generateDeviceCode(ctx: Context): String {
        val androidId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        val raw = androidId + "|" + Build.MANUFACTURER + "|" + Build.MODEL + "|" + Build.BOARD
        val sha = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).fold(StringBuilder()) { acc, b ->
            acc.append("%02X".format(b))
        }.toString()
        // 取前16位分组展示
        return "DC-" + sha.substring(0, 4) + "-" + sha.substring(4, 8) + "-" + sha.substring(8, 12) + "-" + sha.substring(12, 16)
    }

    // ---------- CPU ----------
    data class CpuInfo(val arch: String, val cores: Int, val abi: String, val freqMax: String, val hardware: String, val processor: String)

    fun cpuInfo(): CpuInfo {
        val cores = Runtime.getRuntime().availableProcessors()
        // Build.SUPPORTED_ABIS 为 API 21+，API 19-20 需回退到 Build.CPU_ABI/CPU_ABI2
        val abiList: List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS.toList()
        } else {
            @Suppress("DEPRECATION")
            listOfNotNull(Build.CPU_ABI, Build.CPU_ABI2)
        }
        val abi = abiList.joinToString(", ")
        val arch = abiList.firstOrNull() ?: "unknown"
        val freq = readCpuMaxFreq()
        val hw = readCpuField("Hardware")
        val proc = readCpuField("Processor")
        return CpuInfo(arch, cores, abi, freq, hw, proc)
    }

    private fun readCpuMaxFreq(): String {
        return try {
            var max = 0L
            for (i in 0 until Runtime.getRuntime().availableProcessors()) {
                val f = java.io.File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                if (f.exists()) {
                    val v = f.readText().trim().toLongOrNull() ?: 0L
                    if (v > max) max = v
                }
            }
            if (max > 0) "${max / 1000} MHz" else "未知"
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun readCpuField(key: String): String {
        return try {
            val lines = java.io.File("/proc/cpuinfo").readLines()
            lines.firstOrNull { it.startsWith(key, ignoreCase = true) }
                ?.substringAfter(":")?.trim() ?: "未知"
        } catch (e: Exception) {
            "未知"
        }
    }

    // ---------- GPU ----------
    fun gpuInfo(): String {
        return try {
            val str = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER)
            str ?: "未知"
        } catch (e: Exception) {
            "未知"
        }
    }

    fun glVersion(): String {
        return try {
            android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_VERSION) ?: "未知"
        } catch (e: Exception) {
            "未知"
        }
    }

    // ---------- 屏幕 ----------
    data class ScreenInfo(val resolution: String, val density: String, val dpi: Int, val refreshRate: String, val sizeInch: String)

    @Suppress("DEPRECATION")
    fun screenInfo(ctx: Context): ScreenInfo {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(dm)
        val resolution = "${dm.widthPixels} × ${dm.heightPixels}"
        val dpi = dm.densityDpi
        val density = String.format("%.2f", dm.density) + " (xdpi=" + String.format("%.1f", dm.xdpi) + ")"
        val refresh = try {
            "${wm.defaultDisplay.refreshRate.toInt()} Hz"
        } catch (e: Exception) {
            "未知"
        }
        val sizeInch = try {
            val w = dm.widthPixels.toDouble() / dm.xdpi
            val h = dm.heightPixels.toDouble() / dm.xdpi
            String.format("%.1f\"", Math.sqrt(w * w + h * h))
        } catch (e: Exception) {
            "未知"
        }
        return ScreenInfo(resolution, density, dpi, refresh, sizeInch)
    }

    // ---------- 内存存储 ----------
    data class MemInfo(val totalRam: String, val availRam: String, val totalStorage: String, val availStorage: String)

    fun memInfo(ctx: Context): MemInfo {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val totalRam = formatSize(mi.totalMem)
        val availRam = formatSize(mi.availMem)
        val stat = StatFs(android.os.Environment.getDataDirectory().path)
        val totalStorage = formatSize(stat.totalBytes)
        val availStorage = formatSize(stat.availableBytes)
        return MemInfo(totalRam, availRam, totalStorage, availStorage)
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) String.format("%.1f GB", mb / 1024) else String.format("%.0f MB", mb)
    }

    // ---------- 设备型号 ----------
    data class DeviceInfo(val manufacturer: String, val model: String, val brand: String, val board: String, val device: String, val buildId: String, val fingerprint: String)

    @SuppressLint("HardwareIds")
    fun deviceInfo(): DeviceInfo {
        return DeviceInfo(
            Build.MANUFACTURER,
            Build.MODEL,
            Build.BRAND,
            Build.BOARD,
            Build.DEVICE,
            Build.ID,
            Build.FINGERPRINT
        )
    }

    // ---------- 系统 ----------
    data class OsInfo(val version: String, val sdk: Int, val securityPatch: String, val buildTime: String)

    fun osInfo(): OsInfo {
        return OsInfo(
            Build.VERSION.RELEASE ?: "未知",
            Build.VERSION.SDK_INT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH ?: "未知" else "未知",
            try { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(Build.TIME)) } catch (e: Exception) { "未知" }
        )
    }

    // ---------- 电池 ----------
    data class BatteryInfo(val level: Int, val scale: Int, val status: String, val health: String, val technology: String, val temperature: String, val voltage: String)

    fun batteryInfoFromIntent(ctx: Context): BatteryInfo {
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        val intent = ctx.registerReceiver(null, filter)
        val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        val statusInt = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
        val healthInt = intent?.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val tech = intent?.getStringExtra(android.os.BatteryManager.EXTRA_TECHNOLOGY) ?: "未知"
        val temp = intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val volt = intent?.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val status = when (statusInt) {
            android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
            android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
            android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
            android.os.BatteryManager.BATTERY_STATUS_FULL -> "已充满"
            else -> "未知"
        }
        val health = when (healthInt) {
            android.os.BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
            android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
            android.os.BatteryManager.BATTERY_HEALTH_DEAD -> "损坏"
            android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "过压"
            android.os.BatteryManager.BATTERY_HEALTH_COLD -> "过冷"
            else -> "未知"
        }
        return BatteryInfo(
            level, scale, status, health, tech,
            if (temp >= 0) "${temp / 10.0} °C" else "未知",
            if (volt >= 0) "${volt / 1000.0} V" else "未知"
        )
    }

    /** 电池健康百分比（level/scale） */
    fun batteryHealthPercent(b: BatteryInfo): Int {
        return if (b.scale > 0) (b.level * 100 / b.scale) else -1
    }

    // ---------- 传感器列表 ----------
    fun sensorList(ctx: Context): List<String> {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        return sm.getSensorList(android.hardware.Sensor.TYPE_ALL).map { "${it.name} (${it.vendor})" }
    }

    // ---------- 应用列表 ----------
    data class AppItem(
        val pkg: String,
        val name: String,
        val versionName: String,
        val versionCode: Long,
        val sourceDir: String,
        val systemApp: Boolean,
        val installTime: Long,
        val updateTime: Long,
        val targetSdk: Int,
        val minSdk: Int,
        val sizeBytes: Long
    )

    @SuppressLint("QueryAllPackagesPermission")
    fun installedApps(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        val flags = PackageManager.GET_META_DATA
        val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(flags)
        }
        return list.mapNotNull { pi ->
            try {
                val app = pi.applicationInfo ?: return@mapNotNull null
                val name = pm.getApplicationLabel(app).toString()
                val systemApp = (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()
                val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) app.minSdkVersion else android.os.Build.VERSION_CODES.BASE
                val targetSdk = app.targetSdkVersion
                var size = 0L
                try { size = java.io.File(app.sourceDir).length() } catch (e: Exception) {}
                AppItem(
                    pi.packageName, name,
                    pi.versionName ?: "", versionCode,
                    app.sourceDir, systemApp,
                    pi.firstInstallTime, pi.lastUpdateTime,
                    targetSdk, minSdk, size
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.name.lowercase() }
    }
}
