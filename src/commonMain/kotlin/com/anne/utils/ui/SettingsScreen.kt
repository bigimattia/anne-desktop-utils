package com.anne.utils.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anne.utils.AppPreferences
import com.anne.utils.localization.AppStrings

@Composable
internal fun SettingsScreen(
    preferences: AppPreferences,
    strings: AppStrings,
    onShowTrayIconChanged: (Boolean) -> Unit
) {
    var startup by remember { mutableStateOf(preferences.startAtStartup) }
    var tray by remember { mutableStateOf(preferences.showTrayIcon) }
    var closeToTray by remember { mutableStateOf(preferences.closeToTray) }

    Column(Modifier.fillMaxSize()) {
        Text(
            text = strings.settings,
            color = Color.White,
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        SettingCheckbox(
            checked = startup,
            label = strings.startAtStartupInTray,
            onCheckedChange = {
                startup = it
                preferences.startAtStartup = it
                if (it && !tray) {
                    tray = true
                    preferences.showTrayIcon = true
                    onShowTrayIconChanged(true)
                }
            }
        )
        SettingCheckbox(
            checked = tray,
            label = strings.showTrayIcon,
            onCheckedChange = {
                tray = it
                preferences.showTrayIcon = it
                onShowTrayIconChanged(it)
                if (!it && startup) {
                    startup = false
                    preferences.startAtStartup = false
                }
            }
        )
        SettingCheckbox(
            checked = closeToTray,
            label = strings.closeToTray,
            onCheckedChange = {
                closeToTray = it
                preferences.closeToTray = it
            }
        )
    }
}

@Composable
private fun SettingCheckbox(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF4CAF50),
                uncheckedColor = Color.Gray
            )
        )
        Text(
            text = label,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
