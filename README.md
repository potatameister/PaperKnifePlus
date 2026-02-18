#WIP

# PaperKnife+ 📄🔪

Privacy-first PDF utility for Android, built with **Jetpack Compose** and **Kotlin**. This is the native, high-performance successor to the original PaperKnife.

## Features
- **Merge & Split:** Combine PDFs or extract pages visually.
- **Secure:** Add or remove passwords (100% locally).
- **Convert:** Convert Images to PDF and Export PDF pages as Images (ZIP).
- **Edit:** Rotate and Rearrange pages with ease.
- **100% Offline:** No internet permission, no trackers, zero servers.
- **Bento UI:** Clean, modern, and thumb-friendly interface.

## Build Instructions

To build the APK locally, ensure you have the Android SDK installed or use a system with Gradle.

### 1. Clone the repository
```bash
git clone https://github.com/potatameister/PaperKnifePlus
cd PaperKnifePlus
```

### 2. Build the Debug APK
```bash
./gradlew assembleDebug
```
The APK will be located at: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Build the Release APK (Optimized)
```bash
./gradlew assembleRelease
```
The optimized APK will be located at: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Reproducible Builds (RB)
This project is designed for byte-for-byte reproducibility. For F-Droid verification, ensure you use the `SOURCE_DATE_EPOCH` environment variable during build.

## License
Licensed under **GPL-3.0**. See [LICENSE](./LICENSE) for details.
