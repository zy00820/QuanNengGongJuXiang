package com.qngxj.toolbox.util

/**
 * Root 检测与命令执行工具。
 *
 * 用于"内置 Shizuku"功能：检测设备是否已 root，
 * 并通过 su 执行 app_process 启动 Shizuku 服务端，无需安装 Shizuku 应用。
 */
object RootUtils {

    /** 检测设备是否已 root（su 可用） */
    fun isRooted(): Boolean {
        return try {
            // 通过 which su 检测
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val out = process.inputStream.bufferedReader().readText().trim()
            val code = process.waitFor()
            code == 0 && out.isNotEmpty()
        } catch (e: Exception) {
            // 回退：直接尝试 su -c id
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                val code = process.waitFor()
                code == 0
            } catch (e2: Exception) {
                false
            }
        }
    }

    /**
     * 通过 su 执行命令（root 权限）。
     * @param cmd 命令字符串
     * @return Pair(success, output)
     */
    fun execRoot(cmd: String): Pair<Boolean, String> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val input = process.inputStream.bufferedReader().readText()
            val err = process.errorStream.bufferedReader().readText()
            val code = process.waitFor()
            val out = if (input.isNotEmpty()) input else err
            Pair(code == 0, out)
        } catch (e: Exception) {
            Pair(false, e.message ?: "执行异常")
        }
    }
}
