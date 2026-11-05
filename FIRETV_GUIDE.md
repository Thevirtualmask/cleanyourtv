# Fire TV Stick Lite - Launcher & Debloat Guide
**Device:** Amazon Fire TV Stick Lite 1st Gen (Codename: `sheldonp` / `AFTSSS`)  
**OS Version:** Fire OS 7.7.1.7 (`PS7717.5741N`), Android 9 (SDK 28)  
**IP Address:** `192.168.1.9`  

---

## 1. Apple TV Style Custom Launcher (Attivo & Intercettazione Tasto Home)

Nonostante Fire OS 7 protegga `com.amazon.tv.launcher` a livello di framework impedendone la disabilitazione tradizionale, abbiamo implementato con successo la sostituzione della schermata home tramite il servizio di accessibilità nativo `HomeKeyService` (`com.gathin.tvhome/.HomeKeyService`):

1. **Launcher Installato:**
   - Launcher leggero nativo (`com.gathin.tvhome`), consumo RAM minimo (< 25 MB residenti).
   - Sfondo nero puro, dock in pillola di vetro satinato, font Outfit, orologio sottile e data.
   - Badge stato VPN integrato nel pulsante di rete in alto a destra.

2. **Dock configurato con le tue 4 app:**
   1. **Stremio** (`com.stremio.one`)
   2. **SmartTube** (`org.smarttube.beta`)
   3. **IPTV Smarters** (`com.nst.iptvsmarterstvbox`)
   4. **NordVPN** (`com.nordvpn.android`)

3. **Intercettazione Tasto Home:**
   - Configurato `stock_home_redirect` puntato su `com.amazon.tv.launcher`.
   - Il servizio `HomeKeyService` intercetta gli eventi `TYPE_WINDOW_STATE_CHANGED` del launcher Amazon: ogni volta che premi il tasto **Home** sul telecomando Fire TV o accendi la chiavetta, viene mostrata all'istante la schermata pulita Apple TV invece della dashboard pubblicitaria di Amazon!

---

## 2. Debloat di Sistema & DNS AdGuard

1. **Telemetria e Raccolta Dati Disabilitati:**
   - Bloccati tutti i servizi di tracciamento e log Amazon (`client.metrics`, `neo.minerva`, `wirelessmetrics`, `dp.logger`, `ceviche`, `tv.acr`, `wifilocker`, ecc.).
2. **Annunci e Shopping Disabilitati:**
   - Bloccati `kso.blackbird` (motore di bidding annunci), `sneakpeek`, `hybridadidservice`, `shoptv`, `gamehub`.
   - Rimossa la scheda "Live" dalla barra superiore per eliminare il sovraccarico di polling canali.
3. **App e Servizi Inutilizzati Rimossi:**
   - Amazon Music, Amazon Photos, Amazon Kids, Veezie (`st.veezie`), Disney+, Pluto TV, NOW TV, Spotify, Plex, Apple TV.
4. **DNS Privato AdGuard Attivo:**
   - Impostato `dns.adguard-dns.com` per bloccare pubblicità e tracker di rete.

---

## 3. Comandi di Gestione e Ripristino

- **Ripristino totale allo stato di fabbrica:**
  ```powershell
  .\cleanyourtv\restore_firetv.ps1 -TvHost 192.168.1.9
  ```
  *(Re-instaura tutti i pacchetti Amazon, disattiva il reindirizzamento Home e resetta il DNS standard).*

- **Aggiornare la configurazione del dock (senza ricompilare):**
  Modifica `cleanyourtv\themes\firetv\config.json` e applica:
  ```powershell
  powershell -ExecutionPolicy Bypass -File cleanyourtv\tools\theme.ps1 -TvHost 192.168.1.9 cleanyourtv\themes\firetv
  ```

- **Chiusura del Debug USB / ADB a fine sessione:**
  ```powershell
  $env:TV_HOST = "192.168.1.9"
  python cleanyourtv\tools\tv.py "settings put global adb_enabled 0"
  ```
