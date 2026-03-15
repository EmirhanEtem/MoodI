<h1 align="center">Moodİ - AI-Powered Mood Planner</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-100%25-purple?style=flat-square&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack_Compose-UI-blue?style=flat-square&logo=android" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Gemini_AI-Integrated-orange?style=flat-square&logo=google" alt="Gemini AI">
  <img src="https://img.shields.io/badge/Spotify_API-Music-1DB954?style=flat-square&logo=spotify" alt="Spotify">
</p>

**Moodİ** is an intelligent, AI-driven Android lifestyle application that analyzes your daily mood using on-device processing and builds highly personalized daily plans specifically tailored to your emotional state, profession, and goals. 

## ✨ Features

* **🧠 AI Emotion Recognition:** Evaluates your morning mood using the device camera (facial expressions) and microphone (voice tone analysis).
* **📋 Smart Daily Plans:** Automatically generates step-by-step tasks and life recommendations for your day based on your detected mood using Google Gemini.
* **🎵 Spotify Integration:** Instantly builds a tailored Spotify playlist designed to match your mood for your preferred duration of time.
* **💾 Secure Data Persistence:** Keeps track of your emotional history, long-term goals, and occupation using Android Room. 
* **🎨 Modern UI/UX:** Built entirely with Jetpack Compose, featuring engaging animations, markdown rendering, and accessible material design.

## 🚀 Getting Started

### Prerequisites
Before running the project, you need to configure your API keys.

1. **Gemini API Key:** 
   - Get a key from [Google AI Studio](https://aistudio.google.com/).
   - Add it to your `local.properties`: `GEMINI_API_KEY=your_key_here`
   
2. **Spotify API Key:**
   - Create an app on the [Spotify Developer Dashboard](https://developer.spotify.com/).
   - Update `SpotifyAuthManager.kt` with your specific `SPOTIFY_CLIENT_ID` and `SPOTIFY_REDIRECT_URI`.

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/EmirhanEtem/MoodI.git
   ```
2. Open the project in **Android Studio**.
3. Sync project with Gradle files.
4. Run the app on a physical device or emulator (Note: Physical device recommended for microphone and camera analysis features).

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose, Material 3
* **Asynchronous Programming:** Coroutines & Flow
* **Local Database:** Room
* **Networking:** HttpURLConnection, Spotify Web API
* **AI & Machine Learning:** ML Kit (Face Detection), Google Generative AI (Gemini 1.5 Flash), Custom Audio processing.

## 📱 Screenshots & Previews

The app dynamically switches its UI color palette and themes based on the mood evaluated:
- **Enerjik (Energetic):** Custom warm and bright tones.
- **Sakin (Calm):** Cool and relaxing overlays.
- **Stresli (Stressed):** Soft tones to reduce cognitive load.

## 🤝 Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## 📝 License

This project is open-source and available to modify.
