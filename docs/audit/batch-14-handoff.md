# Batch 14 Handoff: Liquid Glass and Motion (G1 to G6)

Branch `batch-14/glass`, base `main`. The effects levels and the frame governor (G1), the glass kit (G2), glass controls (G3), floating layers and the morph (G4), the writer's top island and lamp view (G5), the Rail transition (G5.5) and motion on every screen (G6).

This file was rewritten by the Claude audit. The first handoff Antigravity wrote described files that do not exist (`LiquidGlass.kt`, `GlassMaterials.kt`, `GlassSegmentedControl.kt`, `GlassSwitch.kt`, `GlassDialog.kt`, `GlassBottomSheet.kt`, `GlassDropdown.kt`), signatures that are not in the code (a 30-frame `FrameGovernor`, `GlassSlider(value: Float)`, a `PressDepth` enum with `onClick`, an `object RailTransition`), three "tests broken on purpose" that do not exist, and an empty Questions section; none of it is repeated here. It also said the Rail transition was built: it was not wired into the app. Everything below is read from the code or measured.

## 1. What the audit found and changed (after Antigravity's seven commits)

Blockers:
- **Launch crash at Full and Balanced.** The island was drawn inside the box that records the glass backdrop it samples, so the render tree was cyclic (`RenderNode::prepareTreeImpl` recursing until a SIGSEGV on the emulator). The island, the dark-paper question and the hint now sit beside that box, not in it (`WriterScreen`: `glassSource` is on the box that holds the paper, the lamp light, the rail and the carriage). Checked on the emulator at Full (Auto resolves to Full on API 37): launches, no crash log. No test can catch this: Robolectric draws no blur.
- **The Rail transition (G5.5) was dead code** (`RailTransitionState` was used nowhere). It is now the running app's Write transition and the island's Box button, see section 3.

