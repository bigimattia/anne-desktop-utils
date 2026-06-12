package com.anne.utils.preferences

import com.anne.utils.AppPreferences
import com.anne.utils.hotkeys.BindingMode
import com.anne.utils.hotkeys.HotkeyBindings
import com.anne.utils.hotkeys.ShortcutBinding
import com.anne.utils.hotkeys.ShortcutModifiers
import com.anne.utils.startup.StartupManager
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
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

    override var hotkeyBindings: HotkeyBindings
        get() {
            val mode = runCatching {
                BindingMode.valueOf(
                    preferences.get("hotkeyMode", BindingMode.DIGIT_SEQUENCE.name)
                )
            }.getOrDefault(BindingMode.DIGIT_SEQUENCE)
            val sequenceModifiers = decodeModifiers(
                preferences.getInt("sequenceModifiers", META_MODIFIER)
            )
            val individualBindings = (1..10).associateWith { desktop ->
                val default = defaultBindingFor(desktop)
                ShortcutBinding(
                    keyCode = preferences.getInt("desktop.$desktop.keyCode", default.keyCode),
                    keyText = preferences.get("desktop.$desktop.keyText", default.keyText),
                    modifiers = decodeModifiers(
                        preferences.getInt(
                            "desktop.$desktop.modifiers",
                            encodeModifiers(default.modifiers)
                        )
                    )
                )
            }
            return HotkeyBindings(mode, sequenceModifiers, individualBindings)
        }
        set(value) {
            preferences.put("hotkeyMode", value.mode.name)
            preferences.putInt(
                "sequenceModifiers",
                encodeModifiers(value.sequenceModifiers)
            )
            value.individualBindings.forEach { (desktop, binding) ->
                preferences.putInt("desktop.$desktop.keyCode", binding.keyCode)
                preferences.put("desktop.$desktop.keyText", binding.keyText)
                preferences.putInt(
                    "desktop.$desktop.modifiers",
                    encodeModifiers(binding.modifiers)
                )
            }
        }

    private fun defaultBindingFor(desktop: Int): ShortcutBinding {
        val keyCode = when (desktop) {
            1 -> NativeKeyEvent.VC_1
            2 -> NativeKeyEvent.VC_2
            3 -> NativeKeyEvent.VC_3
            4 -> NativeKeyEvent.VC_4
            5 -> NativeKeyEvent.VC_5
            6 -> NativeKeyEvent.VC_6
            7 -> NativeKeyEvent.VC_7
            8 -> NativeKeyEvent.VC_8
            9 -> NativeKeyEvent.VC_9
            else -> NativeKeyEvent.VC_0
        }
        val keyText = if (desktop == 10) "0" else desktop.toString()
        return ShortcutBinding(
            keyCode = keyCode,
            keyText = keyText,
            modifiers = ShortcutModifiers(meta = true)
        )
    }

    private fun encodeModifiers(modifiers: ShortcutModifiers): Int =
        (if (modifiers.control) CONTROL_MODIFIER else 0) or
            (if (modifiers.alt) ALT_MODIFIER else 0) or
            (if (modifiers.shift) SHIFT_MODIFIER else 0) or
            (if (modifiers.meta) META_MODIFIER else 0)

    private fun decodeModifiers(value: Int): ShortcutModifiers = ShortcutModifiers(
        control = value and CONTROL_MODIFIER != 0,
        alt = value and ALT_MODIFIER != 0,
        shift = value and SHIFT_MODIFIER != 0,
        meta = value and META_MODIFIER != 0
    )

    private companion object {
        const val CONTROL_MODIFIER = 1
        const val ALT_MODIFIER = 2
        const val SHIFT_MODIFIER = 4
        const val META_MODIFIER = 8
    }
}
