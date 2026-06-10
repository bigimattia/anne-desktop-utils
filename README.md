# Anne Utils

Anne Utils è un'applicazione desktop multi-piattaforma scritta in Kotlin Multiplatform (KMP) con Compose Desktop. L'app intercetta scorciatoie da tastiera a livello globale per cambiare desktop virtuale (`Win+Numero` su Windows, `Cmd+Numero` su Mac).

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

## Limitazioni e Note

- **Windows**: Per far sì che l'applicazione cambi effettivamente desktop virtuale superando le limitazioni del sistema operativo, richiede il download di un'utility chiamata `VirtualDesktop.exe`. Questa utilità va piazzata in `~/.anne-desktop-utils/VirtualDesktop.exe`.
- **macOS**: Non ci sono API pubbliche per cambiare le "Scrivanie". L'app simula per questo la pressione di `Ctrl+Numero`. Affinché l'app funzioni correttamente, assicurati di aver abilitato le relative scorciatoie in *Impostazioni di Sistema -> Tastiera -> Abbreviazioni da tastiera -> Mission Control -> Passa alla scrivania N*.
