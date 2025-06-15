# Immunda Noctis - Motore di Gioco per GDR in Solitaria

![Licenza MIT](https://img.shields.io/badge/License-MIT-blue.svg)

**Immunda Noctis** non è un semplice gioco, ma un motore di avventure testuali per giochi di ruolo in solitaria, interamente guidato da intelligenza artificiale eseguita in locale sul tuo dispositivo Android.

## 📜 Visione del Progetto

Ricordi i vecchi **libri-gioco**, dove ogni scelta apriva un nuovo paragrafo e una nuova diramazione della storia? Immunda Noctis nasce da quello stesso spirito, ma lo porta a un livello successivo. Invece di seguire percorsi predefiniti, l'avventura viene creata, narrata e adattata in tempo reale da un'intelligenza artificiale che funge da Dungeon Master personale.

L'obiettivo è creare un'esperienza di gioco di ruolo profonda, rigiocabile e completamente offline, dove ogni partita è un racconto unico e irripetibile.

## ✨ Funzionalità Chiave

Questo progetto è costruito con un'architettura moderna e modulare per garantire flessibilità e manutenibilità.

* **Gestione delle Sessioni**: Il gioco salva i tuoi progressi! Puoi continuare la tua ultima avventura o iniziarne una nuova da zero, sovrascrivendo quella precedente.

* **Creazione dell'Eroe Personalizzato**: Prima di iniziare una nuova campagna, puoi dare forma al tuo eroe scegliendo:
    * **Nome della Campagna**: Per dare un titolo unico alla tua partita.
    * **Nome dell'Eroe**: Scegli il nome del tuo protagonista.
    * **Ritratto e Genere**: Seleziona un ritratto maschile o femminile.
    * **Classe**: Scegli tra Guerriero, Ladro, Mago o Saggio, ognuno con una breve descrizione.
    * **Allineamento**: Definisci il carattere del tuo eroe come Buono, Neutrale or Malvagio.
    * **Lingua**: Imposta la lingua del tuo eroe (per future funzionalità TTS).

* **Motore IA Locale**: L'inferenza viene eseguita al 100% sul dispositivo grazie a implementazioni di `llama.cpp` e MediaPipe (per modelli Gemma), garantendo un'esperienza offline e la massima privacy.

* **Architettura a Doppio Motore**: L'applicazione permette agli utenti di scaricare e gestire due modelli IA separati (es. Gemma per il DM, GGUF per i PG) tramite un'interfaccia dedicata.

* **Struttura Modulare**: Il progetto è diviso in due moduli principali:
    * `:app`: Contiene tutta l'interfaccia utente (scritta in Jetpack Compose) e la logica di gioco.
    * `:llama`: Una libreria Android autonoma che incapsula la complessità di `llama.cpp`, fornendo un'API Kotlin pulita.

## 🗺️ Flusso dell'Applicazione

L'app è strutturata con un flusso di navigazione chiaro:

1.  **MainActivity**: La schermata iniziale che funge da hub, da cui puoi scegliere se iniziare a giocare o configurare i motori AI.
2.  **SetupActivity**: Il cuore della preparazione dell'avventura. Qui scegli se continuare una partita esistente o crearne una nuova attraverso la personalizzazione del personaggio.
3.  **ModelActivity**: La schermata tecnica per la gestione e il download dei modelli di linguaggio.
4.  **AdventureActivity**: Il tavolo da gioco vero e proprio, dove si svolge l'avventura testuale.

## 🚀 Come Iniziare

Per compilare ed eseguire il progetto, segui questi passaggi:

1.  Assicurati di avere Android Studio e l'NDK configurati correttamente.
2.  Clona questo repository.
3.  Apri il progetto con Android Studio.
4.  Esegui una prima sincronizzazione con Gradle.
5.  Avvia l'app su un emulatore o un dispositivo fisico.

## 🙏 Un Tributo Speciale

> Questo progetto, e la passione per la programmazione e i giochi di ruolo, non sarebbero mai nati senza il meraviglioso **JD**, Joe Dever.
>
> Le sue avventure e la sua creatività sono state la scintilla che ha acceso tutto, ispirando non solo questo codice, ma un'intera generazione di sviluppatori e narratori.
>
> Un saluto a dovunque lui sia, da un suo Fan.

## ✒️ Autore e Contatti

* **Michele Lops**
* Email: [sentieroluminoso@gmail.com](mailto:sentieroluminoso@gmail.com)

## 📄 Licenza

Questo progetto è rilasciato sotto la **Licenza MIT**. Vedi il file `LICENSE` per maggiori dettagli.
