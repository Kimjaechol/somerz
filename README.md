# Gemini Live Translator

Real-time voice translation Android app powered by Google Gemini Live API.

## Features

- Real-time voice translation using Gemini 2.0 Flash Live API
- Support for multiple language pairs:
  - Korean <-> English
  - Korean <-> Japanese
  - Korean <-> Chinese
- Natural voice output with high-quality speech synthesis
- Modern Material Design 3 UI with Jetpack Compose
- Easy language switching with one tap

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 26 or higher
- Firebase project with AI (Gemini) enabled

## Setup

1. Clone this repository
2. Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
3. Enable the Gemini API in your Firebase project
4. Download `google-services.json` from Firebase Console and replace the placeholder file in `app/google-services.json`
5. Build and run the app

## Project Structure

```
app/
├── src/main/
│   ├── java/com/somerz/translator/
│   │   ├── MainActivity.kt
│   │   ├── audio/
│   │   │   ├── AudioPlayer.kt
│   │   │   └── AudioRecorder.kt
│   │   ├── navigation/
│   │   │   └── NavGraph.kt
│   │   ├── ui/
│   │   │   ├── screens/
│   │   │   │   ├── TranslatorScreen.kt
│   │   │   │   └── SettingsScreen.kt
│   │   │   └── theme/
│   │   │       ├── Color.kt
│   │   │       ├── Theme.kt
│   │   │       └── Type.kt
│   │   └── viewmodel/
│   │       └── TranslatorViewModel.kt
│   ├── res/
│   └── AndroidManifest.xml
├── build.gradle.kts
└── google-services.json
```

## How It Works

1. The app uses the microphone to capture your voice
2. Audio is streamed in real-time to the Gemini Live API
3. Gemini translates your speech to the target language
4. The translated speech is played back through the speaker

## Technologies Used

- Kotlin
- Jetpack Compose
- Material Design 3
- Firebase AI (Gemini 2.0 Flash Live)
- Android AudioRecord & AudioTrack
- Navigation Compose
- Kotlin Coroutines & Flow

## Permissions

The app requires the following permissions:
- `RECORD_AUDIO` - For capturing voice input
- `INTERNET` - For communicating with Gemini API

## License

This project is open source and available under the MIT License.
