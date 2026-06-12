package com.anne.utils.hotkeys

import com.anne.utils.HotkeyManager
import com.anne.utils.hotkeys.BindingMode.DIGIT_SEQUENCE
import com.anne.utils.hotkeys.BindingMode.INDIVIDUAL
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.NativeInputEvent
import com.github.kwhat.jnativehook.dispatcher.VoidDispatchService
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import java.awt.Robot
import java.awt.EventQueue
import java.awt.event.KeyEvent
import java.io.File
import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Level
import java.util.logging.Logger

class DesktopSwitchManager(
    initialBindings: HotkeyBindings
) : HotkeyManager, NativeKeyListener {
    private var running = false
    override val isRunning: Boolean get() = running

    private var bindings = initialBindings
    private var captureCallback: ((ShortcutBinding) -> Unit)? = null
    private var captureOnlyListener = false
    private var capturedKeyAwaitingRelease: Int? = null
    private val pressedShortcutKeys = mutableSetOf<Int>()
    private var suppressMetaRelease = false
    private var suppressAltRelease = false
    private val reservedField: Field = NativeInputEvent::class.java
        .getDeclaredField("reserved")
        .apply { isAccessible = true }

    private val isMac = System.getProperty("os.name").lowercase().contains("mac")
    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    private val windowsDesktopHelper: File by lazy { extractWindowsDesktopHelper() }

    init {
        val logger = Logger.getLogger(GlobalScreen::class.java.getPackage().name)
        logger.level = Level.OFF
        logger.useParentHandlers = false
    }

    override fun start(): Boolean {
        if (running) return true

        return try {
            ensureListenerRegistered()
            running = true
            captureOnlyListener = false
            true
        } catch (exception: NativeHookException) {
            exception.printStackTrace()
            false
        } catch (exception: Exception) {
            exception.printStackTrace()
            false
        }
    }

    override fun stop() {
        if (!running) return

        GlobalScreen.removeNativeKeyListener(this)
        pressedShortcutKeys.clear()
        suppressMetaRelease = false
        suppressAltRelease = false
        captureCallback = null
        captureOnlyListener = false
        capturedKeyAwaitingRelease = null
        running = false
    }

    override fun updateBindings(bindings: HotkeyBindings) {
        this.bindings = bindings
    }

    override fun captureShortcut(onCaptured: (ShortcutBinding) -> Unit) {
        captureCallback = onCaptured
        if (!running) {
            try {
                ensureListenerRegistered()
                captureOnlyListener = true
            } catch (exception: Exception) {
                captureCallback = null
                exception.printStackTrace()
            }
        }
    }

    override fun cancelCapture() {
        captureCallback = null
        removeCaptureOnlyListener()
    }

    override fun nativeKeyPressed(event: NativeKeyEvent) {
        val callback = captureCallback
        if (callback != null) {
            if (isModifierKey(event.keyCode)) return

            consume(event)
            val capturedModifiers = modifiersFrom(event.modifiers)
            suppressMetaRelease = capturedModifiers.meta
            suppressAltRelease = capturedModifiers.alt
            capturedKeyAwaitingRelease = event.keyCode
            captureCallback = null
            val shortcut = ShortcutBinding(
                keyCode = event.keyCode,
                keyText = NativeKeyEvent.getKeyText(event.keyCode),
                modifiers = capturedModifiers
            )
            EventQueue.invokeLater { callback(shortcut) }
            return
        }

        val match = findMatch(event) ?: return

        consume(event)
        suppressMetaRelease = match.binding.modifiers.meta
        suppressAltRelease = match.binding.modifiers.alt

        if (pressedShortcutKeys.add(event.keyCode)) {
            if (match.moveWindow) {
                moveActiveWindowToWindowsDesktop(match.desktop)
            } else {
                switchToDesktop(match.desktop)
            }
        }
    }

    override fun nativeKeyReleased(event: NativeKeyEvent) {
        if (event.keyCode == capturedKeyAwaitingRelease) {
            consume(event)
            capturedKeyAwaitingRelease = null
            if (
                !suppressMetaRelease &&
                !suppressAltRelease &&
                modifiersFrom(event.modifiers).isEmpty
            ) {
                removeCaptureOnlyListener()
            }
            return
        }

        if (event.keyCode == NativeKeyEvent.VC_META && suppressMetaRelease) {
            consume(event)
            suppressMetaRelease = false
            if (!suppressAltRelease) removeCaptureOnlyListener()
            return
        }

        if (event.keyCode == NativeKeyEvent.VC_ALT && suppressAltRelease) {
            consume(event)
            suppressAltRelease = false
            if (!suppressMetaRelease) removeCaptureOnlyListener()
            return
        }

        if (pressedShortcutKeys.remove(event.keyCode)) {
            consume(event)
        }

        if (
            captureOnlyListener &&
            capturedKeyAwaitingRelease == null &&
            !suppressMetaRelease &&
            !suppressAltRelease &&
            isModifierKey(event.keyCode)
        ) {
            removeCaptureOnlyListener()
        }
    }

    override fun nativeKeyTyped(event: NativeKeyEvent) = Unit

    private fun findMatch(event: NativeKeyEvent): ShortcutMatch? {
        val actualModifiers = modifiersFrom(event.modifiers)
        return when (bindings.mode) {
            DIGIT_SEQUENCE -> {
                val desktop = desktopNumberFor(event.keyCode)
                if (desktop == -1 || bindings.sequenceModifiers.isEmpty) return null
                matchBinding(
                    desktop = desktop,
                    binding = ShortcutBinding(
                        event.keyCode,
                        NativeKeyEvent.getKeyText(event.keyCode),
                        bindings.sequenceModifiers
                    ),
                    actualModifiers = actualModifiers
                )
            }

            INDIVIDUAL -> bindings.individualBindings.entries.firstNotNullOfOrNull {
                (desktop, binding) ->
                if (binding.keyCode == event.keyCode) {
                    matchBinding(desktop, binding, actualModifiers)
                } else {
                    null
                }
            }
        }
    }

    private fun matchBinding(
        desktop: Int,
        binding: ShortcutBinding,
        actualModifiers: ShortcutModifiers
    ): ShortcutMatch? {
        if (actualModifiers == binding.modifiers) {
            return ShortcutMatch(desktop, binding, moveWindow = false)
        }

        val moveModifiers = binding.modifiers.copy(shift = true)
        if (
            isWindows &&
            !binding.modifiers.shift &&
            actualModifiers == moveModifiers
        ) {
            return ShortcutMatch(desktop, binding, moveWindow = true)
        }
        return null
    }

    private fun desktopNumberFor(keyCode: Int): Int = when (keyCode) {
        NativeKeyEvent.VC_1 -> 1
        NativeKeyEvent.VC_2 -> 2
        NativeKeyEvent.VC_3 -> 3
        NativeKeyEvent.VC_4 -> 4
        NativeKeyEvent.VC_5 -> 5
        NativeKeyEvent.VC_6 -> 6
        NativeKeyEvent.VC_7 -> 7
        NativeKeyEvent.VC_8 -> 8
        NativeKeyEvent.VC_9 -> 9
        NativeKeyEvent.VC_0 -> 10
        else -> -1
    }

    private fun consume(event: NativeKeyEvent) {
        reservedField.setShort(event, 0x01.toShort())
    }

    private fun modifiersFrom(value: Int): ShortcutModifiers = ShortcutModifiers(
        control = value and NativeInputEvent.CTRL_MASK != 0,
        alt = value and NativeInputEvent.ALT_MASK != 0,
        shift = value and NativeInputEvent.SHIFT_MASK != 0,
        meta = value and NativeInputEvent.META_MASK != 0
    )

    private fun isModifierKey(keyCode: Int): Boolean = keyCode in setOf(
        NativeKeyEvent.VC_CONTROL,
        NativeKeyEvent.VC_ALT,
        NativeKeyEvent.VC_SHIFT,
        NativeKeyEvent.VC_META
    )

    private fun ensureListenerRegistered() {
        if (!GlobalScreen.isNativeHookRegistered()) {
            // Event consumption must happen synchronously before the native hook returns.
            GlobalScreen.setEventDispatcher(VoidDispatchService())
            GlobalScreen.registerNativeHook()
        }
        GlobalScreen.removeNativeKeyListener(this)
        GlobalScreen.addNativeKeyListener(this)
    }

    private fun removeCaptureOnlyListener() {
        if (captureOnlyListener) {
            GlobalScreen.removeNativeKeyListener(this)
            captureOnlyListener = false
            capturedKeyAwaitingRelease = null
        }
    }

    private fun switchToDesktop(desktop: Int) {
        when {
            isMac -> switchMacDesktop(desktop)
            isWindows -> runWindowsDesktopCommand("switch", (desktop - 1).toString())
        }
    }

    private fun switchMacDesktop(desktop: Int) {
        try {
            val keyCode = when (desktop) {
                1 -> KeyEvent.VK_1
                2 -> KeyEvent.VK_2
                3 -> KeyEvent.VK_3
                4 -> KeyEvent.VK_4
                5 -> KeyEvent.VK_5
                6 -> KeyEvent.VK_6
                7 -> KeyEvent.VK_7
                8 -> KeyEvent.VK_8
                9 -> KeyEvent.VK_9
                10 -> KeyEvent.VK_0
                else -> return
            }
            Robot().run {
                keyPress(KeyEvent.VK_CONTROL)
                keyPress(keyCode)
                keyRelease(keyCode)
                keyRelease(KeyEvent.VK_CONTROL)
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun moveActiveWindowToWindowsDesktop(desktop: Int) {
        runWindowsDesktopCommand("move", (desktop - 1).toString())
    }

    private fun runWindowsDesktopCommand(vararg arguments: String) {
        try {
            ProcessBuilder(windowsDesktopHelper.absolutePath, *arguments).start()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun extractWindowsDesktopHelper(): File {
        val resource = checkNotNull(
            DesktopSwitchManager::class.java.getResourceAsStream("/windows/AnneVirtualDesktop.exe")
        ) {
            "Embedded Windows virtual desktop helper not found."
        }

        val helperDir = File(System.getProperty("java.io.tmpdir"), "anne-desktop-utils")
        check(helperDir.exists() || helperDir.mkdirs()) {
            "Unable to create helper directory: ${helperDir.absolutePath}"
        }

        val helper = File(helperDir, "AnneVirtualDesktop.exe")
        resource.use {
            Files.copy(it, helper.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        helper.deleteOnExit()
        return helper
    }

    private data class ShortcutMatch(
        val desktop: Int,
        val binding: ShortcutBinding,
        val moveWindow: Boolean
    )
}
