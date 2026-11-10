# Relazione Tecnica Completa: Progetto TIM Box (Technicolor Jade / uzw4010tim)

**Dispositivo:** TIM Box (Technicolor Jade, codename `uzw4010tim`, board `skipper_tim`)  
**Sistema Operativo:** Android TV 8.0.0 Oreo (API Level 26, 32-bit armeabi-v7a)  
**Hardware:** SoC Synaptics/Marvell Berlin BG4CT, 2 GB RAM, 8 GB eMMC, Tuner integrato DVB-T/T2  
**Indirizzo IP:** `192.168.1.48:5555`  
**Data Interventi:** Novembre 2026  
**Autore:** Alessio (`Thevirtualmask`)  

---

## 1. Obiettivi del Progetto

1. **Eliminare il Bloatware e la Pubblicità:** Rimuovere l'interfaccia proprietaria TIMVISION, la telemetria invasiva, i giochi, le promozioni e i banner non desiderati.
2. **Installare un Launcher Moderno e Leggero:** Integrare **CleanYourTV** con il tema fotografico *Scenes* (sfondi panoramici ad alta risoluzione a rotazione automatica ogni 60 minuti, dock satinato "glass" e dynamic glow).
3. **Mantenere il Tuner Hardware del Digitale Terrestre (DTT):** Consentire la visione fluida dei canali antenna della TV italiana senza dover ricorrere forzatamente allo streaming internet.
4. **Dominio Totale del Tasto HOME:** Garantire che la pressione del tasto Home sul telecomando fisico apra istantaneamente CleanYourTV e **mai** l'interfaccia originale TIMVISION.
5. **Bloccare le Reinstallazioni Nottturne Silenziose:** Individuare ed eliminare i demoni di sistema che riscaricavano autonomamente le app bloatware in background.
6. **Pubblicare e Condividere il Risultato:** Rendere il codice e la configurazione accessibili sia sul proprio GitHub personale sia come contributo ufficiale (Pull Request) al progetto open-source mondiale `Gathin23/cleanyourtv`.

---

## 2. Backup Completo di Fabbrica

