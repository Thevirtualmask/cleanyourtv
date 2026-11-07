# TIM Box (Technicolor Jade / uzw4010tim) - Guida Completa Setup & Debloat

**Dispositivo:** TIM Box (Technicolor Jade, codename `uzw4010tim`)  
**Versione Android:** Android TV 8.0.0 Oreo (API level 26)  
**Indirizzo IP:** `192.168.1.48`  
**Data intervento:** Novembre 2026  
**Preset Esempio:** [`cleanyourtv/examples/technicolor-timbox-uzw4010tim/`](file:///c:/Users/alexi/Documents/antigraviti generico/cleanyourtv/examples/technicolor-timbox-uzw4010tim/)

---

## 1. Panoramica delle Due Modalità Operative

Sul TIM Box sono disponibili due modalità di funzionamento distinte in base alle esigenze dell'utente:

| Funzionalità | Modalità 1: Debloat Completo | Modalità 2: Setup Ibrido DTT (Attivo) |
| :--- | :--- | :--- |
| **Launcher Principale** | CleanYourTV (100% esclusivo) | CleanYourTV (con intercettazione tasto Home) |
| **Launcher TIMVISION** | Disabilitato (`pm disable-user`) | Abilitato (solo backend tuner / TV live) |
| **Digitale Terrestre (Antenna)**| Solo streaming IPTV (OpenTV) | **Canali DTT Antenna via Tuner Hardware** |
| **Comportamento Tasto Home** | Gestito nativamente da Android (0ms) | Gestito da `HomeKeyService` (overlay istantaneo <8ms) |
| **Uso consigliato** | Per chi guarda solo streaming web | Per chi vuole antenna TV classica + launcher pulito |

---

## 2. Stato dell'Intervento sul Dispositivo

### A. Backup completo di fabbrica
Tutti i dati e lo stato iniziale del TIM Box sono salvati nella cartella locale:  
[`cleanyourtv/backup_timbox/`](file:///c:/Users/alexi/Documents/antigraviti generico/cleanyourtv/backup_timbox/)
- `packages_all.txt`: Elenco completo dei 125 pacchetti originari
- `packages_disabled.txt`: Stato disabilitazioni iniziale
- `getprop.txt`: Proprietà di sistema complete
- `settings_global.txt`, `settings_secure.txt`, `settings_system.txt`: Tabelle impostazioni
- `home_activities.txt`: Launcher originali registrati
- `wm_size.txt` & `wm_density.txt`: Dimensioni fisiche (1920x1080 @ 320 dpi)

---

### B. Debloat di Sistema (41 pacchetti disabilitati a livello utente)
Nessun pacchetto è stato eliminato o disinstallato; tutti sono stati disabilitati in sicurezza tramite `pm disable-user --user 0` (reversibile al 100%):

1. **Telemetria, Tracker e Servizi Operatore TIM:**
   - `timvision.telemetry`: Telemetria TIMVISION
   - `it.telecomitalia.digitalsupport`: Agente diagnostico TIM
   - `it.telecomitalia.linemanager`: Line manager operatore
   - `com.movenda.omadm.tv` & `com.movenda.p4mcore`: Gestione remota OMA-DM
   - `tv.broadpeak.nanocdnwrapper`: Demone multicast nanoCDN Broadpeak
   - `com.technicolor.zta`: Provisioning Zero-Touch Technicolor
   - `com.technicolor.android.BootupTraceQR`: Logger di avvio QR
   - `com.technicolor.autoperiodicreboot`: Demone di riavvio periodico forzato
   - `android.autoinstalls.config.technicolor`: Autoinstaller silenzioso di app sponsorizzate
   - `com.technicolor.app.ota`: **ApplicationOta** (demone Technicolor di download/reinstallazione silenziosa in background dei pacchetti partner operatore; disabilitato per impedire che scarichi nuovamente le app commerciali)
   - `timvision.setup_wizard`: Procedura di primo avvio TIMVISION (non necessaria)

2. **Bloatware TIM, Giochi e Annunci:**
   - `it.telecomitalia.thefilmclub`: The Film Club
   - `it.telecomitalia.games`: TIMGAMES
   - `it.telecomitalia.timmusic_tv`: TIMMUSIC
   - `it.telecomitalia.timppapp`: Portale promo TIM
   - `com.technicolor.tim.customization`: Overlay personalizzazione TIM
   - `com.technicolor.project.releasenote`: Note di rilascio
   - `com.figc.vivoazzurrotv`: Vivo Azzurro TV
   - `it.rds.androidtv.rdssocialtv`: RDS Social TV
   - `com.cbs.ca`: CBS / Paramount

3. **App di Streaming commerciali rimosse:**
   - `com.netflix.ninja`: Netflix
   - `com.amazon.amazonvideo.livingroom`: Amazon Prime Video
   - `com.disney.disneyplus`: Disney+
   - `com.dazn`: DAZN
   - `com.wbd.stream`: Discovery+ / Max
   - `com.apple.atve.androidtv.appletv`: Apple TV
   - `it.mediaset.infinitytv`: Mediaset Infinity
   - `tv.pluto.android`: Pluto TV
   - `com.upst.hayu`: Hayu

4. **Bloatware Google non necessario rimosso:**
   - `com.google.android.youtube.tv`: YouTube ufficiale con annunci (sostituito da SmartTube)
   - `com.google.android.youtube.tvkids`: YouTube Kids
   - `com.google.android.youtube.tvmusic`: YouTube Music
   - `com.google.android.play.games`: Google Play Games
   - `com.google.android.videos`: Google Play Film & TV
   - `com.google.android.tvtutorials`: Tutorial Google TV
   - `com.google.android.tvrecommendations` & `com.google.android.leanbacklauncher.recommendations`: Righe pubblicitarie di raccomandazioni
   - `com.google.android.feedback`: Invio crash e segnalazioni
   - `com.google.android.partnersetup`: **GooglePartnerSetup** (servizio Google che forza l'installazione delle app partner da Play Store; disabilitato per bloccare reinstallazioni tipo Apple TV)

5. **Servizi essenziali conservati e intatti al 100%:**
   - **Tuner Digitale Terrestre (DVB-T) & Scansione canali**: `com.technicolor.android.dtvinput`, `com.technicolor.android.dtvscan.project`, `com.technicolor.android.cas.provider`
   - **Telecomandi e Bluetooth**: `com.technicolor.android.remotecontrolservice`, `com.technicolor.tv.remotepairing`, `com.technicolor.globalkey`
   - **Google Assistant (Microfono vocale)**: `com.google.android.katniss`, `com.google.android.speech.pumpkin`
   - **Chromecast integrato (Google Cast)**: `com.google.android.apps.mediashell`
   - **Google Play Store & Play Services**: `com.android.vending`, `com.google.android.gms`
   - **Tastiera a schermo (Gboard / LatinIME)**: `com.google.android.inputmethod.latin`
   - **Aggiornamenti di sistema / OTA Firmware**: `timvision.upgrade`, `com.technicolor.ota`, `com.technicolor.checkota`

---

## 3. Tema Scenes & Configurazione Launcher

### Configurazione Estetica (Tema Scenes):
- **Sfondi rotanti dinamici**: 14 sfondi panoramici ad alta risoluzione che ruotano ogni 60 minuti (`wallpaper_rotation_minutes: 60`).
- **Dock Satinato "Glass"**: Barra inferiore traslucida a 5 colonne con effetto blur.
- **Dynamic App Glow**: Alone luminoso animato attorno all'icona selezionata che riprende i colori primari del banner.
- **Top Bar Funzionale**: Indicatori di stato a pillola (Rete Wi-Fi/Ethernet con IP, Ingressi HDMI e scorciatoia rapida Impostazioni).

### Dock Principale (5 Icone):
1. **Nuvio** (`com.nuvio.tv`)
2. **SmartTube** (`org.smarttube.stable` - YouTube senza pubblicità e con SponsorBlock)
3. **OpenTV** (`app.opentv` - Client IPTV veloce e pulito)
4. **Digitale Terrestre** (`timvision.launcher` - Apre istantaneamente la visualizzazione a schermo intero dei canali TV sintonizzati dall'antenna)
5. **NordVPN** (`com.nordvpn.android` - Installabile con 1 clic dal Play Store)

### Griglia Applicazioni Secondaria (sotto al Dock):
- **Sintonia DTT** (`com.technicolor.android.dtvscan.project`): Posizionata ordinatamente nella griglia inferiore delle applicazioni, consente di rieseguire la scansione automatica dei canali ogni volta che l'impianto d'antenna subisce variazioni.

---

## 4. Architettura Tecnica del Tasto Home (Modalità Ibrida)

Su Android TV 8.0 Oreo, il launcher operatore `timvision.launcher` possiede una priorità hardcoded `priority=2` nel manifesto di sistema. Per impedire che la pressione del tasto Home mostri la schermata principale TIMVISION:

1. **`HomeKeyService` (Accessibilità con Intercettazione Hardware)**:
   - Registra `FLAG_REQUEST_FILTER_KEY_EVENTS` per intercettare i codici tasto del telecomando (`KEYCODE_HOME`, `KEYCODE_TV`, `KEYCODE_PROG_RED`, ecc.).
2. **Fail-Safe con Overlay Istantaneo (<8ms)**:
   - Se `timvision.launcher.BoxMainActivity` tenta di passare in primo piano a seguito di un evento Home hardware, `HomeKeyService` inietta all'istante una vista a schermo intero oscurante (`TYPE_APPLICATION_OVERLAY`) azzerando qualsiasi sfarfallio visivo.
3. **Ripristino di CleanYourTV in Primo Piano**:
   - `HomeKeyService` richiama `ActivityManager.moveTaskToFront` per riportare `com.gathin.tvhome` in cima allo stack delle attività e rimuove l'overlay oscurante.
4. **Lancio Intenzionale dei Canali TV**:
   - Quando l'utente clicca volontariamente l'icona "Digitale Terrestre" dal dock, `AppAdapter` registra un timestamp di lancio (`setLastLaunchTime`). `HomeKeyService` riconosce la richiesta intenzionale e consente a TIMVISION di riprodurre il video a pieno schermo senza interferenze.
5. **Persistenza del Servizio (`restore_key_services: true`)**:
   - CleanYourTV auto-ripristina automaticamente i permessi di accessibilità ad ogni riavvio o riavvio del processo tramite `ServiceRepair.restore(this)` e `WRITE_SECURE_SETTINGS`.

---

## 5. Come Passare da una Modalità all'Altra

Il passaggio tra **Modalità 1 (Debloat Completo)** e **Modalità 2 (Setup Ibrido DTT)** è istantaneo:

### Per passare a Modalità 1 (Debloat Completo):
Se decidi di non usare l'antenna TV tradizionale e vuoi disattivare del tutto TIMVISION:
```powershell
python cleanyourtv/tools/tv.py pm disable-user --user 0 timvision.launcher
```

### Per tornare a Modalità 2 (Setup Ibrido DTT):
Se vuoi riavere il Digitale Terrestre con la TV dal vivo:
```powershell
python cleanyourtv/tools/tv.py pm enable timvision.launcher
```

---

## 6. Ripristino Completo di Fabbrica

In qualsiasi momento è possibile ripristinare il box allo stato originale al 100%:

```powershell
.\cleanyourtv\restore_timbox.ps1 -TvHost 192.168.1.48
```
oppure su Linux / macOS:
```bash
./cleanyourtv/restore_timbox.sh 192.168.1.48
```

---

## 7. Chiusura del Debug ADB (Opzionale per Sicurezza)

Al termine della configurazione puoi andare su:  
**Impostazioni → Opzioni sviluppatore → disattiva "Debug USB"**.  
In questo modo la porta di rete 5555 viene chiusa e nessun dispositivo sulla rete locale potrà inviare comandi non autorizzati. Se in futuro vorrai effettuare nuove modifiche, basterà riattivare la levetta dalle impostazioni della TV.
