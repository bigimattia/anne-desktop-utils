package com.anne.utils

import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import java.awt.Robot
import java.awt.event.KeyEvent
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import java.util.prefs.Preferences

class DesktopSwitchManager : HotkeyManager, NativeKeyListener {
    private var _isRunning = false
    override val isRunning: Boolean get() = _isRunning

    private val isMac = System.getProperty("os.name").lowercase().contains("mac")
    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    init {
        val logger = Logger.getLogger(GlobalScreen::class.java.getPackage().name)
        logger.level = Level.OFF
        logger.useParentHandlers = false
    }

    override fun start(): Boolean {
        if (!_isRunning) {
            try {
                if (!GlobalScreen.isNativeHookRegistered()) {
                    GlobalScreen.registerNativeHook()
                }
                GlobalScreen.addNativeKeyListener(this)
                _isRunning = true
                return true
            } catch (e: NativeHookException) {
                e.printStackTrace()
                // Se c'è un errore (es. permessi Accessibilità su Mac), ritorniamo false
                return false
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
        return true
    }

    override fun stop() {
        if (_isRunning) {
            GlobalScreen.removeNativeKeyListener(this)
            _isRunning = false
        }
    }

    override fun nativeKeyPressed(e: NativeKeyEvent) {
        val modifiers = e.modifiers
        val hasMeta = (modifiers and NativeKeyEvent.VC_META) != 0

        if (hasMeta) {
            val desktopNumber = when (e.keyCode) {
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

            if (desktopNumber != -1) {
                try {
                    val field = NativeKeyEvent::class.java.getDeclaredField("reserved")
                    field.isAccessible = true
                    field.setShort(e, 0x01.toShort()) 
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                switchToDesktop(desktopNumber)
            }
        }
    }

    override fun nativeKeyReleased(e: NativeKeyEvent) {}
    override fun nativeKeyTyped(e: NativeKeyEvent) {}

    private fun switchToDesktop(desktop: Int) {
        if (isMac) switchMacDesktop(desktop)
        else if (isWindows) switchWindowsDesktop(desktop)
    }

    private fun switchMacDesktop(desktop: Int) {
        try {
            val robot = Robot()
            val keyCode = when(desktop) {
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
            robot.keyPress(KeyEvent.VK_CONTROL)
            robot.keyPress(keyCode)
            robot.keyRelease(keyCode)
            robot.keyRelease(KeyEvent.VK_CONTROL)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun switchWindowsDesktop(desktop: Int) {
        try {
            val userHome = System.getProperty("user.home")
            val toolDir = File(userHome, ".anne-desktop-utils")
            if (!toolDir.exists()) toolDir.mkdirs()
            val exeFile = File(toolDir, "VirtualDesktop.exe")
            if (exeFile.exists()) {
                val process = ProcessBuilder(exeFile.absolutePath, "/Switch:$desktop")
                process.start()
            } else {
                println("VirtualDesktop.exe not found.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class AppPreferencesImpl : AppPreferences {
    private val prefs = Preferences.userRoot().node("com.anne.utils")
    private val isMac = System.getProperty("os.name").lowercase().contains("mac")
    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    override var startAtStartup: Boolean
        get() = prefs.getBoolean("startAtStartup", false)
        set(value) {
            prefs.putBoolean("startAtStartup", value)
            updateStartupLogic(value)
        }

    override var showTrayIcon: Boolean
        get() = prefs.getBoolean("showTrayIcon", true)
        set(value) {
            prefs.putBoolean("showTrayIcon", value)
        }

    override var closeToTray: Boolean
        get() = prefs.getBoolean("closeToTray", true)
        set(value) {
            prefs.putBoolean("closeToTray", value)
        }

    private fun updateStartupLogic(enable: Boolean) {
        if (isMac) {
            // Autostart su macOS: LaunchAgent plist
            val userHome = System.getProperty("user.home")
            val launchAgentsDir = File(userHome, "Library/LaunchAgents")
            if (!launchAgentsDir.exists()) launchAgentsDir.mkdirs()
            val plistFile = File(launchAgentsDir, "com.anne.utils.plist")

            if (enable) {
                // Semplificato: assume che l'eseguibile sia nel PATH o in una location nota
                // In un app pacchettizzata, dovrebbe puntare a /Applications/AnneUtils.app/Contents/MacOS/...
                val plistContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                    <plist version="1.0">
                    <dict>
                        <key>Label</key>
                        <string>com.anne.utils</string>
                        <key>ProgramArguments</key>
                        <array>
                            <string>/Applications/AnneUtils.app/Contents/MacOS/AnneUtils</string>
                        </array>
                        <key>RunAtLoad</key>
                        <true/>
                    </dict>
                    </plist>
                """.trimIndent()
                plistFile.writeText(plistContent)
            } else {
                if (plistFile.exists()) plistFile.delete()
            }
        } else if (isWindows) {
            // Autostart su Windows: Shortcut nel menu esecuzione automatica
            // Dato che da Java non è banale creare uno shortcut (LNK), una via semplice
            // è usare un piccolo script VBS oppure una chiave di registro.
            try {
                if (enable) {
                    val command = "REG ADD \"HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run\" /V \"AnneUtils\" /t REG_SZ /F /D \"C:\\Program Files\\AnneUtils\\AnneUtils.exe\""
                    Runtime.getRuntime().exec(arrayOf("cmd.exe", "/c", command))
                } else {
                    val command = "REG DELETE \"HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run\" /V \"AnneUtils\" /F"
                    Runtime.getRuntime().exec(arrayOf("cmd.exe", "/c", command))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

fun main() = application {
    val manager = remember { DesktopSwitchManager() }
    val prefs = remember { AppPreferencesImpl() }
    var isWindowVisible by remember { mutableStateOf(true) }

    val icon = painterResource("anne_icon.png")

    if (prefs.showTrayIcon && icon != null) {
        Tray(
            icon = icon,
            tooltip = "Anne Utils",
            onAction = { isWindowVisible = true },
            menu = {
                Item("Open", onClick = { isWindowVisible = true })
                Item("Exit", onClick = { 
                    manager.stop()
                    exitApplication() 
                })
            }
        )
    }

    if (isWindowVisible) {
        Window(
            onCloseRequest = {
                if (prefs.closeToTray && prefs.showTrayIcon) {
                    isWindowVisible = false
                } else {
                    manager.stop()
                    exitApplication()
                }
            }, 
            title = "Anne Utils",
            icon = icon
        ) {
            App(manager, prefs)
        }
    }
}
