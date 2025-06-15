# **Development Roadmap: Immunda Noctis**

This is the roadmap for the application's upcoming features, ordered by priority and increasing complexity.

### **Phase 1: Refactoring and Stability (The Basics)**

1. **Rename ModelActivity to ConfigurationActivity**
    * **Objective**: Make the class name more descriptive, as it now handles audio and game configurations, not just the models.
    * **Complexity**: Low.
2. **Fix TTS Voice Issue and Add Options**
    * **Objective**: Fix the TTS voice bug and add customization options in ConfigurationActivity.
    * **Complexity**: Low/Medium.

### **Phase 2: Core Features**

3. **Random Hero Name Generator**
    * **Objective**: Implement the dice feature in SetupActivity to generate random names from a JSON file.
    * **Complexity**: Medium.
4. **Chat Saving System**
    * **Objective**: Create a system to save chats, with an auto-save option and including the session name in the filename.
    * **Complexity**: Medium.
5. **Character Sheet Screen**
    * **Objective**: Create a new screen to display the hero's stats, skills, and inventory, accessible from the adventure screen.
    * **Complexity**: Medium.

### **Phase 3: Game UI (Placeholders)**

6. **Add Placeholder Icons and Popups in AdventureActivity**
    * **Objective**: Add icons for skills and dice to the chat interface, which will open temporary popups.
    * **Complexity**: Medium.

### **Phase 4: Game Mechanics (Complex Logic)**

7. **Implement Skill Logic**
    * **Objective**: Connect the skill popups to insert action tags (e.g., {lockpick}) into the chat.
    * **Complexity**: High.
8. **Implement Dice Roll Logic and Animation**
    * **Objective**: Make the dice popup functional, with result calculation, animation, and inserting the formatted tag (e.g., {dice3d6\[15\]}) into the chat.
    * **Complexity**: High.

### **Phase 5: Optimization and Intelligence (Final Phase)**

9. **Prompt Engineering and AI Optimization**
    * **Objective**: Improve the DM's narrative quality and the AI model's performance.
    * **Sub-Task (Prompt Engineering)**: Create a detailed *system prompt* to define the DM's personality and rules.
    * **Sub-Task (Performance Optimization)**: Implement advanced options for GPU inference with Gemma.
    * **Complexity**: Very High.
