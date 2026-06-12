package com.anne.utils.preferences

import com.anne.utils.AppPreferences
import com.anne.utils.startup.StartupManager
import java.util.prefs.Preferences

class AppPreferencesImpl(
    private val startupManager: StartupManager = StartupManager()
) : AppPreferences {
    private val preferences = Preferences.userRoot().node("com.anne.utils")

    init {
        if (startAtStartup) {
            startupManager.update(true)
        }
    }

    override var startAtStartup: Boolean
        get() = preferences.getBoolean("startAtStartup", false)
        set(value) {
            preferences.putBoolean("startAtStartup", value)
            startupManager.update(value)
        }

    override var showTrayIcon: Boolean
        get() = preferences.getBoolean("showTrayIcon", true)
        set(value) {
            preferences.putBoolean("showTrayIcon", value)
        }

    override var closeToTray: Boolean
        get() = preferences.getBoolean("closeToTray", true)
        set(value) {
            preferences.putBoolean("closeToTray", value)
        }
}
