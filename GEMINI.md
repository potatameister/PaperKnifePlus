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

## Project Integrity & Trust
- **100% Offline:** Verified. All PDF processing (Merge, Split, Text Extraction, etc.) happens entirely on-device using the `pdfbox-android` engine. No internet permission is declared in `AndroidManifest.xml`.
- **Reproducible Builds (RB):** 
    - **Current Status:** High probability but not byte-for-byte guaranteed yet. 
    - **Required for F-Droid/Izzy:** We must ensure the build environment is deterministic.
    - **Next Steps:** Standardize the build container, remove any dynamic timestamps in `build.gradle.kts`, and ensure all dependencies are resolved from fixed versions in `libs.versions.toml`.
- **Privacy First:** Zero trackers, zero analytics. The app does not have the ability to reach the network.

## Major Evolutions
- **2026-02-17:** Implemented Search and Icon headers for Tools and History.
- **2026-02-17:** Fixed 0-byte file saving issues by enforcing memory-only merging and explicit stream flushing.
- **2026-02-17:** Refined UI: Increased radial hue size (320.dp), more compact tool items, and dark-mode optimized FAB edges.
- **2026-02-17:** Enhanced PDF-to-Text with "No text found" detection (identifying need for OCR).
- **2026-02-16:** Initial project scaffolded with Android Compose and Material 3 theme.
- **2026-02-16:** Implemented core tools: Merge, Split, Protect, Unlock, Rotate, Rearrange, Image-to-PDF, and PDF-to-Images.
- **2026-02-16:** Replicated PaperKnife branding: Logo, Bento-style UI, and "Titan" Bottom Navigation.
- **2026-02-16:** Optimized for F-Droid: 100% offline, R8 tree-shaking enabled, and Credits/About page added.
- **2026-02-16:** Hardened Build System: Resolved persistent CI hangs and dependency resolution issues.

## Critical Lessons & Build Fixes
- **PDFBox-Android (Legacy Support):** This library requires `android.enableJetifier=true` in `gradle.properties` to resolve correctly.
- **Package Naming:** The correct package for PDFBox-Android is `com.tom_roush` (with an underscore). Specifically, use `com.tom_roush.pdfbox.android.PDFBoxResourceLoader` for initialization.
- **Icon Resolution:** Wildcard imports for `androidx.compose.material.icons.filled.*` often fail in CI for extended icons. Use **explicit imports** for all icons (e.g., `import androidx.compose.material.icons.filled.GridView`) and ensure `material-icons-extended` is in the dependencies.
- **Resource Allocation:** Gradle builds for this project require at least `4096m` heap size in CI to avoid GC-related hangs and log flooding.
- **Kotlin Operators:** Use explicit property access when using operators like `rem` on custom types to avoid ambiguity during compilation.
