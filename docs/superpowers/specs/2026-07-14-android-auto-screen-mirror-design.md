# Spec — Mirroring dello schermo del telefono su Android Auto

**Data:** 2026-07-14
**Stato:** approvata a voce, in attesa di revisione scritta
**Fase:** 1 di 2 (fase 2 futura: streaming da un secondo telefono via WiFi — fuori scope, vedi "Estensioni future")

## Obiettivo

Un'app Android per uso personale che mostra in tempo reale lo schermo del telefono
sul display dell'auto tramite Android Auto, con tre pulsanti di controllo minimo
(play/pausa, indietro, home). Auto di riferimento: Nissan Qashqai J12, connessione
Android Auto via cavo USB-C.

## Vincoli e presupposti

- **Solo uso personale.** L'app non è pubblicabile sul Play Store: il mirroring non
  rientra nelle categorie ammesse da Google. Richiede sul telefono la modalità
  sviluppatore di Android Auto + "Sorgenti sconosciute".
- **Niente root.** Si usano solo API ufficiali (Car App Library + MediaProjection),
  in modo creativo: l'app si dichiara di categoria **navigazione**, l'unica che
  riceve una `Surface` a disegno libero sul display auto.
- Il telefono di cui si condivide lo schermo è **quello connesso via USB** che
  esegue Android Auto.
- L'audio non viene gestito dall'app: con Android Auto connesso, l'audio
  multimediale del telefono va già alle casse dell'auto.
- Stack: Kotlin, `androidx.car.app`, minSdk 26, un solo modulo Gradle.

## Architettura

Tre unità con responsabilità nette, tutte nella stessa app (comunicazione
in-process, binding diretto):

### 1. Lato auto — `CarAppService` + `MirrorScreen`

- Dichiara `androidx.car.app.category.NAVIGATION` nel manifest.
- `MirrorScreen` usa `NavigationTemplate` e registra un `SurfaceCallback`:
  quando la Surface del display auto è disponibile, la passa al `MirrorService`.
- Action strip con tre azioni: **⏯ play/pausa**, **◀ indietro**, **⌂ home**.
- Non sa come vengono prodotti i frame: chiede solo "disegna su questa Surface".
- Se il mirroring non è attivo, mostra un messaggio di stato sul display auto
  ("Conferma sul telefono", "Apri l'app sul telefono e riprova"), mai schermo nero.

### 2. Lato telefono — `MirrorService` (foreground service)

- Possiede la sessione `MediaProjection`.
- Riceve la Surface dal lato auto e crea un `VirtualDisplay` in modalità mirror
  (`VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR`) che copia lo schermo del telefono su quella
  Surface. Nessuna codifica video: latenza minima, consumo contenuto.
- Scaling **aspect-fit**: bande nere se le proporzioni non combaciano, mai stretching.
- Rotazione del telefono: ricrea il VirtualDisplay con le nuove dimensioni
  (~0,5 s di nero accettabile).
- Foreground service con notifica persistente durante il mirroring (tipo
  `mediaProjection`).
- **Punto di aggancio per la fase 2:** il servizio disegna sulla Surface a partire
  da una *sorgente di frame* astratta. Oggi l'unica implementazione è lo schermo
  locale via VirtualDisplay; in futuro una seconda implementazione potrà essere un
  flusso video ricevuto via WiFi, senza toccare il lato auto.

### 3. Lato telefono — Activity di setup

UI minima con:
- stato corrente (connesso / non connesso / mirroring attivo);
- richiesta del permesso di cattura schermo (dialog di sistema, obbligatorio);
- passo guidato per attivare il servizio Accessibility;
- istruzioni per abilitare modalità sviluppatore di Android Auto e
  "Sorgenti sconosciute" (passo manuale dell'utente, documentato nell'app e nel README).

### Pulsanti di controllo

- **Play/pausa:** media key event via `AudioManager.dispatchMediaKeyEvent`
  (non richiede Accessibility).
- **Indietro / Home:** `AccessibilityService.performGlobalAction`. Se il servizio
  Accessibility non è attivo, i pulsanti mostrano un toast sul display auto
  ("Attiva il servizio nelle impostazioni") e il mirroring continua a funzionare.

### Flusso dati

```
Schermo telefono → MediaProjection / VirtualDisplay → Surface del display auto
```

