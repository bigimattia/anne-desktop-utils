package com.anne.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.anne.utils.hotkeys.DesktopSwitchManager
import com.anne.utils.localization.AppLocalizations
import com.anne.utils.preferences.AppPreferencesImpl
import com.anne.utils.startup.START_IN_TRAY_ARGUMENT
import com.anne.utils.ui.App
import java.util.Locale

fun main(args: Array<String>) = application {
    val startInTray = args.any { it.equals(START_IN_TRAY_ARGUMENT, ignoreCase = true) }
    val preferences = remember { AppPreferencesImpl() }
    val hotkeyManager = remember { DesktopSwitchManager(preferences.hotkeyBindings) }
    if (startInTray && !preferences.showTrayIcon) {
        preferences.showTrayIcon = true
    }

    val strings = remember { AppLocalizations.forLanguage(Locale.getDefault().language) }
    var showTrayIcon by remember { mutableStateOf(preferences.showTrayIcon) }
    var isWindowVisible by remember { mutableStateOf(!startInTray) }
    val icon = painterResource("anne_icon.png")

    if (showTrayIcon) {
        Tray(
            icon = icon,
            tooltip = "Anne Utils",
            onAction = { isWindowVisible = true },
            menu = {
                Item(strings.open, onClick = { isWindowVisible = true })
                Item(
                    strings.exit,
                    onClick = {
                        hotkeyManager.stop()
                        exitApplication()
                    }
                )
            }
        )
    }

    if (isWindowVisible) {
        Window(
            onCloseRequest = {
                if (preferences.closeToTray && showTrayIcon) {
                    isWindowVisible = false
                } else {
                    hotkeyManager.stop()
                    exitApplication()
                }
            },
            title = "Anne Utils",
            icon = icon
        ) {
            App(
                hotkeyManager = hotkeyManager,
                preferences = preferences,
                strings = strings,
                onShowTrayIconChanged = { showTrayIcon = it }
            )
        }
    }
}
