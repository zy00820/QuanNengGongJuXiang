package com.qngxj.toolbox.ui.hardware

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.qngxj.toolbox.adapter.HardwareCard
import com.qngxj.toolbox.adapter.HardwareCardAdapter
import com.qngxj.toolbox.databinding.FragmentHardwareBinding
import com.qngxj.toolbox.util.DeviceUtils

class HardwareFragment : Fragment() {

    private var _binding: FragmentHardwareBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHardwareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.refresh.setOnRefreshListener {
            load()
            binding.refresh.isRefreshing = false
        }
        load()
    }

    private fun load() {
        val ctx = binding.root.context
        val cards = mutableListOf<HardwareCard>()

        // 设备型号
        val dev = DeviceUtils.deviceInfo()
        cards.add(HardwareCard(getString(com.qngxj.toolbox.R.string.hw_device), listOf(
            "品牌" to dev.brand,
            "厂商" to dev.manufacturer,
            "型号" to dev.model,
            "主板" to dev.board,
            "设备代号" to dev.device,
            "Build ID" to dev.buildId
        )))

        // CPU
        val cpu = DeviceUtils.cpuInfo()
        cards.add(HardwareCard(getString(com.qngxj.toolbox.R.string.hw_cpu), listOf(
            "处理器" to cpu.processor,
            "硬件" to cpu.hardware,
            "架构" to cpu.arch,
            "指令集" to cpu.abi,
            "核心数" to "${cpu.cores} 核",
            "最高频率" to cpu.freqMax
        )))

        // GPU
        cards.add(HardwareCard(getString(com.qngxj.toolbox.R.string.hw_gpu), listOf(
            "GPU 渲染器" to DeviceUtils.gpuInfo(),
            "OpenGL 版本" to DeviceUtils.glVersion()
        )))

        // 屏幕
        val scr = DeviceUtils.screenInfo(ctx)
        cards.add(HardwareCard(getString(com.qngxj.toolbox.R.string.hw_screen), listOf(
            "分辨率" to scr.resolution,
            "屏幕尺寸" to scr.sizeInch,
            "刷新率" to scr.refreshRate,
            "屏幕密度" to scr.density,
            "DPI" to "${scr.dpi} dpi"
        )))

        // 内存存储
        val mem = DeviceUtils.memInfo(ctx)
        cards.add(HardwareCard(getString(com.qngxj.toolbox.R.string.hw_memory), listOf(
            "总内存 RAM" to mem.totalRam,
            "可用内存" to mem.availRam,
            "总存储" to mem.totalStorage,
            "可用存储" to mem.availStorage
        )))

        // 电池
        val bat = DeviceUtils.batteryInfoFromIntent(ctx)
        val healthPct = DeviceUtils.batteryHealthPercent(bat)
        cards.add(HardwareCard(getString(com.qngxj.toolbox.R.string.hw_battery), listOf(
            "电量" to (if (healthPct >= 0) "$healthPct%" else "未知"),
            "状态" to bat.status,
            "健康度" to bat.health,
            "电池技术" to bat.technology,
            "温度" to bat.temperature,
            "电压" to bat.voltage
        )))

        // 系统
        val os = DeviceUtils.osInfo()
        cards.add(HardwareCard(getString(com.qngxj.toolbox.R.string.hw_system), listOf(
            "Android 版本" to os.version,
            "SDK 版本" to "API ${os.sdk}",
            "安全补丁" to os.securityPatch,
            "构建时间" to os.buildTime,
            "指纹" to os.let { dev.fingerprint }
        )))

        // 传感器（取前若干个，避免过长）
        val sensors = DeviceUtils.sensorList(ctx)
        cards.add(HardwareCard(getString(com.qngxj.toolbox.R.string.hw_sensor), listOf(
            "传感器数量" to "${sensors.size} 个",
            "主要传感器" to (sensors.take(3).joinToString("\n") { "• $it" }.ifEmpty { "无" })
        )))

        val span = if (isTablet()) 2 else 1
        val lm = GridLayoutManager(ctx, span)
        binding.rv.layoutManager = lm
        binding.rv.adapter = HardwareCardAdapter(cards)
        binding.rv.itemAnimator?.changeDuration = 0
    }

    private fun isTablet(): Boolean {
        val w = resources.configuration.screenWidthDp
        return w >= 600
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
