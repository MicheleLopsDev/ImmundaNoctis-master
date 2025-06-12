# Immunda Noctis - Motore di Gioco per GDR in Solitaria

![Licenza MIT](https://img.shields.io/badge/License-MIT-blue.svg)

**Immunda Noctis** non è un semplice gioco, ma un motore di avventure testuali per giochi di ruolo in solitaria, interamente guidato da intelligenza artificiale eseguita in locale sul tuo dispositivo Android.

## 📜 Visione del Progetto

Ricordi i vecchi **libri-gioco**, dove ogni scelta apriva un nuovo paragrafo e una nuova diramazione della storia? Immunda Noctis nasce da quello stesso spirito, ma lo porta a un livello successivo. Invece di seguire percorsi predefiniti, l'avventura viene creata, narrata e adattata in tempo reale da un'intelligenza artificiale che funge da Dungeon Master personale.

L'obiettivo è creare un'esperienza di gioco di ruolo profonda, rigiocabile e completamente offline, dove ogni partita è un racconto unico e irripetibile.

## ✨ Architettura e Funzionalità Chiave

Questo progetto è costruito con un'architettura moderna e modulare per garantire flessibilità e manutenibilità.

* **Architettura a Moduli**: Il progetto è diviso in due moduli principali:
    * `:app`: Contiene tutta l'interfaccia utente (scritta in Jetpack Compose) e la logica di gioco.
    * `:llama`: Una libreria Android autonoma che incapsula la complessità di `llama.cpp`, fornendo un'API Kotlin pulita per interagire con i modelli di linguaggio.

* **Motore IA Locale**: L'inferenza viene eseguita al 100% sul dispositivo grazie a `llama.cpp` e in futuro a MediaPipe, garantendo un'esperienza offline e la massima privacy.

* **Gestione a Doppio Motore**: L'applicazione permette agli utenti di scaricare e gestire due modelli IA separati (es. Gemma per il DM, GGUF per i PG) tramite un'interfaccia dedicata (`ModelActivity`), rendendo il gioco completamente personalizzabile.

* **Navigazione Multi-Activity**: L'app è strutturata con un menu principale che indirizza al tavolo da gioco (`AdventureActivity`) o alla gestione dei motori (`ModelActivity`).

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

---
**Copyright (c) 2025 Michele Lops**

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
