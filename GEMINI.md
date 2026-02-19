# PaperKnife+ (Kotlin Edition)

Privacy-first PDF utility for Android, built with Jetpack Compose. A high-performance, native successor to the original PaperKnife.

## Project Vision
- **Zero-Server Architecture:** 100% local processing. No trackers, no analytics, no internet permission.
- **Nitro Performance:** Optimized native rendering pipeline for "Blitz" speed on documents of any size.
- **F-Droid Ready:** Targeted for F-Droid and IzzyOnDroid with a focus on Reproducible Builds (RB).
- **Size Constraint:** Aiming for < 30MB APK.

---

## 🚀 Architecture: The Nitro Engine 2.0

PaperKnife+ uses a custom high-concurrency rendering pipeline designed to eliminate the common "jank" associated with PDF previews on Android.

### 1. Rendering Pipeline
- **Native Core:** Leverages Android's `PdfRenderer` (C++ Pdfium wrapper) for hardware-accelerated rendering.
- **Parallel Renderer Pool:** Replaces global mutexes with a 4-thread `Semaphore` and `NativeRendererPool`. This allows up to 4 pages to render concurrently, preventing grid-lock during fast scrolling.
- **Global Blitz Cache:** A singleton `ImageLoader` (Coil) provided via `CompositionLocal`. Ensures that thumbnails rendered in one tool (e.g., Split) are instantly available in others (e.g., Rearrange) without re-rendering.
- **Global Decryption Cache:** Password-protected PDFs are decrypted **once** to a temporary cache file. This allows the high-performance native engine to handle protected docs as if they were standard PDFs, fixing the "Missing Images" bug.

### 2. PDF Preview Types
- **Type A (Grid Preview):** Optimized for selection/reordering.
    - **Layout:** Standardized 3-column scrollable grid (Blitz Pro).
    - **Resolution:** 0.4f scale thumbnails (balanced clarity vs. speed).
    - **Visuals:** Circular progress indicators per page; "Locked" placeholders for encrypted files.
- **Type B (Cover Preview):** Optimized for single-doc processing (Compress, Protect).
    - **Layout:** High-res single-page card (1.2f scale).
    - **Deep Zoom:** Connected to a `PageLightbox` with bound-aware panning and pinch-to-zoom.

### 3. Navigation Architecture
- **Lazy Pager Navigation:** The main app shell (`Home`, `Tools`, `History`, `Settings`) uses a `HorizontalPager`.
- **Performance:** Pages are strictly lazy-loaded (`beyondBoundsPageCount = 0`), eliminating background rendering of inactive tabs.
- **Tool Overlays:** Specific tools open in an `AnimatedVisibility` overlay to keep the main pager state preserved in the background.
- **Intelligent BackHandler:** Custom navigation logic prevents accidental app exit; closes tools or navigates back to Home tab first.

---

## 🛡️ Project Integrity & Trust
- **100% Offline:** Verified. All processing happens on-device using `pdfbox-android` and native `PdfRenderer`.
- **Privacy First:** Zero trackers. The app does not declare `INTERNET` permission in `AndroidManifest.xml`.
- **Reproducible Builds (RB):** 
    - **Status:** High probability. 
    - **Mandate:** All dependencies must use fixed versions in `libs.versions.toml`. No dynamic timestamps in `build.gradle.kts`.

---

## 📜 Major Evolutions
- **2026-02-19:** **BLITZ PRO UPDATE**:
    - Overhauled Tools UI to a polished 3-column "Premium Bento" grid.
    - Implemented `beyondBoundsPageCount = 0` to achieve stable 60FPS.
    - Added **Search Text** and **Share PDF** features to the Lightbox.
    - Introduced **PDF to ZIP**, **Delete Pages**, and **Bookmarks** (Placeholder) tools.
    - Standardized Header UI across History, Tools, and Settings screens.
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
- **"White Pages" Bug:** Native `PdfRenderer` can fail or produce empty white bitmaps if using `RGB_565` on certain devices or if the background isn't explicitly cleared. **Fix:** Always use `ARGB_8888` and `canvas.drawColor(Color.WHITE)` before `page.render`.
- **Pager Lag:** Pre-loading adjacent pages in a `HorizontalPager` (`beyondBounds`) causes massive CPU spikes if pages are complex. **Fix:** Keep `beyondBoundsPageCount = 0` for "Blitz" speed.
- **Gesture Collision:** Overlapping zoom (`transformable`) and pager swipe gestures can lock navigation. **Fix:** Conditionally disable zoom modifiers when `scale == 1.0f` to yield control to the Pager.
- **Concurrency:** `PdfRenderer` is **not thread-safe**. Use `NativeRendererPool` to manage multiple instances securely.

### Dependency Notes
- **PDFBox-Android:** Requires `android.enableJetifier=true`. Package is `com.tom_roush`.
- **Icons:**Extended icons (like `Lock` or `ZoomIn`) require explicit imports and `material-icons-extended` dependency.
- **Memory:** Shared `ImageLoader` must have a capped memory cache (e.g., 25% of RAM) to avoid OOM on 500+ page documents.
