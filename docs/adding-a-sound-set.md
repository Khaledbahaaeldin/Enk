# Adding a Sound Set to Enk

This guide details the procedure for adding a new acoustic typewriter sound set to Enk.

## Overview

A sound set in Enk consists of mono OGG Vorbis audio samples (44.1 kHz, 16-bit) representing key strikes, spacebar, carriage return/enter, backspace, and the margin bell.

---

## Step-by-Step Procedure

### 1. Prepare Audio Samples
Prepare mono Ogg Vorbis audio files at 44.1 kHz sample rate:
- At least 5 distinct key strike variations: `key_01.ogg` through `key_05.ogg` (or up to 8).
- `space.ogg`: Spacebar strike.
- `enter.ogg`: Carriage return lever throw.
- `backspace.ogg`: Backspace key mechanism.
- `bell.ogg`: Margin warning bell.

Place them under a new folder: `app/src/main/assets/sounds/<set_id>/`.

### 2. Archive Original Audio & Track Provenance
1. Download the uncompressed original audio files (WAV or FLAC) into `docs/archive/<archive_folder>/`.
2. Append each audio sample to `docs/provenance.csv`:
   ```csv
   file,source_url,author,license,retrieved,archived_copy
   sounds/<set_id>/key_01.ogg,https://...,<author>,CC0,YYYY-MM-DD,docs/archive/...
   ...
   ```
   *Note: Only CC0, CC-BY-4.0, CC-BY-3.0, or CC-BY-2.0 licenses are permitted.*

### 3. Update Audio Manifest
Edit `app/src/main/assets/sounds/manifest.json` and append your sound set object:
```json
{
  "id": "<set_id>",
  "name": "Display Name",
  "keys": [
    { "file": "sounds/<set_id>/key_01.ogg", "gain": 1.0 },
    { "file": "sounds/<set_id>/key_02.ogg", "gain": 1.0 }
  ],
  "space": { "file": "sounds/<set_id>/space.ogg", "gain": 1.0 },
  "enter": { "file": "sounds/<set_id>/enter.ogg", "gain": 1.0 },
  "backspace": { "file": "sounds/<set_id>/backspace.ogg", "gain": 1.0 },
  "bell": { "file": "sounds/<set_id>/bell.ogg", "gain": 1.0 }
}
```

### 4. Required Kotlin Code Changes (Current 1.0 Architecture)

In the current 1.0 codebase, sound sets are strongly typed via the `SoundSetId` enum. Adding a sound set currently requires modifying the following files:

1. **`app/src/main/kotlin/app/enk/settings/Settings.kt`**:
   Add the new identifier to `SoundSetId`:
   ```kotlin
   enum class SoundSetId {
       Classic,
       Antique,
       Soft,
       Keyboard,
       YourNewSet   // <--- Add new entry here
   }
   ```
2. **`app/src/test/kotlin/app/enk/ProvenanceTest.kt`**:
   Update the manifest count assertion (line 105):
   ```kotlin
   assertEquals("manifest.json must parse into exactly 5 sets", 5, sets.size)
   ```

*Note: In a post-1.0 release, dynamic sound set registration will load packs solely from `manifest.json` without requiring Kotlin enum modifications.*

---

## Verification

After adding the sound set, run the verification checks:
```bash
./gradlew :app:testDebugUnitTest --tests "app.enk.ProvenanceTest"
./gradlew :audio:test
bash scripts/check-fdroid-rules.sh
```
All checks must pass.
