# PaperKnife+ (Kotlin Edition)

Privacy-first PDF utility for Android, built with Jetpack Compose. A high-performance, native successor to the original PaperKnife.

## Project Vision
- **Zero-Server Architecture:** 100% local processing. No trackers, no analytics, no internet permission.
- **Nitro Performance:** Optimized native rendering pipeline for "Blitz" speed on documents of any size.
- **F-Droid Ready:** Targeted for F-Droid and IzzyOnDroid with a focus on Reproducible Builds (RB).
- **Size Constraint:** Aiming for < 30MB APK.

---

## 🚀 Architecture: The Nitro Engine 5.0

PaperKnife+ uses a custom high-concurrency rendering pipeline designed to eliminate the common "jank" associated with PDF previews on Android.

### 1. Rendering Pipeline
- **Native Core:** Leverages Android's `PdfRenderer` (C++ Pdfium wrapper) for hardware-accelerated rendering.
- **Parallel Renderer Pool:** Replaces global mutexes with a 4-thread `Semaphore` and `NativeRendererPool`. This allows up to 4 pages to render concurrently, preventing grid-lock during fast scrolling.
- **Global Blitz Cache:** A singleton `ImageLoader` (Coil) provided via `CompositionLocal`. Ensures that thumbnails rendered in one tool (e.g., Split) are instantly available in others (e.g., Rearrange) without re-rendering.
- **Nitro Engine 5.0 Stability:** Implemented `MemoryUsageSetting.setupTempFileOnly()` in the `PdDocumentPool` to manage extreme high-resolution rendering (up to 7.0f) on low-RAM devices without OOM crashes.
- **Global Decryption Cache:** Password-protected PDFs are decrypted **once** to a temporary cache file. This allows the high-performance native engine to handle protected docs as if they were standard PDFs, fixing the "Missing Images" bug.

### 2. PDF Preview Types
- **Type A (Grid Preview):** Optimized for selection/reordering.
    - **Layout:** Standardized 2-column scrollable grid (Gold Grid).
    - **Resolution:** 0.6f scale thumbnails (balanced clarity vs. speed).
    - **Visuals:** Circular progress indicators per page; "Locked" placeholders for encrypted files.
- **Type B (Cover Preview):** Optimized for single-doc processing (Compress, Protect).
    - **Layout:** High-res single-page card (1.2f scale).
    - **Deep Zoom:** Connected to a `PageLightbox` with bound-aware panning and pinch-to-zoom.
- **Type C (Ultra Preview):** Premium reading experience.
    - **Layout:** Extreme high-res vertical scroll (`LazyColumn`) with **7.0f** crisp rendering.
    - **Interactivity:** **Selectable & Copyable Text** via invisible text layer, clickable URLs, and precise text search with perfect alignment.

### 3. Navigation Architecture
- **Lazy Pager Navigation:** The main app shell (`Home`, `Tools`, `History`, `Settings`) uses a `HorizontalPager`.
- **Performance:** Pages are strictly lazy-loaded (`beyondBoundsPageCount = 0`), eliminating background rendering of inactive tabs.
- **Tool Overlays:** Specific tools open in an `AnimatedVisibility` overlay to keep the main pager state preserved in the background.
- **Intelligent BackHandler:** Custom navigation logic prevents accidental app exit; closes tools or navigates back to Home tab first.

---

## 🛡️ Project Integrity & Trust
- **100% Offline:** Verified. All processing happens on-device using `pdfbox-android` and native `PdfRenderer`.
- **Edge-to-Edge Experience:** Implemented `enableEdgeToEdge()` for immersive UI that covers system status bars.
- **Privacy First:** Zero trackers. The app does not declare `INTERNET` permission in `AndroidManifest.xml`.
- **Reproducible Builds (RB):** 
    - **Status:** High probability. 
    - **Mandate:** All dependencies must use fixed versions in `libs.versions.toml`. No dynamic timestamps in `build.gradle.kts`.

---

## 📜 Major Evolutions
- **2026-02-21:** **ULTRA PREVIEW 10.0f & SELECTABLE TEXT**:
    - **Resolution 10.0f**: Pushed the reader to 10.0f scale for absolute clarity.
    - **Selectable Text**: Refined the invisible text layer and coordinate mapping for reliable text copying.
    - **Signature Suite 2.0**: Added visual placement mode with move, resize (pinch), and rotate support.
    - **Nitro Reorder 7.0**: Implemented absolute hit-testing using `layoutInfo` for perfect drag accuracy.
