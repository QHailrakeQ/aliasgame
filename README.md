# Alias Game 🎭

A modern, high-performance Android implementation of the popular board game "Alias", built with **Jetpack Compose** and **Clean Architecture**.

## 🌟 Key Features

- **Fair Play Logic**: Ensures a balanced experience where every team gets an equal number of turns before a winner is declared, allowing for exciting draw scenarios.
- **Multilingual Support**: Fully localized in 4 languages: English, Ukrainian, German, and Russian.
- **Interactive Gameplay**: Card-based UI with vertical swipe gestures for quick scoring (Swipe Up for Correct, Swipe Down for Skip).
- **Audio-Visual Feedback**: Haptic feedback and sound effects for immersive gameplay.
- **Customizable Sessions**: Configure round time, target score, and team names with persistent storage.
- **Dynamic Word Packs**: Word categories are loaded dynamically from localized JSON assets.

## 🛠 Tech Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (100% Declarative UI)
- **Architecture**: MVVM + Clean Architecture principles
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Asynchronous**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Data Persistence**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences)
- **Navigation**: [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- **Splash Screen**: [Modern Splash Screen API](https://developer.android.com/develop/ui/views/launch/splash-screen)

## 🏗 Architecture Overview

The project follows a modular **Clean Architecture** approach:
- **Presentation Layer**: Jetpack Compose screens and ViewModels managing UI state through `StateFlow`.
- **Domain Layer**: Contains pure business logic, including the `GameEngine`, models, and repository interfaces. Independent of Android framework.
- **Data Layer**: Implementation of repositories, handling DataStore persistence and local JSON word source.

## 💾 Data Management: Why DataStore?

For this project, **Jetpack DataStore** was chosen over Room for several reasons:
1. **Lightweight Configuration**: The game stores simple key-value pairs (settings, current team names). Room (SQLite) would be overkill for these requirements.
2. **Reactive Flow**: DataStore is built on Coroutines and Flow, providing out-of-the-box reactive updates to the UI when settings change.
3. **Safety**: It handles data updates transactionally on a background thread, preventing ANR (Application Not Responding) issues common with SharedPreferences.

*Note: Word packs are stored in localized JSON assets for high performance and easy content updates without requiring a complex database migration.*

## 📸 Screenshots

| Setup Screen | Pack Selection | Gameplay |
| :---: | :---: | :---: |
| ![Setup](https://via.placeholder.com/200x400?text=Setup+UI) | ![Packs](https://via.placeholder.com/200x400?text=Packs+UI) | ![Game](https://via.placeholder.com/200x400?text=Gameplay+UI) |

## 🚀 Getting Started

1. Clone the repository.
2. Open the project in **Android Studio Ladybug** or newer.
3. Ensure you have **JDK 17** configured in Gradle settings.
4. Sync the project and run the `:app` module on an emulator or a physical device.

## 🛤 Roadmap

- [ ] Unit tests for `GameEngine` logic.
- [ ] Enhanced error handling for asset loading.
- [ ] UI decomposition into smaller reusable components.
- [ ] CI/CD integration with GitHub Actions.
