# VisionAI Android App (v2)

- Gradle wrapper: 8.9
- Android Gradle Plugin: 8.7.3 (in root build.gradle)
- compileSdk/targetSdk: 34

## How to use

1. Unzip this folder somewhere, e.g. `C:\Users\vinay\AndroidStudioProjects\VisionAI-v2`.
2. In Android Studio: **File → Open** → select the `VisionAI-v2` folder.
3. Let Gradle sync.
4. Open `gradle.properties` and replace `GROQ_API_KEY=your_groq_api_key_here` with your real key from console.groq.com (no quotes).
5. Sync again if needed, then Run on an emulator or device.

Launcher screen is `LoginActivity` with fake sign-in UI.
Main flow: Login → MainActivity → PersonaConfigActivity → ChatActivity.
