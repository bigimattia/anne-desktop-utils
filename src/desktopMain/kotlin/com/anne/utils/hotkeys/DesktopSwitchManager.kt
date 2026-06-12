package com.anne.utils.hotkeys

import com.anne.utils.HotkeyManager
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.NativeInputEvent
import com.github.kwhat.jnativehook.dispatcher.VoidDispatchService
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import java.awt.Robot
import java.awt.event.KeyEvent
import java.io.File
import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Level
import java.util.logging.Logger

class DesktopSwitchManager : HotkeyManager, NativeKeyListener {
    private var running = false
    override val isRunning: Boolean get() = running

    private val pressedShortcutKeys = mutableSetOf<Int>()
    private var suppressMetaRelease = false
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
            if (!GlobalScreen.isNativeHookRegistered()) {
                // Event consumption must happen synchronously before the native hook returns.
                GlobalScreen.setEventDispatcher(VoidDispatchService())
                GlobalScreen.registerNativeHook()
            }
            GlobalScreen.addNativeKeyListener(this)
            running = true
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
        running = false
    }

    override fun nativeKeyPressed(event: NativeKeyEvent) {
        val hasMeta = (event.modifiers and NativeInputEvent.META_MASK) != 0
        if (!hasMeta) return

        val desktopNumber = desktopNumberFor(event.keyCode)
        if (desktopNumber == -1) return

        consume(event)
        suppressMetaRelease = true

        if (pressedShortcutKeys.add(event.keyCode)) {
            val hasShift = (event.modifiers and NativeInputEvent.SHIFT_MASK) != 0
            if (isWindows && hasShift) {
                moveActiveWindowToWindowsDesktop(desktopNumber)
            } else {
                switchToDesktop(desktopNumber)
            }
        }
    }

    override fun nativeKeyReleased(event: NativeKeyEvent) {
        if (event.keyCode == NativeKeyEvent.VC_META && suppressMetaRelease) {
            consume(event)
            suppressMetaRelease = false
            return
        }

        if (pressedShortcutKeys.remove(event.keyCode)) {
            consume(event)
        }
    }

    override fun nativeKeyTyped(event: NativeKeyEvent) = Unit

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
}
