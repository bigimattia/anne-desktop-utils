package com.anne.utils.localization

data class AppStrings(
    val desktopSwitcher: String,
    val settings: String,
    val virtualDesktopHotkeys: String,
    val hotkeyDescription: String,
    val status: String,
    val active: String,
    val inactive: String,
    val startAtStartupInTray: String,
    val showTrayIcon: String,
    val closeToTray: String,
    val permissionRequired: String,
    val permissionMessage: String,
    val open: String,
    val exit: String
)

object AppLocalizations {
    private val italian = AppStrings(
        desktopSwitcher = "Desktop virtuali",
        settings = "Impostazioni",
        virtualDesktopHotkeys = "Scorciatoie desktop virtuali",
        hotkeyDescription = "Win/Cmd + 0-9 cambia desktop. Su Windows, aggiungi Maiusc per spostare la finestra attiva.",
        status = "Stato",
        active = "Attivo",
        inactive = "Non attivo",
        startAtStartupInTray = "Avvia con il sistema solo nell'area di notifica",
        showTrayIcon = "Mostra l'icona nell'area di notifica",
        closeToTray = "Alla chiusura, riduci nell'area di notifica",
        permissionRequired = "Autorizzazione necessaria",
        permissionMessage = "Impossibile abilitare l'accesso ai dispositivi di assistenza. Apri Impostazioni di Sistema -> Privacy e sicurezza -> Accessibilità e autorizza questa applicazione o il terminale.",
        open = "Apri",
        exit = "Esci"
    )

    private val english = AppStrings(
        desktopSwitcher = "Desktop Switcher",
        settings = "Settings",
        virtualDesktopHotkeys = "Virtual Desktop Hotkeys",
        hotkeyDescription = "Win/Cmd + 0-9 switches desktops. On Windows, add Shift to move the active window.",
        status = "Status",
        active = "Active",
        inactive = "Inactive",
        startAtStartupInTray = "Start with the system in the tray only",
        showTrayIcon = "Show application tray icon",
        closeToTray = "Close application to the tray",
        permissionRequired = "Permission Required",
        permissionMessage = "Failed to enable access for assistive devices. Open System Settings -> Privacy & Security -> Accessibility and grant permission to this application or your terminal.",
        open = "Open",
        exit = "Exit"
    )

    fun forLanguage(language: String): AppStrings =
        if (language.equals("it", ignoreCase = true)) italian else english
}
