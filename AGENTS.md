# AGENTS.md — PaperKnife+

Privacy-first, 100% offline Android PDF utility. Kotlin + Jetpack Compose + PDFBox-Android.
Single-Activity architecture with 24 tools across Edit / Optimize / Secure / Convert categories.

---

## Build & Test Commands

```bash
./gradlew assembleDebug                 # Build debug APK
./gradlew assembleRelease               # Build release APK (signed via env vars)
./gradlew bundleRelease                  # Build release AAB
./gradlew lint                           # Run lint checks
./gradlew test                           # Run unit tests (JUnit 4)
./gradlew connectedAndroidTest           # Run instrumented tests (needs device/emulator)
```

To run a single unit test:
```bash
./gradlew test --tests "com.paperknifeplus.app.ExampleTest.someMethod"
```

No Gradle wrapper script? Use `gradle` directly (Termux environment).

---

## Tech Stack

| Layer          | Technology                |
|----------------|---------------------------|
| Language       | Kotlin 2.0.0              |
| UI             | Jetpack Compose (BOM 2024.06.00) + Material3 |
| PDF Engine     | PDFBox-Android 2.0.27.0 (`com.tom-roush`) + native `PdfRenderer` |
| Image Loading  | Coil 2.6.0 (custom `PdfPageFetcher`) |
| Build          | Gradle 9.3.1 + AGP 8.5.0 |
| minSdk / targetSdk | 24 / 34 |
| Java Target    | JVM 17                    |
| Font           | Plus Jakarta Sans (4 weights) |

---

## Architecture

### Entry Point
`app/src/main/java/com/paperknifeplus/app/MainActivity.kt` — Single `ComponentActivity`. Sets up Coil `ImageLoader` with custom `PdfPageFetcher.Factory`, hosts `HorizontalPager` for Home/Tools/History/Settings, and renders tool views via `when(currentTool)`.

### State Management
- **No ViewModels or StateFlow.** All UI state uses Compose `remember` + `mutableStateOf` / `mutableIntStateOf` / `mutableStateListOf`.
- `SessionManager` (`Tool.kt:28`) — global in-memory history (`mutableStateListOf<ActivityEntry>`).
- `PreferencesManager` (in `ToolUtils.kt`) — persists prefs to `SharedPreferences` (theme, author, history retention).
- `ToolState` enum: `SELECTING → UNLOCKING → CONFIGURING → PROCESSING → PREVIEW_RESULT → SUCCESS`.

### PDF Rendering (NITRO ENGINE)
- **Native `PdfRenderer`** for unprotected PDFs — managed by `NativeRendererPool` (4 instances per URI, 10 URI cap).
- **PDFBox `PDDocument`** for encrypted PDFs and all editing — managed by `PdDocumentPool` (3 doc cap, temp-file-only memory mode).
- **Custom Coil Fetcher** (`PdfPageFetcher.kt`) accepts `PdfPageRequest(uri, pageIndex, password, scale, rotation, priority)`.
- **Concurrency:** `Semaphore(4)` limits rendering; `Dispatchers.IO` for high priority, `Dispatchers.Default` for low.

### Preview Types
- **Type A (Grid):** 2-column grid, 0.6f scale — for Split, Delete, Rearrange.
- **Type B (Cover):** Single-page card, 1.2f scale — for Compress, Protect.
- **Type C (Ultra):** Full-screen reader, 12.0f scale, clickable URLs, text search — in `UltraPreview.kt`.

### Navigation
- `HorizontalPager` with `beyondBoundsPageCount = 0` for performance.
- Tools render as `AnimatedVisibility` overlays with `BackHandler` support.
- Tool picker: `ModalBottomSheet`.

---

## Code Style & Conventions

