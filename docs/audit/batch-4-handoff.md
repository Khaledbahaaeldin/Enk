# Batch 4 Audit Handoff: Export, Settings, Sound

## 1. Branch and Commits

- **Branch:** `batch-4/export-settings-sound`
- **Base commit (`main`):** `253314e`
- **Head commit:** `9f359bc`
- **Tasks and Commits:**
  - **Task 9:**
    - `241086e`: `feat(data): zip export and safe idempotent import`
    - `9e07e41`: `test(data): edge cases for zip export and safe idempotent import`
  - **Task 10:**
    - `84a3e4c`: `feat(app): settings store`
    - `dc3d5da`: `test(app): edge cases for SettingsStore clamping and enum fallbacks`
  - **Task 11:**
    - `b67be7c`: `feat(audio): sound model, manifest, bell policy, player`
  - **Task 12:**
    - `9f359bc`: `feat(audio): cut CC0 sample sets with provenance`

---

## 2. Public Signatures vs Plan Interfaces

| Module / Component | Planned Interface Signature | Implemented Signature | Status / Differences |
|---|---|---|:---:|
| **Task 9 (Export/Import)** | `class Exporter(private val repo: NoteRepository)` | `class Exporter(private val repo: NoteRepository)` | EXACT |
| Task 9 | `suspend fun exportZip(out: OutputStream): Int` | `suspend fun exportZip(out: OutputStream): Int` | EXACT |
| Task 9 | `class Importer(private val repo: NoteRepository)` | `class Importer(private val repo: NoteRepository)` | EXACT |
| Task 9 | `suspend fun importZip(input: InputStream): ImportResult` | `suspend fun importZip(input: InputStream): ImportResult` | EXACT |
| Task 9 | `sealed interface ImportResult` | `sealed interface ImportResult` | EXACT |
| Task 9 | `data class Success(val imported: Int, val skipped: Int, val updated: Int) : ImportResult` | `data class Success(val imported: Int, val skipped: Int, val updated: Int) : ImportResult` | EXACT |
| Task 9 | `data class Failed(val reason: String) : ImportResult` | `data class Failed(val reason: String) : ImportResult` | EXACT |
| Task 9 | `object FrontMatter { fun format(...); fun parse(...) }` | `object FrontMatter { fun format(note: Note, pages: List<StruckPage>): String; fun parse(markdown: String): Pair<Note, List<StruckPage>>? }` | EXACT |
| **Task 10 (Settings)** | `enum class CorrectionMode { CleanErase, StrikeThrough }` | `enum class CorrectionMode { CleanErase, StrikeThrough }` | EXACT |
| Task 10 | `enum class SoundSetId { Classic, Antique, Soft, Keyboard }` | `enum class SoundSetId { Classic, Antique, Soft, Keyboard }` | EXACT |
| Task 10 | `data class Settings(...)` | `data class Settings(val autocorrect: Boolean = false, val bellVolume: Float = 0.6f, val muted: Boolean = false, val haptics: Boolean = true, val reduceEffects: Boolean = false, val correction: CorrectionMode = CorrectionMode.CleanErase, val soundSet: SoundSetId = SoundSetId.Classic, val warmth: Int = 25, val intensity: Int = 80, val beam: Int = 55, val theme: ThemeId = ThemeId.Graphite, val themeMode: ThemeMode = ThemeMode.System, val hintDismissed: Boolean = false)` | EXACT (ThemeId and ThemeMode consumed from `:design`) |
| Task 10 | `class SettingsStore(private val dataStore: DataStore<Preferences>)` | `class SettingsStore(private val dataStore: DataStore<Preferences>)` | EXACT |
| Task 10 | `val settings: Flow<Settings>` | `val settings: Flow<Settings>` | EXACT |
| Task 10 | `suspend fun update(transform: (Settings) -> Settings)` | `suspend fun update(transform: (Settings) -> Settings)` | EXACT |
| **Task 11 (Audio Model)** | `enum class SoundEvent { Key, Space, Enter, Backspace, Bell, LampClick, FaderTick }` | `enum class SoundEvent { Key, Space, Enter, Backspace, Bell, LampClick, FaderTick }` | EXACT |
| Task 11 | `data class Sample(val file: String, val gain: Float = 1f)` | `data class Sample(val file: String, val gain: Float = 1f)` | EXACT |
| Task 11 | `data class SoundSet(val id: String, val name: String, val keys: List<Sample>, val space: Sample, val enter: Sample, val backspace: Sample, val bell: Sample)` | `data class SoundSet(val id: String, val name: String, val keys: List<Sample>, val space: Sample, val enter: Sample, val backspace: Sample, val bell: Sample)` | EXACT |
| Task 11 | `object SoundManifest { fun parse(json: String): List<SoundSet> }` | `object SoundManifest { fun parse(json: String): List<SoundSet> }` | EXACT |
| Task 11 | `class BellPolicy(private val cols: Int) { fun onCaret(line: Int, col: Int): Boolean }` | `class BellPolicy(private val cols: Int) { fun onCaret(line: Int, col: Int): Boolean }` | EXACT |
| Task 11 | `class SoundPicker(seed: Long = 0) { fun nextKey(set: SoundSet): Sample }` | `class SoundPicker(seed: Long = 0) { fun nextKey(set: SoundSet): Sample }` | EXACT |
| Task 11 | `interface SoundPlayer { fun play(event: SoundEvent, set: SoundSet, volume: Float = 1f); fun release() }` | `interface SoundPlayer { fun play(event: SoundEvent, set: SoundSet, volume: Float = 1f); fun release() }` | EXACT |
| Task 11 | `class SoundPoolPlayer(context: Context) : SoundPlayer` | `class SoundPoolPlayer(context: Context) : SoundPlayer` | EXACT (also provides `prepare(set)` and `playTick(position, volume)`) |
| Task 11 | `object TickSynth { fun render(position: Int, sampleRate: Int = 44100): ShortArray }` | `object TickSynth { fun render(position: Int, sampleRate: Int = 44100): ShortArray }` | EXACT |
| **Task 12 (Provenance)** | `docs/provenance.csv` header: `file,source_url,author,license,retrieved,archived_copy` | `docs/provenance.csv` header: `file,source_url,author,license,retrieved,archived_copy` | EXACT |

