
# Immunda Noctis - Motore di Gioco per GDR/LIBRIGAME in Solitaria

**Immunda Noctis** non è un semplice gioco, ma un motore di avventure testuali per giochi di ruolo in solitaria, interamente guidato da intelligenza artificiale eseguita in locale sul tuo dispositivo Android.

## 📜 Visione del Progetto

Ricordi i vecchi **libri-gioco**, where each choice opened a new paragraph and a new branch of the story? Immunda Noctis is born from that same spirit, but takes it to the next level. Invece di seguire percorsi predefiniti, l'avventura viene creata, narrata e adattata in tempo reale da un'intelligenza artificiale che funge da Dungeon Master personale.

L'obiettivo è creare un'esperienza di gioco di ruolo profonda, rigiocabile e completamente offline, dove ogni partita è un racconto unico e irripetibile.

> **Nota dell'Autore:** Questo è un progetto open-source, nato dalla volontà di imparare a sviluppare applicazioni su Android. È realizzato per scopi puramente istruttivi e per divertimento personale, senza alcun fine commerciale.

## ✨ Funzionalità Chiave

Questo progetto è costruito con un'architettura moderna e modulare per garantire flessibilità e manutenibilità.

* **Gestione delle Sessioni**: Il gioco salva i tuoi progressi\! Puoi continuare la tua ultima avventura o iniziarne una nuova da zero, sovrascrivendo quella precedente.

* **Creazione dell'Eroe Personalizzato**: Prima di iniziare una nuova campagna, puoi dare forma al tuo eroe.

* **Motore IA Locale**: L'inferenza viene eseguita al 100% sul dispositivo grazie a implementazioni di `llama.cpp` e MediaPipe (per modelli Gemma), garantendo un'esperienza offline e la massima privacy.

* **Architettura a Doppio Motore**: L'applicazione permette agli utenti di scaricare e gestire due modelli IA separati (es. Gemma per il DM, GGUF per i PG) tramite un'interfaccia dedicata.

* **Struttura Modulare**: Il progetto è diviso in due moduli principali: `:app` (UI e logica di gioco) e `:llama` (libreria per `llama.cpp`).

## 🚀 Come Iniziare

Per compilare ed eseguire il progetto, segui questi passaggi:

1.  Assicurati di avere Android Studio e l'NDK configurati correttamente.
2.  Clona questo repository.
3.  Apri il progetto con Android Studio.
4.  Esegui una prima sincronizzazione con Gradle.
5.  Avvia l'app su un emulatore o un dispositivo fisico.

## ⚙️ Note per la Compilazione Nativa (C++)

A causa di limitazioni del sistema operativo, la compilazione del codice nativo `llama.cpp` può presentare problemi.

**Struttura delle Cartelle:**
Per garantire che la compilazione C++ funzioni, devi clonare il repository di `llama.cpp` allo **stesso livello** del repository `ImmundaNoctis-master`. La tua struttura di directory dovrebbe essere simile a questa:

```
C:\DEV
├── ImmundaNoctis-master\  <-- Questo progetto
└── llama.cpp-master\      <-- Il repository di llama.cpp
```

Questo è fondamentale perché il file `CMakeLists.txt` del modulo `llama` utilizza un percorso relativo per trovare i file di `llama.cpp` e fallirà se la struttura non è corretta.

**Compilazione Condizionale:**
Per evitare di ricompilare il codice nativo a ogni build, puoi usare dei flag Gradle.

* **Windows:** La compilazione completa del codice C++ su un ambiente Windows nativo **non è supportata** e può fallire.
* **WSL (Windows Subsystem for Linux):** La compilazione C++ deve essere eseguita **esclusivamente all'interno di un ambiente WSL (Ubuntu)**, dove è stabile.

**Ottimizzazioni per ARM:**
La configurazione della build nativa include ottimizzazioni specifiche per le GPU Adreno (tipicamente presenti nei dispositivi con chipset Snapdragon), migliorando le performance su questi dispositivi.

## 🙏 Un Tributo Speciale

> Questo progetto, e la passione per la programmazione e i giochi di ruolo, non sarebbero mai nati senza il meraviglioso **JD**, Joe Dever.
>
> Le sue avventure e la sua creatività sono state la scintilla che ha acceso tutto, ispirando non solo questo codice, ma un'intera generazione di sviluppatori e narratori.
>
> Un saluto a dovunque lui sia, da un suo Fan.

## ❤️ Ringraziamenti alla Community e Visione Futura

Un ringraziamento speciale va ai volontari di **Project Aon**, la cui dedizione ha reso il materiale originale di Lupo Solitario, come il libro-gioco "Flight from the Dark", liberamente accessibile a tutti. Senza il loro lavoro, la conversione dei contenuti per questo motore di gioco non sarebbe stata possibile.

Il motore di **Immunda Noctis** è stato progettato per essere flessibile e adattabile a diversi sistemi di gioco. L'implementazione attuale si basa sulla famosa serie di libri-gioco di **Lupo Solitario**, ma l'architettura permette di sostituire le regole e i contenuti per supportare altre grandi avventure.

In futuro, l'obiettivo è rendere i file di contenuto (come `scenes.json`) scaricabili da una fonte esterna, permettendo agli utenti di caricare facilmente nuove avventure e interi libri con un semplice click.

## ✒️ Autore e Contatti

* **Michele Lops**
* Email: [sentieroluminoso@gmail.com](mailto:sentieroluminoso@gmail.com)

## 📄 Licenza

Questo progetto è rilasciato sotto la **Licenza MIT**. Vedi il file `LICENSE` per maggiori dettagli.

(consiglio giro di test RUN o DEBUG)