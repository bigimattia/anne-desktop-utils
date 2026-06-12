package com.anne.utils.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anne.utils.AppPreferences
import com.anne.utils.HotkeyManager
import com.anne.utils.localization.AppStrings

private enum class Screen {
    DESKTOP_SWITCHER,
    SETTINGS
}

@Composable
fun App(
    hotkeyManager: HotkeyManager,
    preferences: AppPreferences,
    strings: AppStrings,
    onShowTrayIconChanged: (Boolean) -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.DESKTOP_SWITCHER) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Row(Modifier.fillMaxSize()) {
            Sidebar(
                currentScreen = currentScreen,
                strings = strings,
                onScreenSelected = { currentScreen = it }
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .padding(32.dp)
            ) {
                when (currentScreen) {
                    Screen.DESKTOP_SWITCHER -> DesktopSwitcherScreen(
                        hotkeyManager = hotkeyManager,
                        preferences = preferences,
                        strings = strings,
                        onPermissionDenied = { showPermissionDialog = true }
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        preferences = preferences,
                        strings = strings,
                        onShowTrayIconChanged = onShowTrayIconChanged
                    )
                }
            }
        }

        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text(strings.permissionRequired) },
                text = { Text(strings.permissionMessage) },
                confirmButton = {
                    Button(onClick = { showPermissionDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun Sidebar(
    currentScreen: Screen,
    strings: AppStrings,
    onScreenSelected: (Screen) -> Unit
) {
    Column(
        Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(Color(0xFF2C2C2C))
            .padding(16.dp)
    ) {
        Text(
            text = "Anne Utils",
            color = Color.White,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        NavigationButton(
            text = strings.desktopSwitcher,
            selected = currentScreen == Screen.DESKTOP_SWITCHER,
            modifier = Modifier.padding(bottom = 8.dp),
            onClick = { onScreenSelected(Screen.DESKTOP_SWITCHER) }
        )
        NavigationButton(
            text = strings.settings,
            selected = currentScreen == Screen.SETTINGS,
            onClick = { onScreenSelected(Screen.SETTINGS) }
        )
    }
}

@Composable
private fun NavigationButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (selected) Color(0xFF444444) else Color.Transparent,
            contentColor = Color.White
        ),
        modifier = modifier.fillMaxWidth(),
        elevation = null
    ) {
        Text(text)
    }
}
