package com.example.aichat.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.RandomAccessFile

data class SystemStats(
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val cpuUsagePercent: Float
)

class SystemStatsManager(private val context: Context) {
    
    fun getStats(): SystemStats {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }
        
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRamMb = memoryInfo.totalMem / (1024 * 1024)
        val availableRamMb = memoryInfo.availMem / (1024 * 1024)

        return SystemStats(
            batteryPercentage = batteryPct?.toInt() ?: 0,
            isCharging = isCharging,
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            cpuUsagePercent = getCpuUsage()
        )
    }

    private fun getCpuUsage(): Float {
        try {
            val reader = RandomAccessFile("/proc/stat", "r")
            var load = reader.readLine()
            var toks = load.split(" +".toRegex())
            var idle1 = toks[4].toLong()
            var cpu1 = toks[1].toLong() + toks[2].toLong() + toks[3].toLong() + toks[5].toLong() + toks[6].toLong() + toks[7].toLong()

            try {
                Thread.sleep(360)
            } catch (e: Exception) {}

            reader.seek(0)
            load = reader.readLine()
            reader.close()

            toks = load.split(" +".toRegex())
            val idle2 = toks[4].toLong()
            val cpu2 = toks[1].toLong() + toks[2].toLong() + toks[3].toLong() + toks[5].toLong() + toks[6].toLong() + toks[7].toLong()

            val cpuDelta = (cpu2 - cpu1).toFloat()
            val idleDelta = (idle2 - idle1).toFloat()
            val totalDelta = cpuDelta + idleDelta
            
            if (totalDelta == 0f) return 0f
            return (cpuDelta / totalDelta) * 100f
        } catch (ex: Exception) {
            return 0f
        }
    }
}
