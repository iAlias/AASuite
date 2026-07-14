# AA Mirror

App personale che mostra lo schermo del telefono sul display Android Auto
(categoria navigazione + MediaProjection). **Solo uso personale:** non
pubblicabile sul Play Store.

## Requisiti

- Telefono Android 8+ (consigliato 10+), app Android Auto installata
- Auto o unità con Android Auto via USB (testata: Nissan Qashqai J12)

## Installazione

1. Compila e installa: `.\gradlew.bat installDebug` (telefono via USB, debug USB attivo)
2. Sul telefono, app **Android Auto** → Impostazioni → tocca 10 volte **Versione**
   per sbloccare la modalità sviluppatore
3. Menu ⋮ → **Impostazioni sviluppatore** → attiva **Fonti sconosciute**
4. Impostazioni → **Personalizza launcher** → attiva **AA Mirror**
5. Apri AA Mirror sul telefono:
   - tocca "Apri impostazioni Accessibility" e attiva **AA Mirror**
     (serve solo per i tasti Indietro/Home dal display auto)
   - concedi il permesso notifiche se richiesto

## Uso

1. Collega il telefono all'auto con il cavo USB-C
2. Sul display auto apri **AA Mirror**
3. Alla richiesta, conferma la cattura schermo sul telefono
   (tocca la notifica "AA Mirror" se il dialog non appare da solo)
4. Barra azioni sul display auto: ⏯ play/pausa · ◀ indietro · ⌂ home

L'audio multimediale arriva alle casse dell'auto tramite Android Auto stesso.

## Test senza auto (Desktop Head Unit)

1. In Android Studio: SDK Manager → SDK Tools → installa **Android Auto Desktop Head Unit Emulator**
2. Sul telefono: Android Auto → Impostazioni sviluppatore → **Avvia server unità principale**
3. Sul PC: `adb forward tcp:5277 tcp:5277`
4. Avvia `%LOCALAPPDATA%\Android\Sdk\extras\google\auto\desktop-head-unit.exe`

## Architettura

- `core/` — stato (reducer puro), gateway in-process, calcolo aspect-fit
- `mirror/` — foreground service + MediaProjection → VirtualDisplay (`FrameSource`)
- `car/` — CarAppService categoria navigazione: Surface, action strip, frame di stato
- `input/` — Indietro/Home via Accessibility, play/pausa via media key
- `setup/` — activity di configurazione e richiesta permessi

Fase 2 prevista: sorgente remota via WiFi da un secondo telefono
(nuova implementazione di `FrameSource`, lato auto invariato).
