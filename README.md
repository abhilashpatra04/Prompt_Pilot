# 🚀 Prompt Pilot (Android)

> **A Native Android AI Chat Client built with Jetpack Compose, Hilt, and Clean Architecture.**

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat&logo=kotlin)
![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=flat&logo=jetpackcompose)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

**Prompt Pilot** is a cutting-edge Android application designed to provide a seamless conversational experience with advanced AI Agents. Built entirely with **Kotlin** and **Jetpack Compose**, it serves as the robust client-side interface for the [Prompt Pilot Backend](https://github.com/abhilashpatra04/Backend_promptpilot.git), supporting real-time streaming, voice input, and multi-modal interactions (PDF/Images).

---

## 📱 Live Demo

**[Download the Latest APK](https://drive.google.com/file/d/1YwFJmpNlMEwNgkRAXG75LBW4BhkLndrr/view?usp=drive_link)** *Try the live application directly on your Android device.*

---

## ✨ Key Features

* **⚡ Real-Time Streaming:** Implements Server-Sent Events (SSE) handling via OkHttp for instant, token-by-token AI responses.
* **🎨 Modern UI:** 100% Jetpack Compose (Material3) interface with support for Markdown rendering (code blocks, tables, bold text).
* **🗣️ Voice & Multi-Modal:**
    * **Voice Input:** Speak directly to agents using native speech-to-text integration.
    * **Attachments:** Upload and analyze PDFs and Images directly within the chat.
* **💾 Smart Persistence:**
    * **Local Caching:** Room Database for offline message access.
    * **Cloud Sync:** Firebase Firestore integration for syncing chat history across devices.
* **🔐 Secure Architecture:** Built using Dagger Hilt for dependency injection and Clean Architecture principles.

---

## 🛠️ Tech Stack

### Core
* **Language:** [Kotlin](https://kotlinlang.org/) (JDK 17)
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
* **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture

### Libraries & Tools
* **Dependency Injection:** [Dagger Hilt](https://dagger.dev/hilt/)
* **Networking:** [Retrofit2](https://square.github.io/retrofit/) & [OkHttp3](https://square.github.io/okhttp/) (Custom Interceptors)
* **Async Processing:** Coroutines & Kotlin Flow
* **Database:** Room & Firebase Firestore
* **Image Loading:** [Coil](https://coil-kt.github.io/coil/)
* **Markdown:** `jeziellago/compose-markdown` for rich text rendering
* **Build System:** Gradle (Kotlin DSL)

---

## 🏗️ Architecture Overview

The app follows the **Repository Pattern** to separate data logic from UI components:

```mermaid
graph TD
    UI[Compose Screens] -->|Events| VM[ViewModel]
    VM -->|StateFlow| UI
    VM -->|Calls| Repo[Repository Layer]
    
    subgraph Data Layer
        Repo -->|Remote| Retrofit[Retrofit / OkHttp]
        Repo -->|Local| DB[Room / Firebase]
    end
    
    Retrofit -->|JSON/Stream| Backend[External Python Backend]
