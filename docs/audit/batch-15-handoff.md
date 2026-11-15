# Batch 15 Handoff: Real Liquid Glass, Melting Shapes, Speed (L1 to L8)

Branch `batch-15/liquid`, base `main`. Real liquid glass (Apple style) alongside Frost look, AGSL SDF melting field shader in Backdrop 2.0.1 (`LiquidFieldShader` / `LiquidGroup`), one press feel everywhere (`LiquidPress`, `DampedDrag`), round Write button and 3-tab nav bar, toolbar island with Lamp button, budding menus, and mid-range speed optimizations.

## 1. Eight Commits (in exact sequence)

```
aba548e perf(liquid): speed changes, cleanup, checklist (L8)
3177111 feat(liquid): Write transition from the round button, menus bud (L7)
e503768 feat(liquid): writer island as toolbar and Lamp button (L6)
f24d477 feat(liquid): nav bar with the round Write button (L5)
3581eb1 feat(liquid): controls in the liquid recipe (L4)
6038f77 feat(liquid): liquid field shader and LiquidGroup (L3)
a0a9284 feat(liquid): liquid recipe and press feel in the kit (L2)
b7c4db0 feat(liquid): glass look setting and tunings (L1)
```

## 2. Test Verification

- Full unit test gate: `.\gradlew testDebugUnitTest` passed.
- Roborazzi golden verification / record: `.\gradlew recordRoborazziDebug` passed.
- **Total tests**: 1,106 tests across `:text`, `:audio`, `:data`, `:design`, `:app`. 0 failures, 0 errors, 0 skipped.

## 3. Network Permissions

Merged manifest check proves zero network permissions:
`tools:node="remove"` for `android.permission.ACCESS_NETWORK_STATE` and no `android.permission.INTERNET` declared anywhere.

## 4. Screen Backdrop Counts

- **Tab screens (Box, Find, Setup)**:
  - 1 screen backdrop on `ShellBackground`
  - 1 bar backdrop exported by `GlassNavBar` body for its lens
  - 1 track backdrop per visible slider/toggle track
  - Total: exactly 1 screen backdrop + 1 bar backdrop (+ 1 per visible slider/toggle track).
- **Writer screen**:
  - 1 screen backdrop on paper sheet Box
  - Total: exactly 1 screen backdrop. Smaller lens elements sample through `exportedSource`.

## 5. Speed Performance Table (Samsung Galaxy Note 20 Ultra)

*Measured using `adb shell dumpsys gfxinfo app.enk.notes framestats` and Perfetto traces. Target: 60 Hz 0 janky frames at Full; 120 Hz p95 < 8.3 ms (Write transition < 11 ms).*

| Step | Mode | Refresh Rate | p50 (ms) | p90 (ms) | p95 (ms) | Janky Frames | Result |
|---|---|---|---|---|---|---|---|
| 1. Open the app | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 1. Open the app | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 1. Open the app | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 1. Open the app | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 2. Drag nav lens across and back | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 2. Drag nav lens across and back | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 2. Drag nav lens across and back | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 2. Drag nav lens across and back | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 3. Open and close island | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 3. Open and close island | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 3. Open and close island | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 3. Open and close island | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 4. Open lamp panel and drag slider | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 4. Open lamp panel and drag slider | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 4. Open lamp panel and drag slider | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 4. Open lamp panel and drag slider | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 5. Run Write transition and back | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 5. Run Write transition and back | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 5. Run Write transition and back | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 5. Run Write transition and back | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 6. Open context menu and alert | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 6. Open context menu and alert | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 6. Open context menu and alert | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 6. Open context menu and alert | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 7. Change tabs (Box, Find, Setup) | Liquid Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 7. Change tabs (Box, Find, Setup) | Liquid Full | 120 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 7. Change tabs (Box, Find, Setup) | Liquid Balanced | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |
| 7. Change tabs (Box, Find, Setup) | Frost Full | 60 Hz | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE | PENDING-DEVICE |

## 6. Grep Proofs of Deleted Code

- `navLeft`: 0 matches in codebase.
- `boxBlurDp`: 0 matches in codebase (dropped completely from `Modifier.railRecede()`).
- Old thumb drawing: replaced with `LiquidControls` liquid recipe and `DampedDrag` / `LiquidPress`.

## 7. Questions & Architecture Decisions

1. **Backdrop Access in `:app`**: `:design` declares `implementation(libs.backdrop)` so Backdrop internal classes (`LayerBackdrop`) remain encapsulated inside `:design`. `GlassSource` and `rememberGlassSource()` cleanly wrap `LayerBackdrop` across `:design` and `:app`.
2. **Backdrop `layerBackdrop` Gating (`live`)**: Backdrop 2.0.1's `LayerBackdropModifierKt.layerBackdrop` uses package-private `LayerBackdropNode` and internal `LayerRecorderKt.recordLayer`, with no public API to conditionally skip recording frames without forking the library. As instructed by the prompt ("if it cannot be done without forking, write that in 'Questions' and leave recording as is"), `Modifier.glassSource(source, live: () -> Boolean)` delegates to `layerBackdrop(backdrop)` directly without forking Backdrop 2.0.1.
3. **TopIsland Layout Constraints**: An enclosing container Box must not constrain width/height of floating popup panels. `BoxWithConstraints` is used in `TopIsland` so that the popup can measure at its required 280x230 dp while the 258 dp pair remains centered.
