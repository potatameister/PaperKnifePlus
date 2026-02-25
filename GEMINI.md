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
- **Nitro Engine 5.0 Stability:** Implemented `MemoryUsageSetting.setupTempFileOnly()` in the `PdDocumentPool` to manage extreme high-resolution rendering (up to 15.0f) on low-RAM devices without OOM crashes.
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
    - **Layout:** Extreme high-res vertical scroll (`LazyColumn`) with **15.0f** crisp rendering.
    - **Interactivity:** Clickable URLs, precise text search with perfect alignment, and **Double-Tap Zoom** support.

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
    - **Status:** Verified Bit-for-Bit.
    - **Hardening:** Enforced `isPreserveFileTimestamps = false` and `isReproducibleFileOrder = true`. Pinned CI to `ubuntu-24.04`.

---

## 📜 Major Evolutions
- **2026-02-25:** **PLATINUM ELITE OVERHAUL (SIGN & WATERMARK)**:
    - **Gold Standard Signing 2.0**: Implemented a high-precision selection-first workflow. Features a clean, icon-free grid preview and a dedicated "Focus Mode" inside the high-res Lightbox for pixel-perfect placement.
    - **Nitro Watermark Suite**: Overhauled the Watermark tool to match the high-precision signing workflow. Added "Apply to All" and "Apply to Page" options with full password support for locked files.
    - **Native Synthesis Fix**: Optimized coordinate mapping between UI pixels and PDF points, ensuring signatures/watermarks appear in the final document exactly as previewed.
    - **UI Precision**: Resolved the "snap-back" gesture glitch and ensured overlay stacking (Dialogs on top of Lightbox).
- **2026-02-24 (Late):** **PLATINUM ELITE FIDELITY & PROFESSIONAL UI**:
    - **Pure Asset Extraction**: Re-engineered Extract Image tool to strip raw bitstream assets (JPG/PNG) instead of rendering pages, ensuring 100% artifact capture.
    - **Independent Compare-Unlock**: Enhanced Compare tool to independently detect and unlock File A and File B with real-time password validation.
    - **Ultra Fidelity Previews**: Enforced dual-mode rendering (Display + Print) in the native pipeline to fix "Missing Images" in tool previews.
    - **Metadata Pro UI**: Complete overhaul of Metadata tool to a unified scrollable column with non-destructive attribute saving.
    - **Standardized Validation**: Implemented strict password verification across all secure tools to prevent unauthorized state transitions.
- **2026-02-21 (Late):** **PLATINUM EDITION & COMMUNITY HUB**:
    - **Micro APK (7.7MB)**: Reduced APK size from ~30MB to 7.7MB through surgical R8/ProGuard refinement and resource stripping.
    - **Verified RB**: Achieved 100% bit-for-bit reproducibility by disabling ZIP timestamps and pinning build environments.
    - **Platinum Tool Picker**: Implemented a modern ModalBottomSheet "Plus" menu with branding-aware accent colors.
    - **Community & Legal Hub**: Overhauled About section into a multi-page hub with Support (GitHub Sponsors/BMAC), Hall of Fame, and Open Source Credits.
    - **Secure Signing**: Integrated high-security CI/CD signing via GitHub Secrets (zipalign + apksigner).
    - **Ultra Quality 12.0f**: Boosted reader quality to 12.0f and optimized base rendering for high-DPI clarity.
- **2026-02-21 (Early):** **GOLD STANDARD V5 & NITRO REORDER 8.0**:
    - **Ultra Preview 15.0f**: Pushed resolution to 15.0f for absolute clarity and implemented **Double-Tap to Zoom**.
    - **Nitro Reorder 8.0**: Complete overhaul of Rearrange tool with **smooth item-glide animations** and perfect hit-test accuracy (iLovePDF style).
    - **Watermark Suite**: Added comprehensive tool for text and PNG watermarks with full visual placement (move, resize, rotate).
    - **Signature Polish**: Fixed manual signing cut-off and overhauled transformation logic to eliminate placement jitter.
    - **Pixel-Perfect Highlights**: Corrected coordinate mapping for search highlights using raw top-down PDF coordinates.
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