### Naming
- Composables: `PascalCase` (e.g., `MergeView`, `SelectionGrid`, `PageLightbox`)
- Data classes: `PascalCase` (e.g., `MergeFile`, `ActivityEntry`, `PdfPageRequest`)
- Utility functions: `camelCase` (e.g., `getPageCount`, `decryptToCache`, `getUriDetails`)
- Colors: `PaperPink`, `PaperBlue`, `PaperAccent` etc. in `theme/Color.kt`

### Imports
- **No wildcard imports.** Every class imported explicitly.
- PDFBox classes: `com.tom_roush.pdfbox.pdmodel.PDDocument`, etc. (not `org.apache.pdfbox`)
- Icons from `androidx.compose.material.icons.filled.*` and `.outlined.*` explicitly.

### Compose Patterns
- Use `@OptIn(ExperimentalFoundationApi::class)` / `@OptIn(ExperimentalMaterial3Api::class)` for APIs that require it.
- File pickers: `rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents())` or `CreateDocument("application/pdf")`.
- Coroutine scope: `rememberCoroutineScope()` for launching work.
- Heavy work always in `Dispatchers.IO` via `scope.launch(Dispatchers.IO)`.
- Use `withContext(Dispatchers.Main)` for UI updates from IO threads.

### Tool Component Structure
Every tool composable follows this pattern:
1. File selection state → `SELECTING` state
2. Unlock prompt if encrypted → `UNLOCKING` state
3. Configuration UI → `CONFIGURING` state
4. Processing with progress → `PROCESSING` state
5. Success screen with preview/share → `SUCCESS` state

Use `PDFBoxResourceLoader.init(context)` in a `LaunchedEffect(Unit)` block.

### Project Structure
```
app/src/main/java/com/paperknifeplus/app/
├── MainActivity.kt          # Entry point, navigation, theme
├── data/image/PdfPageFetcher.kt  # Custom Coil fetcher, renderer pools
└── ui/
    ├── components/           # 40 .kt files — one per tool + shared components
    │   ├── Tool.kt           # ActivityEntry, Tool, SessionManager
    │   ├── ToolState.kt      # ToolState enum
    │   ├── ToolUtils.kt      # Shared utils, PreferencesManager, rendering helpers
    │   └── *.kt              # One file per tool (MergeView, SplitView, etc.)
    └── theme/                # Color.kt, Theme.kt, Type.kt
```

---

## Critical Technical Gotchas

1. **White Pages Bug:** Always use `Bitmap.Config.ARGB_8888` and call `canvas.drawColor(Color.WHITE)` before `page.render()`. `RGB_565` can produce blank pages on some devices.

2. **PdfRenderer is NOT thread-safe.** Never share one `PdfRenderer` instance across threads. Use `NativeRendererPool` for concurrent access.

3. **beyondBoundsPageCount = 0** in all `HorizontalPager` instances. Preloading adjacent pages causes CPU spikes on complex documents.

4. **Merge Streams:** Always load source documents into a collection and close them in a `finally` block *after* `mergeDocuments` completes. Opening/closing streams in a pipeline must preserve order — closing a source before the merge writes it causes `IOException: Stream Closed`.

5. **Gesture Collision:** Overlapping zoom (`transformable`) and pager swipe locks navigation. Conditionally disable zoom modifiers when `scale == 1.0f`.

6. **Memory Usage:** Coil memory cache capped at 25% of RAM. Use `MemoryUsageSetting.setupTempFileOnly()` for PDFBox on large docs to avoid OOM.

7. **No INTERNET Permission:** The app never declares `INTERNET` in `AndroidManifest.xml`. Never add network-dependent libraries or code.

8. **Reproducible Builds:** `isPreserveFileTimestamps = false` and `isReproducibleFileOrder = true` are set. Do not add build steps that introduce timestamps or non-deterministic ordering.

9. **PDFBox Android Package:** All PDFBox imports use `com.tom_roush.pdfbox.*`, NOT `org.apache.pdfbox.*`.

10. **Icon Set:** Uses `material-icons-extended`. Regular `material-icons` may not have icons like `LockOpen`, `Layers`, `ZoomIn`, etc. Always import explicitly.
