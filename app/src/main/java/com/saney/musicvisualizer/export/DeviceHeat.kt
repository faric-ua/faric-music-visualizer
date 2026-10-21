package com.saney.musicvisualizer.export

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

/**
 * V115 (faric 21.10 19:09: «довго йде»): a hot phone slows the GPU and the encoder down by itself.
 * The export report shows the battery temperature and Android's own heat level at start and end,
 * so a slow export can be told apart: «телефон гарячий» vs «важка візуалізація».
 */
object DeviceHeat {
    fun read(context: Context): String {
        val battery =
            runCatching {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                    ?.takeIf { it != Int.MIN_VALUE }
                    ?.let { tenths -> "%.1f °C".format(java.util.Locale.ROOT, tenths / 10f) }
            }.getOrNull() ?: "? °C"
        val level =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    when ((context.getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus) {
                        PowerManager.THERMAL_STATUS_NONE -> "норма"
                        PowerManager.THERMAL_STATUS_LIGHT -> "теплий"
                        PowerManager.THERMAL_STATUS_MODERATE -> "гарячий"
                        PowerManager.THERMAL_STATUS_SEVERE -> "дуже гарячий (сповільнює)"
                        PowerManager.THERMAL_STATUS_CRITICAL -> "критично"
                        else -> "аварійно"
                    }
                }.getOrNull() ?: "?"
            } else {
                "?"
            }
        return "$battery · $level"
    }
}
