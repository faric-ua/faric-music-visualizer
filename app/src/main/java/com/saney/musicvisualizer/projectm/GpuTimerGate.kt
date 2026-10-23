package com.saney.musicvisualizer.projectm

import android.content.Context
import android.content.SharedPreferences

/**
 * V168 (faric 23.10 19:17–19:19, v0.21.27: «падіння в C++», двічі): the V165 graphics timer is new,
 * talks straight to the phone's graphics driver, and a driver bug there kills the whole app. A safety
 * latch: before the first timer call «trying» is written to disk (synchronously); after ~10 s of good
 * results — «ok». If the app starts and finds «trying», the last try ended in a crash → «off» for good
 * on this phone (the meter then shows the «≈» estimate). States: none · trying · ok · off.
 */
object GpuTimerGate {
    private const val PREFS = "projectm_gpu_timer"
    private const val KEY = "state"
    const val GOOD_RESULTS_FOR_OK = 600

    @Volatile private var prefs: SharedPreferences? = null

    @Volatile var allowed: Boolean = false
        private set

    @Volatile private var state: String? = null
    private var checked = false

    /** Once per process, before the first GL frame. */
    @Synchronized
    fun init(context: Context) {
        if (checked) return
        checked = true
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        state = p.getString(KEY, null)
        if (state == "trying") {
            state = "off"
            p.edit().putString(KEY, "off").commit()
        }
        allowed = state != "off"
    }

    /** GL thread, right before the first timer call of this process. */
    fun markTrying() {
        if (state == "ok" || state == "trying") return
        state = "trying"
        prefs?.edit()?.putString(KEY, "trying")?.commit()
    }

    fun markOk() {
        if (state == "ok") return
        state = "ok"
        prefs?.edit()?.putString(KEY, "ok")?.apply()
    }
}