Majors:
- The island's glyphs were `LogoSpec.CREAM` on a cream island at Light: now `colors.t1`.
- The paper's top edge is 8 dp under the safe area (`PAPER_TOP_DP` 64 to 8) so the island floats over the paper and the text fades under it; the old dead band is gone. `MIN_RAIL_Y_DP` stays 124 (it was `PAPER_TOP_DP + 60`), so no rail position changes.
- The island read its animated size and radius in composition (recomposition per frame). Size is now measured in the layout phase and the radius is a `CornerSize` that depends on the shape's size (`min(28 dp, shorter side / 2)`: round at 150 x 40, 28 dp when open).
- The island now has the press scale on touch-down (`GlassLensMath.BUTTON_PRESS`, through `GlassInteraction`) and cross-fades its content (`AnimatedContent`: fade in 120 ms after 80 ms, fade out 90 ms; 100 ms both at Light).
- The tap-outside layer covers the screen while the island is open (it was sized to the island's own box). New tests: a tap far from the island, Back, and an island that is not interactive.
- `StartGoldenTest` drew a hand-made copy of the old top bar; it now draws the real `TopIsland`.

Minors:
- `MainActivity`: the frame listener follows the effects choice at once (a `SideEffect` re-evaluates it when the setting changes while the app is in front); `currentEffectsChoice` and `currentResolved` are `@Volatile` (read on the frame-metrics thread).
- The four synthetic Rail goldens (rectangles standing in for the screens) were deleted; five goldens of the real shell now replace them (section 5).

## 2. Numbers (from the code)

- Island: Rest 150 x 40, Actions 320 x 110, Lamp 320 x 270; spring `ShellMotion.sheet` (response 0.38 s, damping 0.82); auto-fold after 4500 ms.
- Rail transition: one progress `p`, spring `ShellMotion.railPage` (response 0.72 s, damping 0.76); `p` reaches 0.35 at about 130 ms, 0.6 at 200 ms, 0.9 (the ding) at about 325 ms and settles near 430 ms (simulated with the same spring). The exiting screen is kept 1,100 ms (200 ms with Reduce effects, where `p` runs as a 150 ms tween).
- Beat windows are `RailTransitionMath`: Box recedes `ez(seg(p, .05, .70))` (scale 0.96, up 8 dp, blur 8 dp, brightness 0.35; blur and dim off at Light); nav pill to rail `ez(seg(p, .05, .60))`; steel over glass `seg(p, .50, .62)`; paper clip `out(seg(p, .40, .92))`; keyboard `out(seg(p, .35, .88))`; carriage `back(seg(p, .72, .96))`; island `back(seg(p, .80, 1))`; caret `seg(p, .94, 1)`; Box alpha `1 - seg(p, .72, .92)`.

## 3. How the Rail transition is wired in the running app

- `EnkNavHost` owns one `RailTransitionHost` and provides it as `LocalRailTransition`. A route change that is a Rail pair (`railKind`: a tab to `Route.Writer` is forward; `Route.Writer` to `Route.Box` is reverse; a card tap to `Route.Note` and everything else keep their G6 slides) starts a flight during composition, so the first frame is already right; a `LaunchedEffect` plays it (`run(1f)` or `run(0f)`). A route change that is not a pair ends the flight and settles `p` at the screen's rest value.
- At rest the writer sits at `p = 1` and a tab screen at `p = 0` (`writerP()`, `boxP()`); a finished flight keeps its end value, so the exiting screen (still composed for the hold) cannot flash back over the new one. The audit found that flash on the emulator and fixed it.
- Tab screens (`ShellScaffold`): the screen sits in `railRecede()` (graphicsLayer scale, offset, alpha, blur; a dim scrim), all draw-phase; the nav bar is beside it. `GlassNavBar` hides its glass body while the bar is between the two rests and fades its tabs and lens over `seg(p, 0, .16)`; `RailMorphOverlay` (top of `EnkNavHost`) draws the pill morphing from the bar's real bounds (reported by the bar) to the rail's centre line (reported by the writer).
- Writer (`WriterScreen`): the chassis is painted by the host behind the fading Box; paper (`railPaper`: a clip that grows from a band at the rail), keyboard (`railKeyboard`), steel rail (`railSteel`), carriage (`railCarriage`), caret and hint (`railCaret`), island (`railIsland`; `interactive` only from `p >= 0.95`) all read `p` in graphicsLayer blocks. `DingRing` (600 ms, `#ffb36b`) and the bell (`playSound(SoundEvent.Bell)` when sound is on) and a 14 ms pulse (when haptics are on) fire once per ding.
- Type-on gate: `typeOnGate = { rail.typeOnGate() }` is passed to `WriterScreen`; it is false from the start of a forward flight until the ding, so the paper is blank while it is fed, the type-on starts at the ding, and a key press cancels it as before; in reverse the text stays drawn; Light and reduced motion skip the type-on as before.

## 4. Declarations (by script from the code)

| File | Declaration |
|---|---|
| `EffectsLevel.kt` | `enum class EffectsLevel { Auto, Full, Balanced, Light }` |
| `EffectsLevel.kt` | `enum class ResolvedEffects { Full, Balanced, Light }` |
| `EffectsLevel.kt` | `data class EffectsEnvironment(` |
| `EffectsLevel.kt` | `val sdkInt: Int,` |
| `EffectsLevel.kt` | `val systemReducedMotion: Boolean,` |
| `EffectsLevel.kt` | `val highContrastText: Boolean,` |
| `EffectsLevel.kt` | `val lowRamDevice: Boolean,` |
| `EffectsLevel.kt` | `val batterySaver: Boolean,` |
| `EffectsLevel.kt` | `val slowFrames: ResolvedEffects? = null` |
| `EffectsLevel.kt` | `fun resolveEffects(choice: EffectsLevel, env: EffectsEnvironment): ResolvedEffects` |
| `EffectsLevel.kt` | `val base = when (choice)` |
| `FrameGovernor.kt` | `class FrameGovernor(` |
| `FrameGovernor.kt` | `fun onFrame(totalMs: Float, current: ResolvedEffects): ResolvedEffects?` |
| `GlassSpecs.kt` | `enum class GlassTint { Regular, Thick, Lens, None }` |
| `GlassSpecs.kt` | `enum class GlassShadowKind { Regular, Thick, Small, None }` |
| `GlassSpecs.kt` | `data class GlassSpec(` |
| `GlassSpecs.kt` | `val blurDp: Float,` |
| `GlassSpecs.kt` | `val saturation: Float,` |
| `GlassSpecs.kt` | `val tint: GlassTint,` |
| `GlassSpecs.kt` | `val shadow: GlassShadowKind,` |
| `GlassSpecs.kt` | `val lensHeightDp: Float = 0f,` |
| `GlassSpecs.kt` | `val lensAmountDp: Float = 0f,` |
| `GlassSpecs.kt` | `val fringe: Boolean = false,        // chromaticAberration` |
| `GlassSpecs.kt` | `val depth: Boolean = false,         // depthEffect` |
| `GlassSpecs.kt` | `val highlightWidthDp: Float = 0.5f` |
| `GlassSpecs.kt` | `object GlassSpecs` |
| `GlassSpecs.kt` | `val NavBody        = GlassSpec(24f, 1.5f, GlassTint.Regular, GlassShadowKind.Regular)` |
| `GlassSpecs.kt` | `val NavLens        = GlassSpec(1f, 1.5f, GlassTint.Lens, GlassShadowKind.None, 22f, 46f, fringe = true, depth = true, highlightWidthDp = 1f)` |
| `GlassSpecs.kt` | `val SegmentedBody  = GlassSpec(24f, 1.5f, GlassTint.Regular, GlassShadowKind.Small)` |
| `GlassSpecs.kt` | `val SegmentedLens  = GlassSpec(1f, 1.5f, GlassTint.Lens, GlassShadowKind.None, 14f, 30f, fringe = true, depth = true, highlightWidthDp = 1f)` |
| `GlassSpecs.kt` | `val ToggleTrack    = GlassSpec(14f, 1.5f, GlassTint.Regular, GlassShadowKind.None)` |
| `GlassSpecs.kt` | `val ToggleBead     = GlassSpec(1f, 1.5f, GlassTint.Lens, GlassShadowKind.Small, 14f, 28f, fringe = true, depth = true, highlightWidthDp = 1f)` |
| `GlassSpecs.kt` | `val SliderTrack    = GlassSpec(14f, 1.5f, GlassTint.Regular, GlassShadowKind.None)` |
| `GlassSpecs.kt` | `val SliderThumb    = GlassSpec(1f, 1.5f, GlassTint.Lens, GlassShadowKind.Small, 16f, 34f, fringe = true, depth = true, highlightWidthDp = 1f)` |
| `GlassSpecs.kt` | `val Button         = GlassSpec(14f, 1.5f, GlassTint.Regular, GlassShadowKind.Small, 14f, 26f)` |
| `GlassSpecs.kt` | `val Field          = GlassSpec(24f, 1.5f, GlassTint.Regular, GlassShadowKind.None, 12f, 20f)` |
| `GlassSpecs.kt` | `val TopBarButton   = GlassSpec(14f, 1.5f, GlassTint.Regular, GlassShadowKind.None, 10f, 16f)` |
| `GlassSpecs.kt` | `val Floating       = GlassSpec(44f, 1.6f, GlassTint.Thick, GlassShadowKind.Thick, 18f, 34f)   // menu, sheet, alert, pop-up` |
| `GlassSpecs.kt` | `val Toast          = GlassSpec(24f, 1.5f, GlassTint.Thick, GlassShadowKind.Small)` |
| `GlassSpecs.kt` | `val KeyboardTray   = GlassSpec(44f, 1.6f, GlassTint.Thick, GlassShadowKind.None)` |
| `GlassSpecs.kt` | `fun shadowLayers(kind: GlassShadowKind, material: ShellMaterialTokens): List<ShadowLayer> = when (kind)` |
| `GlassSurface.kt` | `class GlassInteraction` |
| `GlassSurface.kt` | `fun Modifier.glassSurface(` |
| `GlassSurface.kt` | `val colors = LocalShellColors.current` |
| `GlassSurface.kt` | `val material = LocalShellMaterial.current` |
| `GlassSurface.kt` | `val backend = rememberGlassBackend()` |
| `GlassSurface.kt` | `val level = LocalEffects.current` |
| `GlassSurface.kt` | `val reduceEffects = LocalReduceEffects.current` |
| `GlassSurface.kt` | `val shadowLayers = GlassSpecs.shadowLayers(spec.shadow, material)` |
| `GlassSurface.kt` | `val backdrop = source.backdrop ?: emptyBackdrop()` |
| `GlassLensMath.kt` | `object GlassLensMath` |
| `GlassLensMath.kt` | `val NAV_PRESS = 1.08f` |
| `GlassLensMath.kt` | `val SEGMENTED_PRESS = 1.08f` |
| `GlassLensMath.kt` | `val BEAD_PRESS = 1.16f` |
| `GlassLensMath.kt` | `val THUMB_PRESS = 1.14f` |
| `GlassLensMath.kt` | `val BUTTON_PRESS = 1.07f` |
| `GlassLensMath.kt` | `fun stretch(velocityDpPerS: Float): Pair<Float, Float>` |
| `GlassLensMath.kt` | `fun specularAngleDeg(centreX: Float, centreY: Float, touchX: Float, touchY: Float): Float` |
| `GlassLensMath.kt` | `fun pressTarget(pressed: Boolean, depth: Float): Float` |
| `GlassLensMath.kt` | `fun coerceLens(` |
| `GlassButton.kt` | `fun GlassButton(` |
| `GlassButton.kt` | `val currentOnClick by rememberUpdatedState(onClick)` |
| `GlassButton.kt` | `val reduceEffects = LocalReduceEffects.current` |
| `GlassButton.kt` | `val scope = rememberCoroutineScope()` |
| `GlassButton.kt` | `val interaction = remember { GlassInteraction() }` |
| `GlassButton.kt` | `val scaleAnim = remember { Animatable(1f) }` |
| `GlassButton.kt` | `val shape = RoundedCornerShape(23.dp)` |
| `GlassSlider.kt` | `fun GlassSlider(` |
| `GlassSlider.kt` | `val material = LocalShellMaterial.current` |
| `GlassSlider.kt` | `val density = LocalDensity.current` |
| `GlassSlider.kt` | `val reduceEffects = LocalReduceEffects.current` |
| `GlassSlider.kt` | `val currentOnValueChange by rememberUpdatedState(onValueChange)` |
| `GlassSlider.kt` | `val currentOnDetent by rememberUpdatedState(onDetent)` |
| `GlassSlider.kt` | `val currentOnTouchChange by rememberUpdatedState(onTouchChange)` |
| `GlassSlider.kt` | `val scope = rememberCoroutineScope()` |
| `GlassSlider.kt` | `val thumbWidthDp = 56.dp` |
| `GlassSlider.kt` | `val thumbHeightDp = 32.dp` |
| `GlassSlider.kt` | `val thumbRadiusDp = 16.dp` |
| `GlassSlider.kt` | `val trackHeightDp = 6.dp` |
| `GlassSlider.kt` | `val touchHeightDp = 48.dp` |
| `GlassSlider.kt` | `val thumbShape = RoundedCornerShape(thumbRadiusDp)` |
| `GlassSlider.kt` | `val trackShape = RoundedCornerShape(trackHeightDp / 2)` |
| `GlassSlider.kt` | `val thumbInteraction = remember { GlassInteraction() }` |
| `GlassSlider.kt` | `val pressScaleAnim = remember { Animatable(1f) }` |
| `GlassSlider.kt` | `val rawFraction = FaderMath.fractionFor(value, min, max)` |
| `GlassSlider.kt` | `val fractionAnim = remember { Animatable(rawFraction) }` |
| `GlassSlider.kt` | `fun updateFraction(newFrac: Float)` |
| `GlassMorph.kt` | `data class MorphFrame(` |
| `GlassMorph.kt` | `val widthDp: Float,` |
| `GlassMorph.kt` | `val heightDp: Float,` |
| `GlassMorph.kt` | `val radiusDp: Float,` |
| `GlassMorph.kt` | `val offsetXDp: Float,` |
| `GlassMorph.kt` | `val offsetYDp: Float,   // from the collapsed top-left corner` |
| `GlassMorph.kt` | `val labelAlpha: Float,` |
| `GlassMorph.kt` | `val itemAlpha: List<Float>,` |
| `GlassMorph.kt` | `val itemDyDp: List<Float>` |
| `GlassMorph.kt` | `fun morphFrame(` |
| `GlassMorph.kt` | `val safeP = if (p.isNaN()) 0f else p` |
| `GlassMorph.kt` | `val cw = collapsed.width.value` |
| `GlassMorph.kt` | `val ew = expanded.width.value` |
| `GlassMorph.kt` | `val ch = collapsed.height.value` |
| `GlassMorph.kt` | `val eh = expanded.height.value` |
| `GlassMorph.kt` | `val width = maxOf(0f, cw + (ew - cw) * safeP)` |
| `GlassMorph.kt` | `val height = maxOf(0f, ch + (eh - ch) * safeP)` |
| `GlassMorph.kt` | `val pClamped = safeP.coerceIn(0f, 1f)` |
| `GlassMorph.kt` | `val radius = collapsedRadiusDp + (expandedRadiusDp - collapsedRadiusDp) * pClamped` |
| `GlassMorph.kt` | `val offsetX = target.x.value * safeP` |
| `GlassMorph.kt` | `val offsetY = target.y.value * safeP` |
| `GlassMorph.kt` | `val labelAlpha = (1f - safeP * 4f).coerceIn(0f, 1f)` |
| `GlassMorph.kt` | `val itemAlpha = List(itemCount) { i ->` |
| `GlassMorph.kt` | `val dy = (1f - ((safeP - 0.25f) * 2f).coerceIn(0f, 1f)) * 8f` |
| `GlassMorph.kt` | `val itemDyDp = List(itemCount) { dy }` |
| `GlassMorph.kt` | `fun GlassMorph(` |
| `GlassMorph.kt` | `val anim = remember { androidx.compose.animation.core.Animatable(if (expanded) 1f else 0f) }` |
| `GlassMorph.kt` | `val effects = LocalEffects.current` |
| `GlassMorph.kt` | `val reduceEffects = LocalReduceEffects.current` |
| `GlassMorph.kt` | `val colors = LocalShellColors.current` |
| `GlassMorph.kt` | `val p = anim.value` |
| `PressFeedback.kt` | `object PressDepth` |
| `PressFeedback.kt` | `const val Button = 0.96f` |
| `PressFeedback.kt` | `const val Card = 0.985f` |
| `PressFeedback.kt` | `const val Pill = 0.95f` |
| `PressFeedback.kt` | `const val Row = 0.985f` |
| `PressFeedback.kt` | `fun Modifier.pressFeedback(depth: Float, enabled: Boolean = true): Modifier` |
| `PressFeedback.kt` | `val reduceEffects = LocalReduceEffects.current` |
| `PressFeedback.kt` | `val scale = remember { Animatable(1f) }` |
| `PressFeedback.kt` | `val scope = rememberCoroutineScope()` |
| `ShellMotion.kt` | `data class ShellSpring(val response: Float, val damping: Float)` |
| `ShellMotion.kt` | `val stiffness: Float` |
| `ShellMotion.kt` | `fun <T> toSpec(): SpringSpec<T> = spring(dampingRatio = damping, stiffness = stiffness)` |
| `ShellMotion.kt` | `object ShellMotion` |
| `ShellMotion.kt` | `val default = ShellSpring(0.35f, 1f)` |
| `ShellMotion.kt` | `val flick = ShellSpring(0.35f, 0.8f)` |
| `ShellMotion.kt` | `val toggle = ShellSpring(0.28f, 1f)` |
| `ShellMotion.kt` | `val sheet = ShellSpring(0.38f, 0.82f)` |
| `ShellMotion.kt` | `val press = ShellSpring(0.20f, 1f)` |
| `ShellMotion.kt` | `val release = ShellSpring(0.35f, 0.8f)` |
| `ShellMotion.kt` | `val page = ShellSpring(0.40f, 1f)` |
| `ShellMotion.kt` | `val railPage = ShellSpring(0.72f, 0.76f)` |
| `ShellMotion.kt` | `const val LONG_PRESS_MS = 420L` |
| `ShellMotion.kt` | `const val DRAG_SLOP_DP = 8f` |
| `ShellMotion.kt` | `const val FLICK_THRESHOLD = 350f` |
| `RailTransition.kt` | `object RailTransitionMath` |
| `RailTransition.kt` | `fun seg(p: Float, a: Float, b: Float): Float =` |
| `RailTransition.kt` | `fun ez(t: Float): Float = t * t * (3f - 2f * t)` |
| `RailTransition.kt` | `fun out(t: Float): Float = 1f - (1f - t) * (1f - t) * (1f - t)` |
| `RailTransition.kt` | `fun back(t: Float): Float = 1f + 2.9f * (t - 1f) * (t - 1f) * (t - 1f) + 1.9f * (t - 1f) * (t - 1f)` |
| `RailTransition.kt` | `fun boxScale(p: Float): Float = 1f - 0.04f * ez(seg(p, 0.05f, 0.70f))` |
| `RailTransition.kt` | `fun boxTranslationY(p: Float): Float = -8f * ez(seg(p, 0.05f, 0.70f))` |
| `RailTransition.kt` | `fun boxBlurDp(p: Float): Float = 8f * ez(seg(p, 0.05f, 0.70f))` |
| `RailTransition.kt` | `fun boxBrightness(p: Float): Float = 1f - 0.65f * ez(seg(p, 0.05f, 0.70f))` |
| `RailTransition.kt` | `fun boxAlpha(p: Float): Float = 1f - seg(p, 0.72f, 0.92f)` |
| `RailTransition.kt` | `fun navLeft(p: Float, startLeft: Float = 12f): Float = startLeft * (1f - ez(seg(p, 0.05f, 0.60f)))` |
| `RailTransition.kt` | `fun navWidth(p: Float, startW: Float = 336f, endW: Float = 360f): Float =` |
| `RailTransition.kt` | `fun navHeight(p: Float, startH: Float = 64f, endH: Float = 16f): Float =` |
| `RailTransition.kt` | `fun navTop(p: Float, startTop: Float, railY: Float): Float =` |
| `RailTransition.kt` | `fun navRadius(p: Float, startR: Float = 32f, endR: Float = 8f): Float =` |
| `RailTransition.kt` | `fun navContentAlpha(p: Float): Float = 1f - seg(p, 0f, 0.16f)` |
| `RailTransition.kt` | `fun steelRailAlpha(p: Float): Float = seg(p, 0.50f, 0.62f)` |
| `RailTransition.kt` | `fun paperClipU(p: Float): Float = out(seg(p, 0.40f, 0.92f))` |
| `RailTransition.kt` | `fun keyboardTranslationY(p: Float): Float = 240f * (1f - out(seg(p, 0.35f, 0.88f)))` |
| `RailTransition.kt` | `fun carriageTranslationY(p: Float): Float = -80f * (1f - back(seg(p, 0.72f, 0.96f)))` |
| `RailTransition.kt` | `fun carriageAlpha(p: Float): Float = seg(p, 0.72f, 0.80f)` |
| `RailTransition.kt` | `fun islandScale(p: Float): Float = 0.55f + 0.45f * back(seg(p, 0.80f, 1f))` |
| `RailTransition.kt` | `fun islandAlpha(p: Float): Float = seg(p, 0.80f, 1f)` |
| `RailTransition.kt` | `fun caretAlpha(p: Float): Float = seg(p, 0.94f, 1f)` |
| `RailTransition.kt` | `const val DING_AT = 0.90f` |
| `RailTransition.kt` | `const val ISLAND_INTERACTIVE_AT = 0.95f` |
| `RailTransition.kt` | `class RailTransitionState(` |
| `RailTransition.kt` | `val progress = Animatable(initialP)` |
| `RailTransition.kt` | `enum class RailKind { None, Forward, Reverse }` |
| `RailTransition.kt` | `fun railKind(from: Route, to: Route): RailKind` |
| `RailTransition.kt` | `fun isTab(r: Route) = r == Route.Box || r == Route.Find || r == Route.Setup` |
| `RailTransition.kt` | `class RailTransitionHost(initialP: Float)` |
| `RailTransition.kt` | `val state = RailTransitionState(initialP)` |
| `RailTransition.kt` | `fun begin(kind: RailKind)` |
| `RailTransition.kt` | `fun writerP(): Float` |
| `RailTransition.kt` | `fun boxP(): Float` |
| `RailTransition.kt` | `fun flightP(): Float?` |
| `RailTransition.kt` | `fun typeOnGate(): Boolean` |
| `RailTransition.kt` | `fun islandInteractive(): Boolean` |
| `RailTransition.kt` | `val LocalRailTransition = staticCompositionLocalOf<RailTransitionHost?> { null }` |
| `RailTransition.kt` | `fun Modifier.railRecede(): Modifier` |
| `RailTransition.kt` | `val host = LocalRailTransition.current ?: return this` |
| `RailTransition.kt` | `val effects = LocalEffects.current` |
| `RailTransition.kt` | `val blur = effects != ResolvedEffects.Light` |
| `RailTransition.kt` | `fun RailMorphOverlay(host: RailTransitionHost, modifier: Modifier = Modifier)` |
| `RailTransition.kt` | `val colors = LocalShellColors.current` |
| `RailTransition.kt` | `val material = LocalShellMaterial.current` |
| `RailTransition.kt` | `class RailBandShape(` |
| `RailTransition.kt` | `fun Modifier.railPaper(host: RailTransitionHost?, paperTopDp: Float, railYDp: Float): Modifier` |
| `RailTransition.kt` | `fun Modifier.railKeyboard(host: RailTransitionHost?): Modifier` |
| `RailTransition.kt` | `fun Modifier.railCarriage(host: RailTransitionHost?): Modifier` |
| `RailTransition.kt` | `fun Modifier.railSteel(host: RailTransitionHost?): Modifier` |
| `RailTransition.kt` | `fun Modifier.railIsland(host: RailTransitionHost?): Modifier` |
| `RailTransition.kt` | `fun Modifier.railCaret(host: RailTransitionHost?): Modifier` |
| `RailTransition.kt` | `fun DingRing(token: Int, centerX: () -> Float, centerY: () -> Float, modifier: Modifier = Modifier)` |
| `RailTransition.kt` | `val progress = remember { Animatable(1f) }` |
| `RailTransition.kt` | `val seen = remember { token }` |
| `TopIsland.kt` | `enum class IslandState` |
| `TopIsland.kt` | `fun TopIsland(` |
| `TopIsland.kt` | `val colors = LocalShellColors.current` |
| `TopIsland.kt` | `val monoFont = LocalMonoFont.current` |
| `TopIsland.kt` | `val reduceEffects = LocalReduceEffects.current` |
| `TopIsland.kt` | `val scope = rememberCoroutineScope()` |
| `TopIsland.kt` | `val initialW = when (initialState)` |
| `TopIsland.kt` | `val initialH = when (initialState)` |
| `TopIsland.kt` | `val widthAnim = remember { Animatable(initialW) }` |
| `TopIsland.kt` | `val heightAnim = remember { Animatable(initialH) }` |
| `TopIsland.kt` | `fun spec(): AnimationSpec<Float> =` |
| `TopIsland.kt` | `val interaction = remember { GlassInteraction() }` |
| `TopIsland.kt` | `val pressAnim = remember { Animatable(1f) }` |
| `TopIsland.kt` | `object : CornerSize` |
| `TopIsland.kt` | `val colors = LocalShellColors.current` |
| `TopIsland.kt` | `val parsedPath = remember` |
| `TopIsland.kt` | `val glyphInk = if (lampOn) lampColor else ink` |
| `LampPopup.kt` | `fun LampPopupContent(` |
| `LampPopup.kt` | `val colors = LocalShellColors.current` |
| `LampPopup.kt` | `val monoFont = LocalMonoFont.current` |
| `LampPopup.kt` | `val colors = LocalShellColors.current` |
| `LampPopup.kt` | `val monoFont = LocalMonoFont.current` |

## 5. Goldens

New (each was looked at; Roborazzi at threshold 0):
- `RailTransitionGoldenTest_theBoxBeforeWrite`: the Box with its nav bar, one note.
- `RailTransitionGoldenTest_writeAt130ms`: the nav bar has become an empty pill moving toward the rail, the Box recedes (Light: no blur), the first sliver of paper at the rail.
- `RailTransitionGoldenTest_writeAt200ms`: the paper band has grown to about half the page between the pill (now thin) and the rising keyboard; the Box ghost behind it.
- `RailTransitionGoldenTest_writeAt330ms`: the paper is full, the steel rail is in place, the carriage is dropping onto it, the island is arriving; the loaded text has not started.
- `RailTransitionGoldenTest_writeSettled`: the writer at rest, hint visible.

Re-recorded because the paper's top moved (each looked at in the writer and start sets): `PaperGoldenTest_*`, `AssembledScreenGoldenTest_*`, `TopIslandGoldenTest_writerIslandRest{Day,Night}`, `StartGoldenTest_*` (the island fades in last, over the paper).

The island's open states are not covered by a golden of the glass itself: Robolectric draws no blur or refraction, so those goldens show the solid backend.

## 6. Test counts (by script from the Gradle XML of the gate run)

| Module | Variant | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|---|
| `:text` | `test` | 52 | 0 | 0 | 0 |
| `:design` | `testDebugUnitTest` | 202 | 0 | 0 | 0 |
| `:design` | `testReleaseUnitTest` | 175 | 0 | 0 | 0 |
| `:audio` | `testDebugUnitTest` | 42 | 0 | 0 | 0 |
| `:audio` | `testReleaseUnitTest` | 42 | 0 | 0 | 0 |
| `:data` | `testDebugUnitTest` | 88 | 0 | 0 | 0 |
| `:data` | `testReleaseUnitTest` | 88 | 0 | 0 | 0 |
| `:app` | `testDebugUnitTest` | 736 | 0 | 0 | 0 |
| `:app` | `testReleaseUnitTest` | 405 | 0 | 0 | 0 |

Classes this audit added to or changed (debug):

| Class | Tests | Failures | Errors |
|---|---|---|---|
| `RailTransitionTest` | 11 | 0 | 0 |
| `RailNavigationTest` | 1 | 0 | 0 |
| `RailTransitionGoldenTest` | 5 | 0 | 0 |
| `TopIslandTest` | 8 | 0 | 0 |
| `TopIslandDismissTest` | 3 | 0 | 0 |
| `WriterGeometryTest` | 12 | 0 | 0 |
| `TabTransitionTest` | 3 | 0 | 0 |
| `PushedScreenTransitionTest` | 3 | 0 | 0 |

New tests: `RailTransitionTest` (the type-on gate is closed until the ding and open for good; a key press mid-flight leaves the flight and the final text intact; leaving the writer never closes the gate and never dings; rest values outside a flight; a finished flight keeps its end value; the island is not interactive below 0.95; `railKind` of the route pairs), `RailNavigationTest` (island, Box button, the Box with its nav bar and no writer; Write, the writer with its island and no nav bar; both screens exist at 100 ms), `TopIslandDismissTest` (a tap far from the island, Back, an island that is not interactive), `TopIslandTest.idleTimerCollapsesAfter4500Ms` (it now waits for the fade-out of the old content), `WriterGeometryTest` (paper top 8; text offset from the paper top).

## 7. Deviations and checks not run

- **Not run (needs a phone):** refraction, rim light, feel of the press and the ding, frame times of the transition, Auto demoting under load. The emulator played the transition at Full (screen recording at 1x and at 6x slow motion): the Box un-dims, the pill returns to the nav bar and the tabs fade in; the paper band, keyboard, rail, carriage and island arrive in order; no crash; no flash-back. Its frame rate (about 20 fps, with the Box blur) says nothing about a phone's.
- **Not implemented: `sizeFraction`** (the island's shadow and edge highlight scaled with its size). `glassSurface` has no such parameter; adding one is a small change in the kit if wanted.
- **`GlassMorph` still reads its progress in composition** (it is used by one alert, `DeleteForeverAlert`, a one-shot, not a hot path).
- **Light is also "reduced motion" in this app** (`rememberReduceMotion(effects) = effects == Light`), so at Light the ding ring is off and `p` is a 150 ms tween; the prompt keeps the ring at Light and drops it only for reduced motion. The bell and the pulse stay.
- The ding's haptic is the existing 14 ms `pulse`, not `HapticFeedbackType.Confirm`.
- During the morph the pill is drawn as a tinted pill without blur or refraction (the prompt's "no refraction during beat 2"); the glass blur returns when the bar is back at rest.
- The reverse transition is only from `Route.Writer`; the island's Box button from a note opened by a card tap keeps the G6 slide.

## 8. Gate

```
timeout 2400 ./gradlew clean build test :design:verifyRoborazziDebug :app:verifyRoborazziDebug :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata :app:assembleRelease :app:compileDebugAndroidTestKotlin --continue -Pkotlin.incremental=false
```

Last lines:

```
> Task :design:check
> Task :design:build
[Incubating] Problems report is available at: file:///C:/Users/Khaled/Documents/Personal%20Projects/Enk/build/reports/problems/problems-report.html
BUILD SUCCESSFUL in 5m 2s
428 actionable tasks: 403 executed, 25 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.9.1/userguide/configuration_cache_enabling.html
```

`scripts/check-no-network.sh`: SUCCESS. `scripts/check-fdroid-rules.sh`: OK.

Release APK SHA-256 (one build; the two-build comparison was not repeated after the audit's changes): `D51193E45C116B14DF2B12F9C79890B32C53193F75190B0D662B94C04E4B6D66`. Sizes: debug 38,447,653 bytes, release 4,110,765 bytes.

## 9. Commits

```
f8ebf44 fix(glass): batch 14 audit fixes (launch crash, Rail transition wired, island, paper top)
10260ed docs(audit): batch 14 handoff
cb4910c feat(glass): motion on every screen and cleanup (G6)
babc327 feat(glass): top island, lamp view, Rail transition, steel panel removed (G5)
248220e feat(glass): floating layers and the morph (G4)
1b8a792 feat(glass): controls on the kit (G3)
67b4f40 feat(glass): glass kit foundation (G2)
0828a30 feat(glass): effects levels, setting and frame governor (G1)
```
