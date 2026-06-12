package com.anne.utils

import com.anne.utils.hotkeys.HotkeyBindings
import com.anne.utils.hotkeys.ShortcutBinding

interface HotkeyManager {
    fun start(): Boolean
    fun stop()
    fun updateBindings(bindings: HotkeyBindings)
    fun captureShortcut(onCaptured: (ShortcutBinding) -> Unit)
    fun cancelCapture()
    val isRunning: Boolean
}

interface AppPreferences {
    var startAtStartup: Boolean
    var showTrayIcon: Boolean
    var closeToTray: Boolean
    var hotkeyBindings: HotkeyBindings
}
