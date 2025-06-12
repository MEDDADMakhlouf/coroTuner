# Voice Morpher - Real-time Audio Effects Engine 🎙️

An interactive Android application built with modern Kotlin that processes audio from the microphone in real-time. This project serves as a practical demonstration of digital signal processing (DSP), advanced concurrency with Kotlin Coroutines, and modern UI development with Jetpack Compose.

<br>

## 🚀 Demo
![im](https://github.com/user-attachments/assets/593468cc-0c5a-457a-91ec-df62b66b388c)


<br>

## ✨ Features

* **Real-time Audio Pipeline:** Low-latency audio processing from microphone to speaker using `AudioRecord` and `AudioTrack`.
* **Live Effects:** All effects are applied on-the-fly to the live audio stream.
* **Volume/Gain Control:** Amplify or reduce the volume of the input signal.
* **Dynamic Echo Effect:** A powerful echo with real-time controls for both **Delay** (time between echoes) and **Decay** (how quickly the echo fades).
* **Acoustic Echo Cancellation:** Utilizes Android's built-in `AcousticEchoCanceler` to minimize microphone feedback.
* **Modern, Reactive UI:** A clean, single-screen interface built entirely with Jetpack Compose.

<br>

## 🛠️ Tech Stack

* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
* **Concurrency:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
* **Architecture:** UI Layer (`MainActivity`) separated from a Logic Layer (`AudioEngine`).
* **Audio APIs:** `AudioRecord`, `AudioTrack`, `AcousticEchoCanceler`

<br>

## ⚙️ How to Use

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Build and run on an Android device or emulator.
4.  **For the best experience, use headphones** to prevent acoustic feedback.
5.  Press "Start" and experiment with the sliders to create different sound effects.
