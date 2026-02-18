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

## PDF Preview Architecture (Nitro Engine)
- **Type A (Grid Preview):** A scrollable 2-column grid of all pages in a document. Optimized for tools where page selection or reordering is required (e.g., Split, Rearrange, PDF to Image). Uses low-res thumbnails (0.2f scale) for maximum scrolling performance.
- **Type B (Cover Preview):** A single, high-quality (1.2f scale) preview of the first page. Used in tools where a single document is being processed as a whole (e.g., Compress, Protect, Unlock). Supports deep-zoom via Lightbox.

## Major Evolutions
- **2026-02-18:** **NITRO ENGINE 2.0 Stability & UX Refinements**:
    - **Global Decryption Cache:** Password-protected PDFs are now decrypted once to a temporary cache file. This enables high-performance native rendering and fixes "Missing Images" in protected documents.
    - **Visual Fix (Zero-White):** Enforced `ARGB_8888` and `RENDER_MODE_FOR_PRINT` to eliminate the "white pages" bug in the grid.
    - **Intelligent Zoom:** Added bound-aware panning and automatic `HorizontalPager` lock-out when zoomed to prevent UX conflicts.
    - **Type A Grid Consistency:** Standardized 2-column layout applied to all tools (Rearrange, Split, Grayscale, etc.).
- **2026-02-18:** Refined **NITRO ENGINE 2.0** UX:
    - **Standardized Type A Grid:** Unified 2-column layout across all tools (Rearrange, Split, Grayscale, etc.).
    - **Loading States:** Added circular progress indicators to all PDF thumbnails.
    - **Pinch-to-Zoom:** Implemented high-performance zoom and double-tap gestures in the Page Lightbox.
    - **Protected Previews:** Added "Locked" placeholder icons for password-protected files.
- **2026-02-18:** Implemented **NITRO ENGINE 2.0** Core:
    - **Global Blitz Cache:** Unified singleton `ImageLoader` via `CompositionLocal` ensures PDF previews are shared across all tools (Merge, Split, etc.).
    - **Nitro Parallel Rendering:** Replaced the global mutex with a **4-Thread Renderer Pool** and a `NativeRendererPool`. This allows up to 4 pages to render concurrently, eliminating grid scrolling lag.
    - **Memory Optimization:** Switched thumbnails to `RGB_565` and ensured redundant loader allocations are purged for "Zero-Jank" performance.
- **2026-02-18:** Established "Proactive Build Troubleshooting" workflow—monitoring CI logs in real-time and applying hot-fixes while builds are in-flight to maximize iteration speed.
- **2026-02-18:** Unified PDF Preview system (Type A/Grid & Type B/Cover) integrated across all PDF tools using high-performance Coil rendering.
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
