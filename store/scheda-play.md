# AA Mirror — materiale per Google Play Console (Test interno)

Tutto il necessario per creare l'app in Play Console e distribuirla nel canale
**Test interno** (nessuna revisione, nessuna pubblicazione pubblica).

## File in questa cartella

| File | Uso in Play Console |
|---|---|
| `play-icon-512.png` | Icona dell'app (512×512) — Scheda dello store principale |
| `feature-graphic-1024x500.png` | Immagine in evidenza (1024×500) |

L'AAB da caricare è generato da Gradle: `app/build/outputs/bundle/release/app-release.aab`.

## Testi della scheda (copia-incolla)

**Nome app** (max 30 caratteri):

> AA Mirror

**Descrizione breve** (max 80 caratteri):

> Mostra lo schermo del telefono sul display Android Auto. Solo uso personale.

**Descrizione completa**:

> AA Mirror duplica lo schermo del telefono sul display dell'auto tramite
> Android Auto, con controlli essenziali dal display (play/pausa, indietro,
> home), tocco e scorrimento inoltrati al telefono e blocco dell'orientamento
> orizzontale.
>
> App sperimentale per uso personale, distribuita esclusivamente in canale di
> test. Richiede la conferma esplicita della registrazione schermo a ogni
> avvio e un servizio di accessibilità (attivato manualmente dall'utente) per
> inoltrare i tocchi. Non raccoglie, memorizza né trasmette alcun dato: tutto
> avviene in locale tra il telefono e il display dell'auto.
>
> Da usare esclusivamente a veicolo fermo e nel rispetto del codice della
> strada.

## Passi in Play Console

1. [Play Console](https://play.google.com/console) → **Crea app**: nome "AA Mirror",
   lingua predefinita Italiano, tipo App, gratuita. Spunta le dichiarazioni.
2. **Test e release → Test → Test interno → Crea nuova release.**
   - Firma: accetta la firma gestita da Google Play (Play App Signing).
   - Carica `app-release.aab`, salva e avvia il rollout per il test interno.
3. Nella scheda **Tester**: crea una lista con l'email dell'account Google
   usato sui telefoni (Fold e S21) e copia il **link di attivazione** (opt-in).
4. Completa solo le sezioni che la console segnala come bloccanti per il test
   interno (di solito: Norme sulla privacy*, Accesso alle app, Contenuti).
   Risposte oneste suggerite:
   - **Accesso alle app**: tutto disponibile senza credenziali.
   - **Classificazione contenuti**: questionario "Utility", nessun contenuto sensibile.
   - **Sicurezza dei dati**: l'app non raccoglie e non condivide alcun dato.
   - **Pubblico di destinazione**: 18+.
5. Sul telefono (con l'account tester): apri il link di attivazione, accetta,
   installa da Google Play. **Prima disinstalla la versione attuale di AA
   Mirror** (firma diversa, l'installazione fallirebbe).
6. Rifai la configurazione dell'app (consenso cattura, accessibilità, overlay)
   e collega il telefono all'auto: l'app deve comparire nel launcher di
   Android Auto.

\* Se la console pretende un URL per le norme sulla privacy anche per il test
interno, pubblica il contenuto di `privacy-policy.md` (per esempio come Gist
GitHub pubblico) e incolla quell'URL.

## Note

- Gli screenshot del telefono (min. 2) servono solo se si passa a canali
  pubblici; per il test interno di solito non sono bloccanti.
- Restare nel canale **Test interno**: non promuovere la release a canali
  aperti/produzione — l'app usa la categoria navigazione dei template in modo
  non conforme alle linee guida di qualità e non supererebbe la revisione.
- Ogni nuovo caricamento richiede `versionCode` incrementato in
  `app/build.gradle.kts`.
