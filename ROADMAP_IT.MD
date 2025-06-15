# **Piano di Sviluppo: Immunda Noctis**

Questa è la roadmap per le prossime funzionalità dell'applicazione, ordinata per priorità e complessità crescente.

### **Fase 1: Refactoring e Stabilità (Le basi)**

1. **Rinominare ModelActivity in ConfigurationActivity**
    * **Obiettivo**: Rendere il nome della classe più descrittivo, dato che ora gestisce configurazioni audio e di gioco, non solo i modelli.
    * **Complessità**: Bassa.
2. **Risolvere Problema Voce TTS e Aggiungere Opzioni**
    * **Obiettivo**: Correggere il bug della voce TTS e aggiungere opzioni di personalizzazione in ConfigurationActivity.
    * **Complessità**: Bassa/Media.

### **Fase 2: Funzionalità di Base**

3. **Generatore di Nomi Casuali per l'Eroe**
    * **Obiettivo**: Implementare la funzionalità del dado in SetupActivity per generare nomi casuali da un file JSON.
    * **Complessità**: Media.
4. **Sistema di Salvataggio della Chat**
    * **Obiettivo**: Creare un sistema per salvare le chat, con opzione di auto-salvataggio e nome della sessione nel nome del file.
    * **Complessità**: Media.
5. **Schermata Scheda del Personaggio**
    * **Obiettivo**: Creare una nuova schermata per visualizzare statistiche, abilità e inventario dell'eroe, accessibile dalla schermata di avventura.
    * **Complessità**: Media.

### **Fase 3: Interfaccia di Gioco (Placeholder)**

6. **Aggiungere Icone e Popup Placeholder in AdventureActivity**
    * **Obiettivo**: Aggiungere le icone per abilità e dado all'interfaccia della chat, che aprono popup temporanei.
    * **Complessità**: Media.

### **Fase 4: Meccaniche di Gioco (Logica Complessa)**

7. **Implementare Logica delle Abilità**
    * **Obiettivo**: Collegare i popup delle abilità per inserire i tag delle azioni (es. {forza\_serratura}) nella chat.
    * **Complessità**: Alta.
8. **Implementare Logica e Animazione del Lancio Dadi**
    * **Obiettivo**: Far funzionare il popup del dado, con calcolo del risultato, animazione e inserimento del tag formattato (es. {dice3d6\[15\]}) nella chat.
    * **Complessità**: Alta.

### **Fase 5: Ottimizzazione e Intelligenza (Fase Finale)**

9. **Prompt Engineering e Ottimizzazione AI**
    * **Obiettivo**: Migliorare la qualità della narrazione del DM e le performance del modello AI.
    * **Sotto-Task (Prompt Engineering)**: Creare un *system prompt* dettagliato per definire la personalità e le regole del DM.
    * **Sotto-Task (Ottimizzazione Performance)**: Implementare le opzioni avanzate per l'inferenza su GPU con Gemma.
    * **Complessità**: Molto Alta.