- **2026-02-20 (Late):** **NITRO ENGINE 5.0 & SIGNATURE SUITE**:
    - **Ultra Preview 7.0f**: Boosted reader resolution to 7.0f for ultimate clarity.
    - **Selectable Text**: Implemented `SelectionContainer` with an invisible text layer, allowing direct text copying from the reader.
    - **Gold Standard Sign Tool**: Added a professional signing tool with a dedicated drawing pad and visual page placement.
    - **Nitro Reorder 6.0**: Overhauled Rearrange tool drag-and-drop with absolute hit-testing and center-point detection for jitter-free swapping.
    - **Coordinate Fix**: Refactored search highlight mapping to use raw top-down PDF coordinates, fixing the offset glitch.
- **2026-02-20 (Early):** **NITRO ENGINE 3.0 & GOLD STANDARD UI**:
    - **Ultra Preview Polish:** Implemented seamless vertical scrolling (no page gaps), accurate link mapping, and a high-performance "Find in Page" search. Added a custom draggable scrollbar.
    - **Gold Standard Rearrange:** Upgraded Rearrange tool to a 2-column grid for larger thumbnails and implemented predictive slot-swap logic.
    - **Universal Previews:** Enforced centered, high-res processing previews across all tools.
- **2026-02-19:** **GOLD STANDARD & BLITZ 4.0**:
    - Refactored **Split** and **Delete** tools to "Gold Standard" (Visual Grid Selection + Sync Range Input).
    - Optimized Nitro Engine to **Blitz 0.4f** scale for crash-free high-speed scrolling.
    - Standardized **LockedFilePrompt** with non-blocking loading states and tool-aware accent colors.
    - Implemented **Smooth Multi-Slot Drag-and-Drop** for Merge tool with spring physics.
    - Centered Processing UI with uncropped A4-accurate previews and informative status text.
    - Added **Jump to Page** feature to Lightbox and fixed sharing stability.
- **2026-02-18:** **NITRO ENGINE 2.0 RELEASE**:
    - Refactored UI to **HorizontalPager** for smooth screen sliding and lazy-loading.
    - Implemented **Parallel Renderer Pool** (4 threads) & **Global Decryption Cache**.
    - Fixed **Zero-White Bug**: Enforced `ARGB_8888` and `RENDER_MODE_FOR_PRINT`.
    - Added **Bound-Aware Zoom**: Intelligent panning logic and pager-lock in Lightbox.
- **2026-02-17:** Enhanced PDF-to-Text, search headers, and fixed 0-byte file saving.
- **2026-02-16:** Initial scaffold, PaperKnife branding, and core PDF logic implementation.

---

## 💡 Critical Lessons & Technical Reference

### Technical Gotchas
- **Troubleshooting:** ALWAYS check the build log errors (e.g., GitHub Actions logs or local `./gradlew` output) first when troubleshooting failures.
- **Merge Streams:** Always load source documents into a collection and close them in a `finally` block *after* `mergeDocuments` completes to prevent `Stream Closed` or `IOException`.
- **"White Pages" Bug:** Native `PdfRenderer` can fail or produce empty white bitmaps if using `RGB_565` on certain devices or if the background isn't explicitly cleared. **Fix:** Always use `ARGB_8888` and `canvas.drawColor(Color.WHITE)` before `page.render`.
- **Pager Lag:** Pre-loading adjacent pages in a `HorizontalPager` (`beyondBounds`) causes massive CPU spikes if pages are complex. **Fix:** Keep `beyondBoundsPageCount = 0` for "Blitz" speed.
- **Gesture Collision:** Overlapping zoom (`transformable`) and pager swipe gestures can lock navigation. **Fix:** Conditionally disable zoom modifiers when `scale == 1.0f` to yield control to the Pager.
- **Concurrency:** `PdfRenderer` is **not thread-safe**. Use `NativeRendererPool` to manage multiple instances securely.

### Dependency Notes
- **PDFBox-Android:** Requires `android.enableJetifier=true`. Package is `com.tom_roush`.
- **Icons:**Extended icons (like `Lock` or `ZoomIn`) require explicit imports and `material-icons-extended` dependency.
- **Memory:** Shared `ImageLoader` must have a capped memory cache (e.g., 25% of RAM) to avoid OOM on 500+ page documents.
