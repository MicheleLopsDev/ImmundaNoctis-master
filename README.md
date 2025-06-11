# Immunda Noctis

![Licenza MIT](https://img.shields.io/badge/License-MIT-blue.svg)

Un'applicazione GDR per Android ambientata in un mondo oscuro e spietato, dove un'intelligenza artificiale agisce come Master, narrando storie mature e avventure gotiche.

## 📖 Descrizione

**Immunda Noctis** è un progetto sperimentale che mira a creare un'esperienza di gioco di ruolo (GDR) solitaria, profonda e immersiva. L'applicazione ti getterà in un'ambientazione dark fantasy, dove ogni ombra nasconde un pericolo e ogni scelta ha un peso.

Il cuore del gioco è un'IA che funge da Master: descrive le ambientazioni cupe, interpreta i personaggi tormentati che le popolano e reagisce alle azioni del giocatore, tessendo una narrazione unica e personale. L'obiettivo è creare un'avventura che si senta viva, imprevedibile e spietata.

## ✨ Funzionalità

### Attuali
* [x] Interfaccia di chat di base per interagire con il Master, costruita con Jetpack Compose.
* [x] Gestione dello stato della conversazione tramite ViewModel.

### Pianificate
* [ ] Integrazione di un modello di linguaggio (LLM) per generare le risposte del Master con uno stile narrativo oscuro e gotico.
* [ ] Implementazione di Text-to-Speech (TTS) per dare una voce inquietante al Master e ai PNG.
* [ ] Implementazione di Speech-to-Text (STT) per un'immersione totale.
* [ ] Generazione procedurale di dungeon, cripte e rovine maledette tramite IA.
* [ ] Motore IA per la gestione di regole di gioco brutali (combattimento, sanità mentale, etc.).

## 🛠️ Stack Tecnologico

* **Linguaggio:** [Kotlin](https://kotlinlang.org/)
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Architettura:** MVVM (Model-View-ViewModel)
* **Librerie Principali:**
    * Kotlin Coroutines & Flow
    * AndroidX Lifecycle & ViewModel

## 🚀 Come Iniziare

Per compilare e testare il progetto in locale:

1.  **Clona il repository:**
    ```bash
    git clone [https://github.com/](https://github.com/)MicheleLopsDev/ImmundaNoctis.git
    ```

2.  **Apri con Android Studio:**
    * Apri Android Studio (versione Giraffe o successiva consigliata).
    * Seleziona "Open an existing project" e scegli la cartella `ImmundaNoctis`.

3.  **Esegui l'applicazione:**
    * Attendi la sincronizzazione delle dipendenze Gradle.
    * Seleziona un emulatore o un dispositivo fisico e premi "Run".

## 🤝 Come Contribuire

Questo è un progetto open-source nato dalla passione per le storie oscure. Ogni aiuto è prezioso. Se vuoi contribuire a plasmare questo mondo:

* Apri una [Issue](https://github.com/MicheleLopsDev/ImmundaNoctis/issues) per segnalare bug o proporre nuove idee.
* Crea una [Pull Request](https://github.com/MicheleLopsDev/ImmundaNoctis/pulls) per aggiungere le tue terrificanti creazioni al codice.

## 📄 Licenza

Questo progetto è rilasciato sotto la **Licenza MIT**.

---

**Copyright (c) 2024 Michele Lops**

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