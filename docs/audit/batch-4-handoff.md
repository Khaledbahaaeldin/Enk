# Batch 4 Audit Handoff: Export, Settings, Sound

## 1. Branch and commits

- **Branch:** `batch-4/export-settings-sound`, base `main` = `253314e`.
- **Implementer commits:** Task 9 `241086e`, `9e07e41`; Task 10 `84a3e4c`, `dc3d5da`; Task 11 `b67be7c`; Task 12 `9f359bc`; first handoff `8078904`.
- **Audit round 1 fixes:** applied on top of `8078904` in the working tree; they are committed in one commit once the author approves.

## 2. Public signatures against the plan's Interfaces blocks

Compared with `docs/superpowers/plans/2026-11-07-enk-1-0-plan.md` (Tasks 9, 10, 11).

| Component | Plan | Implemented | Difference |
|---|---|---|---|
| Exporter | `class Exporter(repo: NoteRepository, dividers: DividerDao) { suspend fun export(out: OutputStream) }` | same; constructor parameters are `private val` | none that callers see |
| ImportResult | `Done(added, replaced, skipped)`, `Failed(reason)` | same | none |
| Importer | `class Importer(repo, dividers) { suspend fun import(input: InputStream): ImportResult }` | same; `private val` parameters | none |
| Export/Import helpers | not in plan | `Exporter.escapePage`, `generateSlug`, `sanitizePathSegment`, `encodeDivider`, `buildNoteContent`; `Importer.isZipSlip`, `unescapeLine`, `decodeDivider`, `MAX_UNCOMPRESSED_BYTES`, `MAX_ENTRIES` (companion objects, public) | extra public helpers (used by tests) |
| Settings | `data class Settings(correction, autocorrect, soundSet, muted, bellVolume, haptics, reduceEffects, theme, themeMode, ...)` | same fields and order as the plan, plus `warmth`, `intensity`, `beam`, `hintDismissed` | none found against the plan text |
| SettingsStore | `class SettingsStore(dataStore: DataStore<Preferences>) { val settings: Flow<Settings>; suspend fun update(f: (Settings) -> Settings) }` | same; `private val dataStore`; the parameter of `update` is named `f` | none |
| CorrectionMode, SoundSetId | as plan | as plan | none |
| SoundEvent, Sample, SoundSet | as plan | as plan | none |
| SoundManifest | `object SoundManifest { fun parse(json: String): List<SoundSet> }` | same | none |
| BellPolicy, SoundPicker, SoundPlayer | as plan | as plan | none |
| SoundPoolPlayer | `class SoundPoolPlayer(context: Context) : SoundPlayer` | same, plus `prepare(set)` and `playTick(position, volume)` (the plan names `playTick(position: Int)`) | extra `volume` parameter |
| TickSynth | `object TickSynth { fun render(position: Int, sampleRate: Int = 44100): ShortArray }` | same; lives in `SoundPlayer.kt` (the plan does not name its file) | file location only |
| `play(SoundEvent.FaderTick)` | plan: played through `SoundPoolPlayer` | plays a fixed mid-range tick (position 50); the position-aware call is `playTick(position)` | decided, documented in code |

The first handoff listed names that do not exist (`exportZip`, `Success(imported, skipped, updated)`, a `FrontMatter` object, a one-argument `Exporter`); that table was wrong and is replaced by the one above.

## 3. DataStore

