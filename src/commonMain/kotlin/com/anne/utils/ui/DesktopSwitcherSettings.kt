package com.anne.utils.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
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
import com.anne.utils.HotkeyManager
import com.anne.utils.hotkeys.BindingMode
import com.anne.utils.hotkeys.HotkeyBindings
import com.anne.utils.hotkeys.ShortcutBinding
import com.anne.utils.hotkeys.ShortcutModifiers
import com.anne.utils.localization.AppStrings

@Composable
internal fun DesktopSwitcherSettings(
    hotkeyManager: HotkeyManager,
    preferences: AppPreferences,
    strings: AppStrings
) {
    var bindings by remember { mutableStateOf(preferences.hotkeyBindings) }
    var capturingDesktop by remember { mutableStateOf<Int?>(null) }

    fun saveBindings(updated: HotkeyBindings) {
        bindings = updated
        preferences.hotkeyBindings = updated
        hotkeyManager.updateBindings(updated)
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = strings.shortcutBindings,
            color = Color.White,
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        BindingModeOption(
            selected = bindings.mode == BindingMode.DIGIT_SEQUENCE,
            label = strings.digitSequenceMode,
            onSelected = {
                hotkeyManager.cancelCapture()
                capturingDesktop = null
                saveBindings(bindings.copy(mode = BindingMode.DIGIT_SEQUENCE))
            }
        )
        BindingModeOption(
            selected = bindings.mode == BindingMode.INDIVIDUAL,
            label = strings.individualMode,
            onSelected = {
                saveBindings(bindings.copy(mode = BindingMode.INDIVIDUAL))
            }
        )

        when (bindings.mode) {
            BindingMode.DIGIT_SEQUENCE -> DigitSequenceSettings(
                modifiers = bindings.sequenceModifiers,
                strings = strings,
                onModifiersChanged = {
                    if (!it.isEmpty) {
                        saveBindings(bindings.copy(sequenceModifiers = it))
                    }
                }
            )

            BindingMode.INDIVIDUAL -> IndividualBindingsSettings(
                bindings = bindings.individualBindings,
                capturingDesktop = capturingDesktop,
                strings = strings,
                onCapture = { desktop ->
                    if (capturingDesktop == desktop) {
                        hotkeyManager.cancelCapture()
                        capturingDesktop = null
                    } else {
                        capturingDesktop = desktop
                        hotkeyManager.captureShortcut { shortcut ->
                            saveBindings(
                                bindings.copy(
                                    individualBindings = bindings.individualBindings +
                                        (desktop to shortcut)
                                )
                            )
                            capturingDesktop = null
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun BindingModeOption(
    selected: Boolean,
    label: String,
    onSelected: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected,
            onClick = onSelected,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF4CAF50),
                unselectedColor = Color.Gray
            )
        )
        Text(text = label, color = Color.White)
    }
}

@Composable
private fun DigitSequenceSettings(
    modifiers: ShortcutModifiers,
    strings: AppStrings,
    onModifiersChanged: (ShortcutModifiers) -> Unit
) {
    Text(
        text = strings.digitSequenceDescription,
        color = Color.Gray,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    Text(
        text = strings.modifiers,
        color = Color.White,
        style = MaterialTheme.typography.subtitle1
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        ModifierCheckbox(modifiers.control, strings.controlKey) {
            onModifiersChanged(modifiers.copy(control = it))
        }
        ModifierCheckbox(modifiers.alt, strings.altKey) {
            onModifiersChanged(modifiers.copy(alt = it))
        }
        ModifierCheckbox(modifiers.shift, strings.shiftKey) {
            onModifiersChanged(modifiers.copy(shift = it))
        }
        ModifierCheckbox(modifiers.meta, strings.metaKey) {
            onModifiersChanged(modifiers.copy(meta = it))
        }
    }
}

@Composable
private fun ModifierCheckbox(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF4CAF50),
                uncheckedColor = Color.Gray
            )
        )
        Text(text = label, color = Color.White)
    }
}

@Composable
private fun IndividualBindingsSettings(
    bindings: Map<Int, ShortcutBinding>,
    capturingDesktop: Int?,
    strings: AppStrings,
    onCapture: (Int) -> Unit
) {
    Text(
        text = strings.individualDescription,
        color = Color.Gray,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    (1..10).forEach { desktop ->
        val binding = bindings.getValue(desktop)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "${strings.desktop} $desktop",
                color = Color.White,
                modifier = Modifier.width(100.dp)
            )
            Text(
                text = binding.displayText(),
                color = Color.LightGray,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { onCapture(desktop) }) {
                Text(
                    if (capturingDesktop == desktop) strings.cancel
                    else strings.recordShortcut
                )
            }
        }
    }

    if (capturingDesktop != null) {
        Text(
            text = strings.pressShortcut,
            color = Color(0xFF4CAF50),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
