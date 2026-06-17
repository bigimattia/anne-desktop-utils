# Anne Utils

Anne Utils è un'applicazione desktop multi-piattaforma scritta in Kotlin Multiplatform (KMP) con Compose Desktop. L'app intercetta scorciatoie da tastiera a livello globale per cambiare desktop virtuale (`Win+Numero` su Windows, `Cmd+Numero` su Mac). Su Windows, `Win+Shift+Numero` sposta la finestra attiva sul desktop indicato. Il tasto `0` corrisponde al desktop 10.

L'interfaccia usa automaticamente l'italiano quando la lingua di sistema è impostata su italiano; per tutte le altre lingue usa l'inglese. L'opzione di avvio con il sistema apre l'applicazione solo nell'area di notifica, senza mostrare la finestra principale.

Le scorciatoie sono configurabili dalle impostazioni in due modalità:

- una combinazione di modificatori condivisa con i tasti `1-9` e `0`;
- una combinazione registrata separatamente per ciascun desktop.

## Installazione

### Windows

Scarica il file `.exe` dalla sezione **Releases** e aprilo con un doppio clic.

Per generare l'installer Windows localmente:

```powershell
.\gradlew.bat packageExe
```

Il file viene creato in:

```text
build\compose\binaries\main\exe\AnneDesktopUtils-1.0.0.exe
```

L'installer `.exe` installa l'app per l'utente corrente, crea le scorciatoie Windows e le rimuove con la disinstallazione. Se hai installato una versione precedente in `C:\Program Files\AnneDesktopUtils`, disinstallala prima di installare il nuovo `.exe`.

Per generare anche un pacchetto MSI opzionale:

```powershell
.\gradlew.bat packageMsi
```

WinGet non installa direttamente un installer locale. Supporta manifest locali, ma richiedono l'attivazione amministrativa della funzionalità. Dopo la pubblicazione del primo installer in una release GitHub, il progetto può essere aggiunto al repository pubblico WinGet per consentire:

```powershell
winget install --id bigimattia.AnneDesktopUtils -e
```

## Requisiti
- **mise** (per gestire i tool di sviluppo)

## Configurazione e Build (senza IDE)

Il progetto utilizza [mise-en-place](https://mise.jdx.dev/) per la gestione delle dipendenze di sistema. I tool (Java 21 e Gradle 8.7) sono già configurati all'interno di `mise.toml`.

Per installare i tool, compilare ed eseguire il progetto completamente da riga di comando:

1. **Assicurati di aver installato mise**.
2. **Scarica i tool necessari** all'interno della cartella di progetto:
   ```bash
   mise install
   ```

3. **Esegui l'applicazione in locale**:
   ```bash
   mise x -- ./gradlew run
   ```

4. **Pacchettizza l'applicazione** in un formato distribuibile:
   - Su macOS (`.dmg`):
     ```bash
     mise x -- ./gradlew packageDmg
     ```
   - Su Windows (`.msi`):
     ```bash
     mise x -- ./gradlew packageMsi
     ```
   - Su Windows (`.exe`):
     ```bash
     mise x -- ./gradlew packageExe
     ```
   - Tutti i formati supportati per il sistema corrente:
     ```bash
     mise x -- ./gradlew packageDistributionForCurrentOS
     ```

## Limitazioni e Note

- **Windows**: L'helper per i desktop virtuali viene compilato dal sorgente incluso in `native/windows/AnneVirtualDesktop.cs` e incorporato nell'app. Non è necessario scaricare eseguibili esterni. Windows non espone API pubbliche sufficienti per enumerare e cambiare desktop per numero o per spostare le finestre di altri processi, quindi l'helper usa le interfacce COM interne di Windows 11 24H2.
- **macOS**: Non ci sono API pubbliche per cambiare le "Scrivanie" o spostare finestre tra di esse. Entrambe le operazioni usano un piccolo helper compilato dal sorgente incluso in `native/macos/AnneVirtualDesktop.m`.
