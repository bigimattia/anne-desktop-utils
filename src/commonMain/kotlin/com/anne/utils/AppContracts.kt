package com.anne.utils

interface HotkeyManager {
    fun start(): Boolean
    fun stop()
    val isRunning: Boolean
}

interface AppPreferences {
    var startAtStartup: Boolean
    var showTrayIcon: Boolean
    var closeToTray: Boolean
}
