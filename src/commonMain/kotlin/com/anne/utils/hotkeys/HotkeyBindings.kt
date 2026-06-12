package com.anne.utils.hotkeys

enum class BindingMode {
    DIGIT_SEQUENCE,
    INDIVIDUAL
}

data class ShortcutModifiers(
    val control: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
    val meta: Boolean = false
) {
    val isEmpty: Boolean
        get() = !control && !alt && !shift && !meta
}

data class ShortcutBinding(
    val keyCode: Int,
    val keyText: String,
    val modifiers: ShortcutModifiers
) {
    fun displayText(): String = buildList {
        if (modifiers.control) add("Ctrl")
        if (modifiers.alt) add("Alt")
        if (modifiers.shift) add("Shift")
        if (modifiers.meta) add("Win/Cmd")
        add(keyText)
    }.joinToString(" + ")
}

data class HotkeyBindings(
    val mode: BindingMode,
    val sequenceModifiers: ShortcutModifiers,
    val individualBindings: Map<Int, ShortcutBinding>
)
