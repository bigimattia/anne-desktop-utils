package com.anne.utils.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
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
import com.anne.utils.HotkeyManager
import com.anne.utils.localization.AppStrings

@Composable
internal fun DesktopSwitcherScreen(
    hotkeyManager: HotkeyManager,
    strings: AppStrings,
    onPermissionDenied: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(hotkeyManager.isRunning) }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = strings.virtualDesktopHotkeys,
            color = Color.White,
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = strings.hotkeyDescription,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${strings.status}: ", color = Color.White)
            Switch(
                checked = isEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        isEnabled = hotkeyManager.start()
                        if (!isEnabled) onPermissionDenied()
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
                text = if (isEnabled) strings.active else strings.inactive,
                color = if (isEnabled) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
