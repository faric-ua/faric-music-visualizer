package com.saney.musicvisualizer

import android.app.Application
import com.saney.musicvisualizer.diagnostics.CrashJournal

/**
 * Process entry point. Installs the crash journal before any Activity or the
 * media service starts, so even an early crash is recorded (TODO.md V24).
 */
class FaricApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashJournal.install(this, BuildConfig.VERSION_NAME + " / build " + BuildConfig.VERSION_CODE + " · код " + BuildConfig.GIT_SHA)
    }
}
