# PaperKnife+ (Kotlin Edition)

Privacy-first PDF utility for Android, built with Jetpack Compose. A high-performance, native successor to the original PaperKnife.

## Project Vision
- **Zero-Server Architecture:** 100% local processing.
- **Privacy-First:** No trackers, no analytics, no internet permission.
- **Native Performance:** Built with Kotlin and Jetpack Compose for a smooth, "bento-style" UI.
- **F-Droid Ready:** Targeted for F-Droid and IzzyOnDroid with a focus on Reproducible Builds (RB).
- **Size Constraint:** Goal of keeping the APK size under 30MB.

## Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **PDF Engine:** (To be determined - likely `PdfRenderer` or a lightweight native wrapper)
- **Build System:** Gradle (Kotlin DSL) with Version Catalogs

## Reproducible Builds (RB)
To ensure byte-for-byte reproducibility:
- Use fixed versions in `libs.versions.toml`.
- Avoid non-deterministic build timestamps.
- Ensure consistent environment via `gradle-wrapper`.

## Major Evolutions
- **2026-02-16:** Initial project scaffolded with Android Compose and Material 3 theme.
- **2026-02-16:** Implemented core tools: Merge, Split, Protect, Unlock, Rotate, Rearrange, Image-to-PDF, and PDF-to-Images.
- **2026-02-16:** Replicated PaperKnife branding: Logo, Bento-style UI, and "Titan" Bottom Navigation.
- **2026-02-16:** Optimized for F-Droid: 100% offline, R8 tree-shaking enabled, and Credits/About page added.