`androidx.datastore:datastore-preferences:1.3.0`, Apache-2.0. The auditor checked Google Maven metadata on 2026-11-08: `1.3.0` is the newest stable (after `1.3.0-rc01`). The merged manifest has one permission, the androidx `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (app-internal); no network or other permission. `checkDebugAarMetadata` and `checkReleaseAarMetadata` were previously disabled in :app via enabled = false in app/build.gradle.kts and are enforced from commit `build(app): enforce AAR metadata checks`. Recorded in `docs/dependencies.md` and `CREDITS.md`. The first handoff did not keep evidence of failed versions; none is recorded.

## 4. Edge cases decided

Task 9:
- Zip-slip: names with a `..` or `.` segment, a leading `/` or `\`, a backslash or a colon anywhere are rejected before any write.
- Zip bomb: bytes are counted while decompressing (50 MB total, 50 MB per entry, 20,000 entries); the tests assert the reason.
- Any parse problem (duplicate entry or id, non-`.md` file, empty file, bad UTF-8, bad front matter, wrong `pages:` count, bad date) fails the whole import before the database is touched. A failure inside the transaction rolls everything back (`databaseFailureMidImportRollsEverythingBack`).
- Text is split on `\n` only, so CRLF and lone CR round-trip exactly (`roundTripSpecialTextCharacters`).
- A line that is the page marker or `---` (with any whitespace or backslashes) is escaped with one extra backslash and unescaped on import.
- Divider names are the truth in the front matter. Names with a line break, edge spaces or a leading quote are written as a quoted string (`encodeDivider`/`decodeDivider`); a blank name means Unfiled. Folder names are sanitised, cannot be `.`/`..`, are limited to 50 characters without cutting a surrogate pair. Two dividers with the same name merge on import. All covered by `awkwardDividerNamesRoundTripExactly`.
- A hand-edited page longer than `Paginator.PAGE_SIZE` is split with `Paginator`.
- A note that is in the trash is not restored by an import; its file counts as skipped.
- A replaced note keeps its position unless the divider changes.

Task 10: clamp on write and read; NaN or infinite `bellVolume` becomes the default 0.6; unknown or missing enum names fall back to defaults; an unreadable or corrupted preferences file falls back to defaults (`corruptedFileFallsBackToDefaults`).

Task 11: `BellPolicy` returns false for `cols <= 4`; a backward move on the same line does not fire again; leaving a line and coming back re-arms it. `SoundPicker` is deterministic per seed, never repeats when there are two or more keys, and rejects empty keys. `TickSynth` clamps the position and never exceeds `Short.MAX_VALUE`. `SoundPoolPlayer`: no audio focus; `release()` is idempotent and no longer creates the pool just to release it; each synthesised tick's `AudioTrack` is released when it finishes; asset descriptors are closed.

Task 12 (see `tools/CutSamples.md`, produced by `tools/cut_samples.py`):
- All 39 samples are scaled to one loudness (RMS of the first 100 ms of the file, target -28.43 dBFS); decoded spread 0.59 dB. The metric depends on the window (other windows differ by several dB); stated in `CutSamples.md`.
- Every decoded sample has its first strike's attack within 2.5 ms of its start (the script fails otherwise). The first handoff's claim of "within 5 ms" was false for Classic Space, Backspace and Enter and Antique Enter (179 to 638 ms); all were recut.
- Roles (which moment is Space, Backspace, and so on) were assigned from measurements, not by listening. Enter for the three typewriter sets comes from ramsamba #318686. The Keyboard set's Space, Enter and Backspace are ordinary keypresses of the pack, chosen by centroid, decay and peak.
- Only `Single Keys/` of the OpenGameArt pack is archived; the zip and typing runs are git-ignored, with SHA-256 noted.
- Peaks now range from about -1 to -15 dBFS and the player caps gain at 1.0, so the sets play quieter than before; see section 7.

## 5. Clean gate

```
JAVA_HOME=<JDK 21> ./gradlew.bat clean build test :design:verifyPaparazziDebug :app:processDebugManifest
```
Result (auditor run, after the fixes):
```
BUILD SUCCESSFUL in 4m 23s
402 actionable tasks: 379 executed, 23 up-to-date
```
`scripts/check-no-network.sh`: `SUCCESS: No network permissions declared in merged manifest.`

## 6. Test counts (Gradle XML reports, after the fixes)

- `:app` 29: AppSmokeTest 1, LauncherIconTest 1, ManifestTest 5, ProvenanceTest 6, SettingsStoreTest 16
- `:audio` 42: AudioSmokeTest 1, BellPolicyTest 10, SoundManifestTest 13, SoundPickerTest 7, SoundPlayerTest 5, TickSynthTest 6
- `:data` 85: DaoTest 10, DataSmokeTest 1, ExportImportTest 27, NoteRepositoryTest 21, SearchTest 18, StruckCodecTest 8
- `:design` 141 (Paparazzi golden tests included)
- `:text` 52: TextSmokeTest 1, GridLayoutTest 19, PaginatorTest 21, StruckPageTest 11
- **Total 349, 0 failures, 0 errors, 0 skipped.** The first handoff said 346; its own list summed to 341.

## 7. Not verified

- Playback on a device. Everything about `SoundPoolPlayer` and `AudioTrack` is unverified on hardware.
- The sounds by ear. Roles of the cut samples, the Keyboard set's Space/Enter/Backspace, and whether the sets sit well together were chosen from measurements.
- Loudness and attack are enforced by `tools/cut_samples.py` when it runs, not by a unit test (the JVM cannot decode Vorbis here); `ProvenanceTest` checks files, licenses, manifest and forbidden words.
- The sets are quieter than in the first cut (the 100 ms-equal loudness lowers the peaks). If a device test finds them too quiet, add a master gain; not done.
- A corrupted DataStore file is handled by the store's flow, but where the real `DataStore` is created (a later task) should also set a corruption handler so the file is replaced.
