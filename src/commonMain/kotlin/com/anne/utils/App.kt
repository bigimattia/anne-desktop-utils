package com.anne.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

enum class Screen {
    DESKTOP_SWITCHER,
    SETTINGS
}

@Composable
fun App(hotkeyManager: HotkeyManager, prefs: AppPreferences) {
    var currentScreen by remember { mutableStateOf(Screen.DESKTOP_SWITCHER) }
    var isEnabled by remember { mutableStateOf(hotkeyManager.isRunning) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Row(Modifier.fillMaxSize()) {
            // Sidebar
            Column(
                Modifier.width(200.dp).fillMaxHeight().background(Color(0xFF2C2C2C)).padding(16.dp)
            ) {
                Text(
                    text = "Anne Utils",
                    color = Color.White,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Button(
                    onClick = { currentScreen = Screen.DESKTOP_SWITCHER },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (currentScreen == Screen.DESKTOP_SWITCHER) Color(0xFF444444) else Color.Transparent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    elevation = null
                ) {
                    Text("Desktop Switcher")
                }

                Button(
                    onClick = { currentScreen = Screen.SETTINGS },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (currentScreen == Screen.SETTINGS) Color(0xFF444444) else Color.Transparent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = null
                ) {
                    Text("Settings")
                }
            }

            // Main Content
            Box(Modifier.fillMaxSize().background(Color(0xFF1E1E1E)).padding(32.dp)) {
                when (currentScreen) {
                    Screen.DESKTOP_SWITCHER -> {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Virtual Desktop Hotkeys",
                                color = Color.White,
                                style = MaterialTheme.typography.h4,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Text(
                                text = "Win/Cmd + 0-9 switches desktops. On Windows, add Shift to move the active window.",
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 32.dp)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Status: ", color = Color.White)
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            val success = hotkeyManager.start()
                                            if (success) {
                                                isEnabled = true
                                            } else {
                                                isEnabled = false
                                                showPermissionDialog = true
                                            }
                                        } else {
                                            hotkeyManager.stop()
                                            isEnabled = false
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF4CAF50),
                                        checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                                        uncheckedThumbColor = Color(0xFFF44336),
                                        uncheckedTrackColor = Color(0xFFF44336).copy(alpha = 0.5f)
                                    )
                                )
                                Text(
                                    text = if (isEnabled) "Active" else "Inactive",
                                    color = if (isEnabled) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                    Screen.SETTINGS -> {
                        Column(Modifier.fillMaxSize()) {
                            Text(
                                text = "Settings",
                                color = Color.White,
                                style = MaterialTheme.typography.h4,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            var startup by remember { mutableStateOf(prefs.startAtStartup) }
                            var tray by remember { mutableStateOf(prefs.showTrayIcon) }
                            var closeToTray by remember { mutableStateOf(prefs.closeToTray) }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                                Checkbox(
                                    checked = startup,
                                    onCheckedChange = { 
                                        startup = it
                                        prefs.startAtStartup = it
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50), uncheckedColor = Color.Gray)
                                )
                                Text("Start application at startup", color = Color.White, modifier = Modifier.padding(start = 8.dp))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                                Checkbox(
                                    checked = tray,
                                    onCheckedChange = { 
                                        tray = it
                                        prefs.showTrayIcon = it
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50), uncheckedColor = Color.Gray)
                                )
                                Text("Show application tray icon (Persistent)", color = Color.White, modifier = Modifier.padding(start = 8.dp))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                                Checkbox(
                                    checked = closeToTray,
                                    onCheckedChange = { 
                                        closeToTray = it
                                        prefs.closeToTray = it
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50), uncheckedColor = Color.Gray)
                                )
                                Text("Close application to tray icon", color = Color.White, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Permission Required") },
                text = { Text("Failed to enable access for assistive devices. Please go to System Settings -> Privacy & Security -> Accessibility and grant permission to this application or your terminal.") },
                confirmButton = {
                    Button(onClick = { showPermissionDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
