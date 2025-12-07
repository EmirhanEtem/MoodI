# WakeMood Planner 🌤️🎵

**Your intelligent morning companion that crafts the perfect day plan and Spotify playlist based on your wake-up mood.**

---

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-%237F52FF?style=for-the-badge&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-1.6.7-blue?style=for-the-badge&logo=jetpackcompose)
![MVVM Architecture](https://img.shields.io/badge/Architecture-MVVM-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)

## 📋 Table of Contents

- [About The Project](#about-the-project)
- [✨ Core Features](#-core-features)
- [📸 Screenshots](#-screenshots)
- [🛠️ Technology Stack](#️-technology-stack)
- [🏛️ Architecture](#️-architecture)
- [🚀 Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation & Setup](#installation--setup)
- [🔑 API Key Configuration](#-api-key-configuration)
- [📄 License](#-license)

## 📖 About The Project

WakeMood Planner is an innovative Android application designed to revolutionize your morning routine. Instead of just giving you a static to-do list, it analyzes *how* you start your day to create a personalized action plan that aligns with your mood and energy levels.

The app intelligently assesses your wake-up state through a combination of factors:
- The number of times you hit the snooze button.
- A real-time analysis of your facial expression.
- A brief voice input about how you're feeling.

Using this data, WakeMood Planner determines your initial mood (e.g., "Energetic & Ready," "Sleepy & Tired," "Grumpy & Stressed"). It then leverages the power of Google's Gemini AI to generate a dynamic, actionable daily plan tailored not only to your mood but also to your profession and specific daily challenges, like an upcoming exam or a project deadline.

The standout feature is its seamless **Spotify integration**. The app can curate a custom music playlist designed to complement your mood, helping you either boost your energy or find calm and focus.

## ✨ Core Features

- **🧠 Intelligent Mood Analysis:** Combines snooze count, camera-based facial analysis, and voice recognition to accurately determine your wake-up mood.
- **🤖 AI-Powered Daily Planning:** Utilizes the **Google Gemini API** to generate highly personalized and adaptive task lists based on your mood, profession, and schedule.
- **🎵 Custom Spotify Playlists:** Generates and creates a new Spotify playlist on your account with songs perfectly matched to your mood and desired duration.
- **👔 Profession-Specific Customization:** Provides tailored plans for different professions like Student, Developer, Doctor, Teacher, and more, taking into account unique professional contexts.
- **👆 Interactive Task Management:** Features a clean, intuitive UI built with Jetpack Compose. Tasks can be expanded to reveal detailed sub-tasks generated on-demand by the AI.
- **🔒 Secure Authentication:** Implements a secure OAuth 2.0 flow with PKCE using the AppAuth library for Spotify login.

## 📸 Screenshots

*(Add your screenshots here. You can drag and drop them into the GitHub editor.)*

| Login Screen | Main Analysis | Plan Display |
| :---: |:---:|:---:|
| ![Login Screen](URL_TO_LOGIN_SCREENSHOT.png) | ![Main Analysis Screen](URL_TO_ANALYSIS_SCREENSHOT.png) | ![Plan Display Screen](URL_TO_PLAN_SCREENSHOT.png) |

| Mood Confirmation | Spotify Playlist Prompt |
| :---: |:---:|
| ![Mood Confirmation](URL_TO_CONFIRMATION_SCREENSHOT.png) | ![Spotify Prompt](URL_TO_SPOTIFY_PROMPT_SCREENSHOT.png) |

## 🛠️ Technology Stack

This project is built with a modern Android technology stack, ensuring it is robust, scalable, and maintainable.

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Architecture:** [MVVM (Model-View-ViewModel)](https://developer.android.com/topic/architecture)
- **Asynchronous Programming:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Artificial Intelligence:** [Google Gemini API](https://ai.google.dev/)
- **Authentication:** [AppAuth for Android](https://github.com/openid/AppAuth-Android) for Spotify OAuth 2.0
- **Networking:** [HttpURLConnection](https://developer.android.com/reference/java/net/HttpURLConnection) for direct API calls to the Spotify Web API.
- **Local Storage:** [SharedPreferences](https://developer.android.com/training/data-storage/shared-preferences) / [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Dependency Injection:** (Implicitly managed via ViewModel Factories)

## 🏛️ Architecture

The application follows the **MVVM (Model-View-ViewModel)** architectural pattern.

-   **View (Composable Screens):** The UI layer, built entirely with Jetpack Compose. It observes state changes from the ViewModel and sends user events to it. It has no business logic.
-   **ViewModel (MoodPlannerViewModel):** Acts as the bridge between the View and the data layer. It holds the UI state (using `StateFlow`), executes business logic, and makes calls to the Repository/Services. The ViewModel is lifecycle-aware and survives configuration changes.
-   **Model (Repository & Services):** The data layer. It's responsible for fetching data from remote sources (Gemini API, Spotify API) and local sources (SharedPreferences). It abstracts the data sources from the rest of the app.

This separation of concerns makes the codebase clean, highly testable, and easy to scale.

## 🚀 Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (Iguana or newer recommended)
- JDK 17 or higher
- A Spotify Developer account and a Google AI Studio account to get API keys.

### Installation & Setup

1.  **Clone the repository:**
    ```sh
    git clone https://github.com/your-username/WakeMood-Planner.git
    ```
2.  **Open the project in Android Studio.** It will automatically sync and download the required Gradle dependencies.

3.  **Configure API Keys:**
    This project requires API keys for Google Gemini and Spotify. You must create a `local.properties` file in the root directory of the project.
    - See the [API Key Configuration](#-api-key-configuration) section below for detailed instructions.

4.  **Configure Spotify Redirect URI:**
    - In your Spotify Developer Dashboard, go to your app's settings.
    - Click "Edit Settings".
    - In the "Redirect URIs" field, add the following URI:
      ```
      com.example.mood://spotify-login-callback
      ```
    - Make sure the `SPOTIFY_REDIRECT_URI_SCHEME` and `SPOTIFY_REDIRECT_URI_HOST` constants in `SpotifyAuthHelper.kt` match this value.

5.  **Build and Run the application:**
    - Select your target device (emulator or physical device).
    - Click the "Run" button in Android Studio.

## 🔑 API Key Configuration

To use the app's core features, you must provide your own API keys.

1.  Navigate to the root directory of the project.
2.  Create a file named `local.properties`.
3.  Add your keys to this file in the following format:

    ```properties
    # Google Gemini API Key
    GEMINI_API_KEY="YOUR_GOOGLE_GEMINI_API_KEY"

    # Spotify Developer Client ID
    SPOTIFY_CLIENT_ID="YOUR_SPOTIFY_CLIENT_ID"
    ```

4.  **How to get the keys:**
    - **GEMINI_API_KEY:** Go to the [Google AI Studio](https://aistudio.google.com/app/apikey) and create a new API key.
    - **SPOTIFY_CLIENT_ID:** Go to your [Spotify Developer Dashboard](https://developer.spotify.com/dashboard), create a new application, and you will find your Client ID in the app's settings.

The project is configured to read these keys from the `local.properties` file, which is included in `.gitignore` to keep your credentials secure. **Do not commit this file to your repository.**

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

**Made with ❤️ for more productive mornings.**