## Esperienza d'uso

**Setup iniziale (~2 minuti, una tantum):**
1. Aprire l'app sul telefono, seguire i due passi guidati (Accessibility,
   modalità sviluppatore AA + sorgenti sconosciute).
2. Collegare il cavo USB: l'app "Mirror" compare tra le app di Android Auto.

**Uso quotidiano:**
1. Toccare "Mirror" sul display auto.
2. Alla prima attivazione per sessione, confermare il permesso di cattura schermo
   sul telefono (obbligo di sicurezza Android, non aggirabile; va ripetuto solo se
   il servizio viene chiuso).
3. Lo schermo del telefono appare sul display auto, aspect-fit. In landscape
   riempie meglio lo schermo.
4. La barra azioni offre play/pausa, indietro, home; tutto il resto si fa
   toccando il telefono.

**Nota:** l'app non blocca l'uso in movimento; la responsabilità dell'uso è
dell'utente. Android Auto può mostrare il suo avviso standard per le app di
navigazione.

## Gestione errori e casi limite

| Caso | Comportamento |
|---|---|
| Permesso cattura negato o scaduto | Messaggio sul display auto: "Apri l'app sul telefono e riprova" |
| Servizio ucciso da Android (battery optimization) | Foreground service + notifica persistente; alla riconnessione si ripropone la richiesta permesso |
| Rotazione del telefono | VirtualDisplay ricreato con le nuove dimensioni |
| Disconnessione del cavo | Android Auto termina; il servizio rilascia MediaProjection, la notifica sparisce |
| Accessibility non attivo | Toast sul display auto; mirroring comunque funzionante |

## Test

- **Sviluppo senza auto:** Desktop Head Unit (DHU) di Google su PC, collegato al
  telefono via ADB — telefono vero, display auto emulato.
- **Unit test** sulle parti pure: calcolo aspect-fit, macchina a stati del servizio.
- **Collaudo finale sulla Qashqai** con checklist: avvio, permesso, rotazione,
  pulsanti, riconnessione dopo scollegamento cavo.

## Addendum 2026-07-14 — Touch dal display auto + blocco landscape

Approvato dopo il collaudo DHU della v0.1. Due funzionalità aggiuntive:

### Touch dal display auto (tap + scroll)

- `SurfaceCallback.onClick(x, y)` fornisce i tap sul display auto;
  `onScroll(distanceX, distanceY)` fornisce lo scorrimento (solo distanze).
- Un `TouchMapper` puro (testato) converte le coordinate auto → telefono
  invertendo l'aspect-fit; i tap sulle bande nere vengono ignorati. La
  dimensione corrente dello schermo telefono tiene conto della rotazione.
- I gesti reali sul telefono vengono eseguiti dal servizio Accessibility con
  `dispatchGesture` (tap ~50 ms; scroll = swipe ancorato al centro dello
  schermo). Richiede `android:canPerformGestures="true"` — dopo
  l'aggiornamento può essere necessario riattivare il servizio.
- Niente multi-touch/pinch né drag lunghi (limite di Android Auto; scelta
  esplicita dell'utente: tap + scroll bastano).
- I 3 pulsanti esistenti restano come scorciatoie.

### Blocco orientamento landscape

- Quarto pulsante (toggle) nella barra azioni: forza l'intero telefono in
  `SENSOR_LANDSCAPE` tramite una finestra overlay invisibile 0×0
  (tecnica "Rotation Control"); vale anche per app bloccate in verticale.
- Richiede il permesso "Mostra sopra altre app" (`SYSTEM_ALERT_WINDOW`),
  con passo dedicato nell'activity di setup; se manca, il toggle mostra un
  messaggio sul display auto.
- Feedback: CarToast "Bloccato in orizzontale" / "Rotazione normale".

## Estensioni future (fuori scope per questa spec)

- **Fase 2 — secondo telefono via WiFi:** il telefono B cattura il suo schermo,
  lo codifica (H.264), lo streamma via WiFi al telefono A connesso all'auto, che
  lo decodifica e lo proietta sulla stessa Surface tramite una nuova
  implementazione della sorgente di frame. Avrà una spec dedicata.
- Controllo touch completo dal display auto (limitato dai gesti del template
  navigazione; valutare solo se il controllo minimo si rivela insufficiente).