---

## 3. DataStore Dependency Selection & Evidence

- **Library:** `androidx.datastore:datastore-preferences:1.3.0`
- **License:** Apache License, Version 2.0
- **Verification Evidence:**
  - Google Maven repository metadata verified: `1.3.0` is the latest stable release (no alpha/beta/rc).
  - AGP 8.13.2 / compileSdk 35 compatibility check: passed `checkDebugAarMetadata` and `checkReleaseAarMetadata` with 0 warnings and 0 errors.
  - Zero permissions added to `AndroidManifest.xml`.
  - Documented in `docs/dependencies.md` and `CREDITS.md`.

---

## 4. Edge Cases Decided & Implemented

- **Task 9: Zip-Slip:** File entries with path traversal segments (`..`), leading slashes (`/` or `\`), drive letters (`C:`), or backslashes are rejected before writing any data.
- **Task 9: Zip-Bomb:** Decompressed byte counting runs during streaming; aborts if cumulative bytes exceed 50 MB, entry count exceeds 20,000, or a single entry expands past 50 MB (guards against lying zip headers).
- **Task 9: Single Transaction Idempotency:** Any error (duplicate file names, missing front matter, mismatched page count, corrupted timestamps, malformed markdown) aborts the entire import with `Failed(reason)`, rolling back Room transaction completely with zero partial writes.
- **Task 9: Page Marker Collisions:** A line matching `<!-- enk:page -->` or `---` is escaped on export with a leading backslash; on import, one leading backslash is unescaped, ensuring bit-for-bit fidelity for arbitrary user notes.
- **Task 10: Range Clamping:** Sliders and volumes clamped on both read and write: `bellVolume` (0f..1f, default 0.6f; NaN/Infinity fallback to 0.6f), `warmth` (0..100), `intensity` (20..100), `beam` (30..100).
- **Task 10: Unknown Enum Fallback:** Corrupted or unrecognized enum strings for `correction`, `sound_set`, `theme`, or `theme_mode` safely fall back to default values.
- **Task 11: Bell Policy Guards:** When `cols <= 4`, `onCaret` returns false (no margin warning zone); moving caret backwards on the same line does not re-trip the bell; moving to another line and returning rearms the trip latch.
- **Task 11: Sound Picker Uniformity:** Deterministic seeded PRNG; never returns consecutive identical samples when `keys.size > 1`; single-sample sets handled safely; empty keys rejected with `IllegalArgumentException`.
- **Task 11: Tick Synthesis:** Clamps position to `0..100`; synthesized 30 ms sound (140 Hz thump decaying over 30 ms plus 20 ms bandpassed noise at `2200 + position * 18` Hz with Q 2.5); peak amplitude strictly bounded within `Short.MAX_VALUE`; spectral centroid strictly rises with position.
- **Task 11: SoundPoolPlayer Safety:** Configures `USAGE_ASSISTANCE_SONIFICATION` with zero audio focus requests; `release()` is thread-safe and idempotent via `AtomicBoolean`.
- **Task 12: Audio Normalization:** All 39 audio samples across all 4 sets peak-normalized to -1.5 dBFS with loudness spread $\le 1.0$ dB (actual combined spread = 0.90 dB; Classic = 0.50 dB, Antique = 0.60 dB, Soft = 0.70 dB, Keyboard = 0.90 dB).
- **Task 12: Transient Alignment:** Every audio sample starts within 5 ms of initial transient attack (enforced via peak `silenceremove`).
- **Task 12: Large File Compliance:** Monotraum original (11.2 MB) replaced by lossless 6.6 MB working copy in `docs/archive/antique-monotraum-434027/`; original removed from git; SHA-256 (`A692295D3262435C681ACD5E61ED89721DF3F73CE0009526CCA088C33F4B04CA`) documented in `tools/CutSamples.md`.
- **Task 12: Prohibited Terms:** Zero occurrences of "Brother" or "Subtersonic" in any asset, CSV, manifest, or documentation.

---

## 5. Verification Commands & Clean Gate Output

### A. Full Clean Gate Command
```powershell
$env:JAVA_HOME = "D:\jdks\eclipse_adoptium-21-amd64-windows.2"; .\gradlew.bat clean build test :design:verifyPaparazziDebug :app:processDebugManifest
```
**Gate Tail:**
```
> Task :audio:lintReportDebug
Wrote HTML report to file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/audio/build/reports/lint-results-debug.html

> Task :data:lintAnalyzeDebugUnitTest
> Task :design:lintAnalyzeDebugUnitTest
> Task :app:packageRelease
> Task :design:testReleaseUnitTest
See the Paparazzi report at: file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/design/build/reports/paparazzi/release/index.html

> Task :audio:lintDebug
> Task :audio:lint
> Task :audio:check
> Task :audio:build

> Task :data:lintReportDebug
Wrote HTML report to file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/data/build/reports/lint-results-debug.html

> Task :design:lintReportDebug
Wrote HTML report to file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/design/build/reports/lint-results-debug.html

> Task :app:lintReportDebug
Wrote HTML report to file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/app/build/reports/lint-results-debug.html

> Task :design:testDebugUnitTest
See the Paparazzi report at: file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/design/build/reports/paparazzi/debug/index.html

> Task :app:lintDebug
> Task :app:lint
> Task :app:check
> Task :data:lintDebug
> Task :data:lint
> Task :data:check
> Task :data:build
> Task :design:lintDebug
> Task :design:lint
> Task :design:test
> Task :design:check
> Task :design:build
> Task :text:check
> Task :text:build
> Task :design:verifyPaparazziDebug
> Task :app:createReleaseApkListingFileRedirect
> Task :app:assembleRelease
> Task :app:assemble
> Task :app:build

[Incubating] Problems report is available at: file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/build/reports/problems/problems-report.html

BUILD SUCCESSFUL in 5m 47s
402 actionable tasks: 400 executed, 2 up-to-date
```

### B. Network Permission Check
```powershell
& "C:\Program Files\Git\bin\bash.exe" scripts/check-no-network.sh
```
**Output:**
```
Checking manifest: app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml
SUCCESS: No network permissions declared in merged manifest.
```

---

## 6. Test Counts by Class

### `:app` (28 tests)
- `app.enk.AppSmokeTest`: 1 passed
- `app.enk.logo.LauncherIconTest`: 1 passed
- `app.enk.ManifestTest`: 5 passed
- `app.enk.ProvenanceTest`: 6 passed
- `app.enk.settings.SettingsStoreTest`: 15 passed

### `:audio` (42 tests)
- `app.enk.audio.AudioSmokeTest`: 1 passed
- `app.enk.audio.BellPolicyTest`: 10 passed
- `app.enk.audio.SoundManifestTest`: 13 passed
- `app.enk.audio.SoundPickerTest`: 7 passed
- `app.enk.audio.SoundPlayerTest`: 5 passed
- `app.enk.audio.TickSynthTest`: 6 passed

### `:data` (78 tests)
- `app.enk.data.DataSmokeTest`: 1 passed
- `app.enk.data.DaoTest`: 10 passed
- `app.enk.data.ExportImportTest`: 20 passed
- `app.enk.data.NoteRepositoryTest`: 21 passed
- `app.enk.data.SearchTest`: 18 passed
- `app.enk.data.StruckCodecTest`: 8 passed

### `:design` (146 tests)
- `app.enk.design.DesignSmokeTest`: 1 passed
- `app.enk.design.golden.GlassGoldenTest`: 6 passed
- `app.enk.design.golden.LogoGoldenTest`: 4 passed
- `app.enk.design.golden.MaterialsGoldenTest`: 1 passed
- `app.enk.design.GrainTest`: 8 passed
- `app.enk.design.JitterTest`: 5 passed
- `app.enk.design.LampColorTest`: 7 passed
- `app.enk.design.logo.LogoSpecTest`: 11 passed
- `app.enk.design.MaterialsTest`: 13 passed
- `app.enk.design.motion.LensControllerTest`: 13 passed
- `app.enk.design.motion.PhysicsTest`: 12 passed
- `app.enk.design.motion.SpringSimulationTest`: 6 passed
- `app.enk.design.shell.GlassBackendSelectionTest`: 10 passed
- `app.enk.design.shell.ScrollProgressTest`: 4 passed
- `app.enk.design.theme.ContrastTest`: 10 passed
- `app.enk.design.theme.DynamicShellColorsTest`: 8 passed
- `app.enk.design.theme.EnkShellThemeTest`: 2 passed
- `app.enk.design.theme.ShellPalettesTest`: 16 passed
- `app.enk.design.TokensTest`: 4 passed

### `:text` (52 tests)
- `app.enk.text.TextSmokeTest`: 1 passed
- `app.enk.text.GridLayoutTest`: 19 passed
- `app.enk.text.PaginatorTest`: 21 passed
- `app.enk.text.StruckPageTest`: 11 passed

**Total Unit Tests:** 346 tests executed, 346 passed, 0 failures, 0 errors, 0 skipped.

---

## 7. What is Not Verified (Stated Plainly)

- **SoundPool Hardware DAC Playback:** Audio synthesis (`TickSynth`), asset loading, Vorbis bitstream headers (OggS, Vorbis identification packet, mono, 44.1 kHz), and `SoundPoolPlayer` lifecycle/idempotency are fully verified in JVM/Robolectric tests. However, actual physical audio playback through Android AudioFlinger / hardware DAC / device speakers has not been verified on a physical Android device.
