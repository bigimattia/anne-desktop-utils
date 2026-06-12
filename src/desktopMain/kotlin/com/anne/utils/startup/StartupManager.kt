package com.anne.utils.startup

import java.io.File

const val START_IN_TRAY_ARGUMENT = "--tray"

class StartupManager {
    private val isMac = System.getProperty("os.name").lowercase().contains("mac")
    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    fun update(enabled: Boolean) {
        when {
            isMac -> updateMacStartup(enabled)
            isWindows -> updateWindowsStartup(enabled)
        }
    }

    private fun updateMacStartup(enabled: Boolean) {
        val launchAgentsDir = File(System.getProperty("user.home"), "Library/LaunchAgents")
        val plistFile = File(launchAgentsDir, "com.anne.utils.plist")

        if (!enabled) {
            if (plistFile.exists()) plistFile.delete()
            return
        }

        if (!launchAgentsDir.exists()) launchAgentsDir.mkdirs()
        plistFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>com.anne.utils</string>
                <key>ProgramArguments</key>
                <array>
                    <string>/Applications/AnneUtils.app/Contents/MacOS/AnneUtils</string>
                    <string>$START_IN_TRAY_ARGUMENT</string>
                </array>
                <key>RunAtLoad</key>
                <true/>
            </dict>
            </plist>
            """.trimIndent()
        )
    }

    private fun updateWindowsStartup(enabled: Boolean) {
        try {
            val runKey = "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run"
            val arguments = if (enabled) {
                listOf(
                    "reg.exe",
                    "ADD",
                    runKey,
                    "/V",
                    "AnneUtils",
                    "/t",
                    "REG_SZ",
                    "/F",
                    "/D",
                    "\"${windowsApplicationPath()}\" $START_IN_TRAY_ARGUMENT"
                )
            } else {
                listOf(
                    "reg.exe",
                    "DELETE",
                    runKey,
                    "/V",
                    "AnneUtils",
                    "/F"
                )
            }
            ProcessBuilder(arguments).start()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun windowsApplicationPath(): String {
        val processCommand = ProcessHandle.current().info().command().orElse("")
        val processName = File(processCommand).name
        if (
            processCommand.endsWith(".exe", ignoreCase = true) &&
            !processName.equals("java.exe", ignoreCase = true) &&
            !processName.equals("javaw.exe", ignoreCase = true)
        ) {
            return processCommand
        }

        val programFiles = System.getenv("ProgramFiles") ?: "C:\\Program Files"
        return File(programFiles, "AnneDesktopUtils/AnneDesktopUtils.exe").absolutePath
    }
}
