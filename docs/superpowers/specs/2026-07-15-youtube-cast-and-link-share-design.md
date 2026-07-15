# AA Suite — YouTube Cast e condivisione link dal secondo telefono

Data: 2026-07-15 · Stato: approvata

## Obiettivo

Aggiungere ad AA Suite una terza modalità, **YouTube Cast**: il telefono
collegato all'auto si comporta come una smart TV YouTube, comandata dal
telefono di un passeggero col normale tasto "trasmetti". In più, dal secondo
telefono si può **condividere qualsiasi link** che si apre nel browser sul
display dell'auto.

Fuori scope (impossibile per licenza): fare da ricevitore cast per
Netflix/Disney+/Prime — quelle app accettano solo ricevitori Chromecast
certificati con DRM hardware. Deciso con l'utente il 2026-07-15.

## Menu dell'auto

1. Mirroring schermo (esistente)
2. Browser web (esistente)
3. **YouTube Cast** (nuovo)
+ righe Blocco rotazione e Riempi schermo (esistenti).

## Componente 1: WebDisplay generalizzato

`BrowserDisplay` (object) diventa la classe **`WebDisplay(homeUrl, userAgent?)`**:
stessa tecnica attuale (VirtualDisplay privato + Presentation + WebView,
tocchi iniettati come MotionEvent, scroll via `scrollBy`). Due istanze
singleton:

- `BrowserDisplay = WebDisplay(google.com, UA di default)` — comportamento
  identico a oggi (preferiti inclusi);
- `YouTubeDisplay = WebDisplay(youtube.com/tv, UA smart TV)` — lo user-agent
  TV è una costante documentata e facile da aggiornare.

Ogni istanza conserva il proprio `currentUrl` e i propri cookie di processo
(la WebView condivide il cookie store: accettabile, i due usi non
confliggono). Solo un'istanza alla volta possiede la Surface, tramite il
`SurfaceRouter` esistente e un `WebSink` per istanza.

## Componente 2: YouTubeScreen (lato auto)

`NavigationTemplate` con barra: **menu** (torna alla lista) e **ricarica**.
Navigazione dentro l'interfaccia TV con il touch già esistente.

Abbinamento (una tantum): sull'interfaccia TV → Impostazioni → "Collega con
codice TV"; sul secondo telefono → app YouTube → Impostazioni → "Guarda
sulla TV" → inserisci codice. Funziona via internet (nessuna rete comune
richiesta); persiste nei cookie WebView tra i riavvii.

Audio: normale audio multimediale del processo, instradato alle casse
dell'auto da Android Auto.

## Componente 3: ShareServer (telefono auto)

Mini server HTTP su socket puri, porta **8977**, attivo mentre la sessione
car è viva (avviato/fermato da `MirrorSession`). Unica azione:
`POST /open` con il testo condiviso nel body →

1. estrae il primo URL dal testo (`SharedTextParser`, logica pura);
2. `BrowserDisplay.loadUrl(UrlResolver.resolve(url))`;
3. porta la sessione sul browser: pop alla radice + push `BrowserScreen`
   (tramite un `ShareInbox` SharedFlow osservato da `MirrorSession`).

Risposte: `200` ok, `400` nessun URL, altrimenti `404`. Sicurezza: solo
richieste dalla rete locale, nessun'altra azione esposta; uso personale.

## Componente 4: AA Share (modulo `companion`, secondo telefono)

Micro-APK separato (`com.viami.aashare`), **installato direttamente**
(niente Play: sul secondo telefono non valgono le restrizioni di Android
Auto). Si registra come destinazione **Condividi** (`ACTION_SEND`,
`text/plain`): activity trasparente che legge il testo, individua il gateway
dell'hotspot (= telefono auto, via `WifiManager.dhcpInfo`), fa la POST con
timeout 3 s, mostra toast esito ("Inviato all'auto ✓" / "Collegati
all'hotspot del telefono dell'auto") e si chiude.

Prerequisito di rete dichiarato: il telefono dell'auto fa da hotspot e il
secondo telefono vi è agganciato.

## Errori e casi limite

- Testo condiviso senza URL → 400 + toast d'errore sul secondo telefono.
- Hotspot assente/gateway non raggiungibile → toast dedicato, nessun crash.
- Porta 8977 occupata → log e server non attivo (nessun crash della sessione).
- Interfaccia TV rifiutata da Google → aggiornare la costante user-agent;
  pulsante ricarica per riprovare.

## Test

- **Unit (TDD)**: `SharedTextParser` (estrazione URL da testo libero),
  parser della richiesta HTTP (request line + body), routing verso
  `ShareInbox`.
- **Manuale**: DHU per YouTube TV (interfaccia, codice, touch); telefoni
  reali per hotspot + condivisione end-to-end; regressione modalità
  esistenti (mirroring, browser, preferiti).

## Rilascio

- AA Suite **v0.5** (versionCode 5) sul canale di test interno Play, solito
  flusso.
- AA Share: APK firmato con la stessa chiave, installazione diretta sul
  secondo telefono.