Prima di effettuare qualsiasi modifica sul dispositivo, è stato generato un backup integrale non invasivo salvato nella directory [`cleanyourtv/backup_timbox/`](file:///c:/Users/alexi/Documents/antigraviti generico/cleanyourtv/backup_timbox/):

- `packages_all.txt`: Censimento completo dei 125 pacchetti presenti sul sistema di fabbrica.
- `settings_global.txt`, `settings_secure.txt`, `settings_system.txt`: Tabelle di configurazione Android memorizzate su disco.
- `getprop.txt`: Tutte le proprietà di sistema e parametri hardware della scheda Marvell/Technicolor.
- `home_activities.txt`: Mappatura dei gestori registrati per l'intent `android.intent.category.HOME`.
- `wm_size.txt` e `wm_density.txt`: Geometria del display (1920x1080 @ 320 dpi).

> [!IMPORTANT]
> Nessun file di sistema è stato cancellato o rimosso fisicamente dalle partizioni protette (evitando brick o perdita di stabilità). Tutti i pacchetti sono stati disabilitati a livello utente tramite `pm disable-user --user 0`, garantendo una reversibilità di fabbrica al 100%.

---

## 3. La Scoperta Cruciale sul Funzionamento della Live TV (DTT)

### Il Problema Iniziale
Quando abbiamo disabilitato il launcher operatore `timvision.launcher` per impostare CleanYourTV, la visione dei canali del Digitale Terrestre ha smesso di funzionare:
- L'applicazione di sistema AOSP *Live Channels* (`com.google.android.tv`) forzava una sintonizzazione infinita dei canali terminando costantemente con l'errore "0 canali trovati".
- Il tuner sembrava scollegato dal sistema.

### L'Architettura Nascosta di TIMVISION
Dall'analisi del bytecode decompilato, dei manifesti di sistema e dei dump delle finestre, abbiamo scoperto che:
1. **Pipeline non standard:** Il TIM Box non usa la pipeline standard Android TV Input Framework per presentare i canali dentro Google Live Channels.
2. **Player proprietario integrato:** La superficie video hardware DVB-T/T2, il deinterlacciatore e il gestore dei canali LCN risiedono direttamente dentro l'APK di `timvision.launcher`.
3. **Le componenti chiave:**
   - Decoder e tuner hardware: `com.technicolor.android.dtvinput` e `com.technicolor.android.cas.provider`.
   - Modulo di scansione frequenze: `com.technicolor.android.dtvscan.project` (*Sintonia DTT*).
   - Player a pieno schermo dei canali TV: `timvision.launcher/com.canal.player.ui.tv.dvb.BoxPlayerV2Activity`.
   - Orchestratore di sistema: `timvision.launcher/com.canal.box.ui.boot.BoxMainActivity`.

> [!NOTE]
> Se si disabilita `timvision.launcher`, il player video hardware `BoxPlayerV2Activity` viene spento e la visualizzazione dei canali antenna diventa impossibile. Per continuare a vedere la TV d'antenna, `timvision.launcher` **deve rimanere abilitato**.

---

## 4. La Soluzione Ingegneristica: Il Setup Ibrido DTT

### Il Blocco di Android 8.0 Oreo (`stopAppSwitches`)
Mantenendo `timvision.launcher` abilitato, sorgeva un ostacolo tecnico gravissimo:
- Nel manifesto di `timvision.launcher`, l'attività `BoxMainActivity` è registrata con `category.HOME` a `priority=2`.
- Su Android Oreo, `PackageManagerService.chooseBestActivity()` assegna priorità assoluta alle priv-app di sistema con priorità > 0, ignorando il comando standard `cmd package set-home-activity`.
- Di conseguenza, premendo il tasto fisico **HOME** sul telecomando, il decoder mostrava la home page commerciale TIMVISION.
- Inoltre, `PhoneWindowManager.launchHomeFromHotKey` scatena il blocco di sistema `stopAppSwitches()`, impedendo alle altre app in background di effettuare `startActivity` o `moveTaskToFront` per ben **5000 millisecondi** (5 secondi di lockout totale).

### L'Innovazione in `HomeKeyService`
Abbiamo modificato il codice Java del launcher CleanYourTV (`launcher/app/src/main/java/com/gathin/tvhome/HomeKeyService.java`) implementando un'architettura di intercettazione multi-livello:

```mermaid
flowchart TD
    A["Pressione Tasto HOME / Uscita TV"] --> B["HomeKeyService (Intercettazione Hardware)"]
    B --> C["Rilevato Foreground: timvision.launcher?"]
    C -- "No / Altra App" --> D["Pass-through Normale"]
    C -- "Sì (Non Voluto)" --> E["Fail-Safe: Iniezione Istantanea Overlay Nero (<8ms)"]
    E --> F["Zero Glitch Visivo: TIMVISION Completamente Oscurato"]
    F --> G["ActivityManager.moveTaskToFront (Retry a 150, 350, 750, 1500, 3000, 5100ms)"]
    G --> H["CleanYourTV Ritorna in Primo Piano"]
    H --> I["Rimozione Automatica dell'Overlay"]
    
    J["Click su 'Digitale Terrestre' dal Dock"] --> K["AppAdapter: Registrazione timestamp lastLaunchTime"]
    K --> L["HomeKeyService Riconosce Avvio Intenzionale"]
    L --> M["Apertura Fluida Canale TV Antenna a Schermo Intero"]
```

1. **Intercettazione a Basso Livello:** `HomeKeyService` usa `FLAG_REQUEST_FILTER_KEY_EVENTS` per filtrare gli eventi fisici del telecomando prima che il sistema li distribuisca.
2. **Fail-Safe con Finestra Overlay (<8ms):** Se il launcher operatore tenta di salire in primo piano, il servizio inietta all'istante una vista oscurante (`TYPE_APPLICATION_OVERLAY`), nascondendo l'interfaccia TIMVISION prima che il processore grafico disegni il primo fotogramma.
3. **Superamento del Lockout con Retry Schedulati:** Vengono lanciati tentativi a cascata di `moveTaskToFront` e `startActivity` a 150ms, 350ms, 750ms, 1500ms, 3000ms e 5100ms, riprendendo il controllo non appena il timer di sicurezza di Oreo scade.
4. **Tracciamento Avvii Intenzionali (`lastLaunchTime`):** Quando l'utente clicca volontariamente l'icona "Digitale Terrestre" dal dock, `AppAdapter` memorizza un timestamp. Il servizio riconosce che l'avvio è intenzionale e non interferisce, lasciando che la TV parta a schermo intero.
5. **Persistenza del Servizio (`restore_key_services: true`):** Tramite `ServiceRepair.restore(this)` e i permessi `WRITE_SECURE_SETTINGS`, CleanYourTV controlla e riattiva autonomamente il servizio ad ogni avvio.

---

## 5. La Scoperta del Demone di Reinstallazione Silenziosa

### L'Anomalia Notturna
A distanza di poche ore dalla pulizia, alle 04:37 del mattino, sono riapparse improvvisamente nella griglia le icone di *Disney+*, *Mediaset Infinity*, *Paramount+*, *Hayu*, *The Film Club*, *RDS Social TV*, *Vivo Azzurro TV* e *Apple TV*.

### L'Indagine Forense
Interrogando il Package Manager con `pm list packages -i` e `dumpsys package`, abbiamo identificato l'installatore:
```text
installerPackageName=com.technicolor.app.ota
codePath=/system/priv-app/ApplicationOta
```

- **Il Vero Ruolo di `ApplicationOta`:** Questo pacchetto non è un aggiornamento firmware di sistema, bensì l'**app store fantasma di Technicolor/TIM**. Eseguito con permessi di sistema (`uid 1000`), si sveglia alle 4:30 del mattino o alla connessione di rete, contatta i server TIM e riscarica forzatamente qualsiasi app partner mancante.
- **Il Ruolo di Google:** L'app *Apple TV* veniva spinta da `com.google.android.partnersetup` tramite Google Play Store.

### La Neutralizzazione Definitiva
Abbiamo disattivato i tre motori di reinstallazione forzata:
1. `com.technicolor.app.ota` (**ApplicationOta**) -> Disabilitato.
2. `com.google.android.partnersetup` (**Google Partner Setup**) -> Disabilitato.
3. `timvision.setup_wizard` (**Wizard di Primo Avvio**) -> Disabilitato.

> [!TIP]
> I veri aggiornamenti firmware del box rimangono operativi tramite `com.technicolor.ota` e `com.technicolor.checkota`. Il sistema non può più riscaricare bloatware in background.

---

## 6. Configurazione Finale e Applicazioni Installate

### Dock Superiore (5 Icone):
1. **NuvioTV** (`com.nuvio.tv`): Piattaforma multimediale avanzata.
2. **SmartTube** (`org.smarttube.stable`): Client YouTube per Android TV privo di annunci, con SponsorBlock e risoluzione 4K/HDR.
3. **OpenTV** (`app.opentv`): Client IPTV veloce per la fruizione streaming di tutti i canali TV italiani in alta definizione.
4. **Digitale Terrestre** (`timvision.launcher`): Avvio diretto della visualizzazione a pieno schermo del sintonizzatore d'antenna DVB-T/T2.
5. **NordVPN** (`com.nordvpn.android`): Client VPN per sicurezza e sblocco geografico.

### Griglia Inferiore (4 Icone Selezionate):
1. **Better xCloud** (`com.redphx.betterxc` v0.24.3): Applicazione dedicata al Cloud Gaming Xbox, con supporto per controller Bluetooth, vibrazione e script Better xCloud integrato.
2. **Impostazioni** (`com.android.tv.settings`): Accesso rapido alle impostazioni di sistema e connessioni.
3. **Google Play Store** (`com.android.vending`): Store ufficiale per aggiornamenti e nuove app.
4. **Sintonia DTT** (`com.technicolor.android.dtvscan.project`): Strumento nativo Technicolor per rieseguire la sintonizzazione automatica delle frequenze d'antenna.

### Tema Attivo: Scenes
- 14 sfondi panoramici ad alta risoluzione a rotazione automatica (ogni 60 minuti).
- Barra inferiore in vetro satinato (blur glass).
- Dynamic App Glow che campiona il colore dominante dell'icona attiva.
- Barra superiore con pillole informative di stato: Ora, Data, IP Wi-Fi/Ethernet, Menu Ingressi HDMI e Impostazioni.

---

## 7. Cosa Abbiamo Pubblicato e Come su GitHub

Il lavoro svolto è stato strutturato e pubblicato su due livelli complementari:

### Livello A: Repository Personale (`Thevirtualmask/cleanyourtv`)
Sul tuo profilo GitHub personale è ospitata l'edizione completa personalizzata per TIM Box:
- **Branch:** `main` e `feat/timbox-hybrid-dtt`.
- **Contenuti Inclusi:**
  - Guida completa in lingua italiana ([`TIMBOX_GUIDE.md`](file:///c:/Users/alexi/Documents/antigraviti generico/cleanyourtv/TIMBOX_GUIDE.md)).
  - Script di automazione per il ripristino istantaneo di fabbrica ([`restore_timbox.ps1`](file:///c:/Users/alexi/Documents/antigraviti generico/cleanyourtv/restore_timbox.ps1) per Windows e [`restore_timbox.sh`](file:///c:/Users/alexi/Documents/antigraviti generico/cleanyourtv/restore_timbox.sh) per Linux/macOS).
  - Il codice Java del launcher aggiornato con la gestione dell'overlay istantaneo e dei retry di Oreo.
  - File binario compilato e firmato: [`app-release.apk`](file:///c:/Users/alexi/Documents/antigraviti generico/cleanyourtv/launcher/app/build/outputs/apk/release/app-release.apk) (475 KB), pronto da caricare nella sezione **Releases** per il download con 1 clic.

### Livello B: Contribuzione al Progetto Globale (`Gathin23/cleanyourtv`)
Per rendere disponibile il preset a tutti gli utenti nel mondo che possiedono un TIM Box Technicolor:
- **Branch Dedicato:** `pr/timbox-uzw4010tim`, creato direttamente dall'ultimo commit di `upstream/main` per garantire **zero conflitti** e conformità al maintainer bot.
- **File Inclusi nel Preset (`examples/tim-timvision-box-uzw4010tim/`):**
  - `README.md`: Documentazione tecnica in lingua inglese che spiega sia l'opzione 1 (Debloat Completo con IPTV) sia l'opzione 2 (Setup Ibrido DTT).
  - `config.json`: File di configurazione a 5 colonne, dock preimpostato, flag `restore_key_services: true` e tema Scenes.
  - `debloat.txt`: Elenco categorizzato e commentato dei 41 pacchetti sicuri da disabilitare (inclusi `ApplicationOta` e `GooglePartnerSetup`).
  - `tv.json`: Metadati hardware per la rigenerazione automatica della tabella comparativa.
- **Automazione:** Esecuzione di `python tools/gen_tables.py` per sincronizzare gli indici globali del progetto.
- **Pull Request Pronta:** Inviato il branch sul tuo GitHub, pronto per la sottomissione con titolo e descrizione formattati secondo il template ufficiale.

### Livello C: Modifiche al Prompt Ufficiale ([`PROMPT.md`](file:///c:/Users/alexi/Documents/antigraviti generico/cleanyourtv/PROMPT.md))
Abbiamo aggiornato le istruzioni di sistema di CleanYourTV affinché qualsiasi assistente AI, rilevando un decoder operatore con sintonizzatore d'antenna (TIM Box, Vodafone TV, WindTre Hub, Freebox, ecc.), formuli preventivamente la domanda all'utente:
> *"Vuoi eseguire un Debloat Completo (spegnendo il launcher operatore a 0ms e usando lo streaming IPTV) oppure un Setup Ibrido DTT (mantenendo l'antenna terrestre con intercettazione del tasto Home via overlay)?"*

---

## 8. Tabella Riassuntiva dei Comandi di Gestione

| Azione Desiderata | Comando da Eseguire |
| :--- | :--- |
| **Passare a Modalità 1 (Debloat Completo)** | `adb shell "pm disable-user --user 0 timvision.launcher"` |
| **Passare a Modalità 2 (Ibrido DTT Antenna)** | `adb shell "pm enable timvision.launcher"` |
| **Ripristino Totale di Fabbrica** | `.\cleanyourtv\restore_timbox.ps1 -TvHost 192.168.1.48` |
| **Verifica Servizio Tasto Home** | `adb shell "dumpsys accessibility | grep HomeKeyService"` |
| **Disattivare Debug ADB (Sicurezza finale)** | *Impostazioni → Opzioni sviluppatore → Debug USB: OFF* |

---

*Relazione compilata e verificata con successo sul target hardware in data 10 Novembre 2026.*
