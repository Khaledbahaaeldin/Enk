# Release Smoke Test Walk: Enk 1.0

This checklist guides testing the optimized release APK (`app-release-unsigned.apk`) on an emulator or physical device.

## 1. Preparing and Installing the Release APK

Because `app-release-unsigned.apk` is not signed by a production key, Android requires signing with a temporary key before sideloading via `adb`:

```bash
# Generate a throwaway debug key in a temporary folder outside the repo
keytool -genkeypair -v -keystore /tmp/throwaway.keystore \
  -alias throwaway -keyalg RSA -keysize 2048 -validity 1 \
  -storepass android -keypass android -dname "CN=EnkSmoke, O=Test, C=US"

# Sign the release APK with apksigner
apksigner sign --ks /tmp/throwaway.keystore \
  --ks-pass pass:android --key-pass pass:android \
  --out /tmp/enk-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk

# Install via adb
adb install -r /tmp/enk-release-signed.apk
```

---

## 2. Step-by-Step Functional Smoke Walk

Execute each step and verify the expected behavior:

### Step 1: Cold Launch & Start Animation
- Tap the Enk launcher icon.
- **Verify**: The splash symbol resolves cleanly, the wordmark animates, and transitions seamlessly into the typewriter writing screen without stutter or flicker.

### Step 2: The Writer
- Type a sentence.
- **Verify**:
  - Keys produce typewriter strike sounds and tactile haptic feedback.
  - Carriage moves with each typed letter.
  - Hitting backspace strikes through previous characters (or clean-erases depending on mode).
  - Approaching the right margin rings the return bell.
  - Pressing Enter returns the carriage to the margin stop and advances the paper.

### Step 3: Night Lamp & Control Panel
- Long press the carriage marker (or tap the wordmark dot).
- **Verify**: Night lamp turns on, casting warm light on the paper.
- Open the lamp control panel and adjust Warmth, Intensity, and Beam faders.
- **Verify**: Light temperature and vignette spread respond smoothly to slider movements.

### Step 4: The Box
- Tap the top-left wordmark "Enk" to open the Box.
- **Verify**: The Box displays note count, divider tabs ("Unfiled", "+"), and note cards.
- Tap "+" on the divider strip to add a divider (e.g., "Drafts").
- Long press a note card for 420 ms.
- **Verify**: Context menu pops up with "Move to divider" and "Move to trash".

### Step 5: Find (Search)
- Tap the search icon / tab in the bottom navigation bar.
- Type a word from your typed note into the search field.
- **Verify**: Matching cards appear with query text highlighted in bold ink with an underline.
- Tapping a search hit opens the note at the matched page.

### Step 6: Setup & Appearance
- Tap the Setup tab in the bottom navigation bar.
- Select a theme (e.g., Apricot, Sky, Slate, Mono).
- **Verify**: Shell accents and auras change smoothly; writing paper remains faithful typewriter ivory.
- Verify footers under Appearance, Writing, and Display groups match design specifications.

### Step 7: Credits and Licenses
- In Setup, scroll to the "About" group and tap "Credits and licenses".
- **Verify**: App license (Apache 2.0 full text), font notices (Courier Prime, Inter), sound credits grouped by author, and libraries are listed and scrollable.

### Step 8: Export and Import
- In Setup > Data, tap "Export all notes".
- **Verify**: System file picker suggests `enk-notes-YYYY-MM-DD.zip`. Save it.
- Delete or trash a note in the Box.
- Return to Setup > Data and tap "Import notes". Select the exported ZIP archive.
- **Verify**: Message pill displays "Imported N notes"; deleted note is restored in the Box.

### Step 9: Home-Screen Widget
- Return to the Android home screen and add the Enk widget.
- **Verify**: The widget shows the recent note excerpt and a "+ New" button.
- Tap "+ New".
- **Verify**: App launches directly into a fresh blank note in the writer, bypassing the splash intro.
