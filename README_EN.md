# Toolbox 🛠️

A multi-function Android utility application with scheduling, recording, statistics, and entertainment features.

![Android](https://img.shields.io/badge/Android-API%2030-blue.svg) ![Java](https://img.shields.io/badge/Language-Java-orange.svg) ![Version](https://img.shields.io/badge/version-1.1.2-brightgreen.svg)

## 📱 Overview

Toolbox is a comprehensive Android utility app that provides multiple practical tools in a single package. The app uses a clean Material Design interface with navigation drawer and bottom navigation for easy access to different functional modules.

## 🚀 Features

### Core Functions

| Feature | Description |
|---------|-------------|
| **定时返回** (Scheduled Return) | Set timers to return to home screen after specified time periods (15-120 mins or custom) |
| **统计** (Statistics) | View data statistics with CSV/Excel import support |
| **录音机** (Recorder) | Audio recording functionality |
| **转盘抽奖** (Lottery Wheel) | Interactive lottery wheel for random selection |
| **定时录屏** (Scheduled Screen Recorder) | Record screen with timer scheduling |
| **更新日志** (Changelog) | Version change history with unread notification badge |
| **开发者入口** (Developer Entry) | Hidden feature unlocked by tapping 5 times on dev entry |

### Additional Modules

- **Gallery** - Image gallery view
- **Slideshow** - Photo slideshow with transition effects
- **Settings** - User profile management

## 🧩 Architecture

The app follows a modern Android architecture:

```
app/
├── ui/
│   ├── home/          # HomeFragment, HomeViewModel
│   ├── gallery/       # GalleryFragment, GalleryViewModel
│   ├── slideshow/     # SlideshowFragment, SlideshowViewModel
│   ├── PanelGridAdapter # Grid adapter for function panels
│   └── settings/      # SettingsFragment, UserProfileManager
├── function/
│   ├── time/          # Timer & lock functionality
│   ├── lottery/       # Lottery wheel system
│   ├── recorder_photo/# Screen & audio recording
│   ├── statistics/    # Data statistics module
│   └── changelog/     # Changelog manager
└── MainActivity       # Main activity with navigation drawer
```

### Key Technologies

- **View Binding** - Type-safe UI binding
- **Navigation Component** - Safe and structured navigation
- **ViewModel + LiveData** - Lifecycle-aware data handling
- **Foreground Services** - For countdown overlay and screen recording
- **AlarmManager** - For precise timing and scheduled tasks
- **SharedPreferences** - Local storage for app state

## 🛠️ Technical Specifications

| Item | Details |
|------|---------|
| **Minimum SDK** | API 30 (Android 11) |
| **Target SDK** | API 34 (Android 14) |
| **Language** | Java 8 |
| **Build System** | Gradle |
| **Architecture** | MVVM + Navigation Component |

## 🔐 Permissions Required

The app requires several permissions for its functionalities:

- `SYSTEM_ALERT_WINDOW` - Overlay permission for countdown display
- `FOREGROUND_SERVICE_*` - For background services (mediaProjection, dataSync, microphone)
- `POST_NOTIFICATIONS` - Notification permissions (Android 13+)
- `SCHEDULE_EXACT_ALARM` - Precise alarm scheduling
- `RECORD_AUDIO`, `CAMERA` - For recording features
- `WRITE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE` - File management

## 🧪 Testing

- **Unit Tests**: `app/src/test/java/` (JUnit, Robolectric)
- **Instrumented Tests**: `app/src/androidTest/java/` (Espresso, JUnit)

## 📦 Build & Run

1. Open the project in Android Studio
2. Sync Gradle dependencies
3. Build and run on an Android device or emulator

## 🔄 Version History

- **v1.1.2** (Current): Latest stable release with updated target SDK and bug fixes
- See the **Changelog** feature within the app for detailed release notes

## 🤝 Contributing

Contributions are welcome! Feel free to open issues or submit pull requests.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

*Built with ❤️ using Android Jetpack components*