# Enk Sound Set Slicing and Normalization (`tools/CutSamples.md`)

This document records the exact steps, source audio provenance, ffmpeg commands, cutting parameters, and loudness normalization results used to create the 4 sound sets in Enk 1.0 (`Classic`, `Antique`, `Soft`, and `Keyboard`).

All commands are documented using standard portable `ffmpeg` invocations without machine-specific paths.

---

## 1. Source Provenance and Archives

| Set | Source URL | Author | License | Retrieved | Archive Path |
|---|---|---|---|---|---|
| **Classic** | https://freesound.org/people/keithpeter/sounds/123344/ | keithpeter | CC0 | 2026-11-08 | `docs/archive/classic-keithpeter-123344/123344__keithpeter__typewriter-olivetti-lettra-22.wav` (9.4 MB) |
| **Antique** | https://freesound.org/people/monotraum/sounds/434027/ | monotraum | CC0 | 2026-11-08 | `docs/archive/antique-monotraum-434027/434027__monotraum__old-typewriter-working.wav` (6.6 MB working copy) |
| **Soft** | https://freesound.org/people/blukotek/sounds/246057/ | blukotek | CC0 | 2026-11-08 | `docs/archive/soft-blukotek-246057/246057__blukotek__typewriter.wav` (2.4 MB) |
| **Keyboard** | https://opengameart.org/content/keyboard-soundpack-1-typing-and-single-keystrokes | unicaegames | CC0 | 2026-11-08 | `docs/archive/keyboard-opengameart-soundpack-1/` |
| **Shared Bell** | https://freesound.org/people/knufds/sounds/345955/ | knufds | CC0 | 2026-11-08 | `docs/archive/bell-knufds-345955/345955__knufds__typewriter-bell-carriage-reset.wav` |
| **Alternate Bell** | https://freesound.org/people/ramsamba/sounds/318687/ | ramsamba | CC0 | 2026-11-08 | `docs/archive/bell-ramsamba-318687/318687__ramsamba__typewriter-bell.wav` |

### Note on Large Antique Original:
The original file `434027__monotraum__old-typewriter.wav` downloaded from Freesound is 11,760,044 bytes (11.2 MB), which exceeds the project repository's 10 MB per-file threshold.
Per project requirements, the full 11.2 MB original is not tracked in git. A lossless cut working copy (PCM 24-bit stereo 44.1 kHz, 0 to 25.08 s, 6,635,596 bytes) containing all candidate audio sections was created as `docs/archive/antique-monotraum-434027/434027__monotraum__old-typewriter-working.wav`.
- Original file SHA-256: `A692295D3262435C681ACD5E61ED89721DF3F73CE0009526CCA088C33F4B04CA`
- Original source URL: `https://freesound.org/people/monotraum/sounds/434027/`

---

## 2. Audio Processing Standards

All samples are produced to the following audio invariants:
1. **Format:** Ogg Vorbis mono (`-ac 1`), 44.1 kHz (`-ar 44100`), quality 4 (`-c:a libvorbis -q:a 4`).
2. **Transient Alignment:** Every sample begins within 5 ms of the initial attack transient. This is enforced with ffmpeg's `silenceremove` filter:
   `-af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak"`
3. **Loudness Normalization:** Peak normalization to -1.5 dBFS (`max_volume = -1.5 dB`), ensuring that every sample in every set has maximum impact without digital clipping, and the loudness spread within each set and across all sets combined is strictly within 1.0 dB.

---

## 3. Cutting Invocations

### A. Classic Set (8 keys, space, enter, backspace, bell)
Source: `123344__keithpeter__typewriter-olivetti-lettra-22.wav`
- `key_01.ogg`: `ffmpeg -y -ss 13.15 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=12.5dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/classic/key_01.ogg`
- `key_02.ogg`: `ffmpeg -y -ss 13.92 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=12.8dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/classic/key_02.ogg`
- `key_03.ogg`: `ffmpeg -y -ss 14.18 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=12.6dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/classic/key_03.ogg`
- `key_04.ogg`: `ffmpeg -y -ss 15.10 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=12.6dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/classic/key_04.ogg`
- `key_05.ogg`: `ffmpeg -y -ss 15.95 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=13.0dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/classic/key_05.ogg`
- `key_06.ogg`: `ffmpeg -y -ss 16.27 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=12.8dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/classic/key_06.ogg`
- `key_07.ogg`: `ffmpeg -y -ss 17.06 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=12.7dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/classic/key_07.ogg`
- `key_08.ogg`: `ffmpeg -y -ss 17.50 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=12.5dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/classic/key_08.ogg`
- `space.ogg`: `ffmpeg -y -ss 18.44 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=12.6dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/classic/space.ogg`
- `enter.ogg`: `ffmpeg -y -ss 24.59 -t 0.65 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=12.8dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/classic/enter.ogg`
- `backspace.ogg`: `ffmpeg -y -ss 18.81 -t 0.20 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=12.5dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/classic/backspace.ogg`
- `bell.ogg`: `ffmpeg -y -ss 0.00 -t 1.20 -i docs/archive/bell-knufds-345955/345955__knufds__typewriter-bell-carriage-reset.wav -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=1.2dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/classic/bell.ogg`

### B. Antique Set (5 keys, space, enter, backspace, bell)
Source: `434027__monotraum__old-typewriter-working.wav`
- `key_01.ogg`: `ffmpeg -y -ss 1.50 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=4.5dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/antique/key_01.ogg`
- `key_02.ogg`: `ffmpeg -y -ss 2.04 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=4.6dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/antique/key_02.ogg`
- `key_03.ogg`: `ffmpeg -y -ss 2.88 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=4.4dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/antique/key_03.ogg`
- `key_04.ogg`: `ffmpeg -y -ss 3.32 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=4.0dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/antique/key_04.ogg`
- `key_05.ogg`: `ffmpeg -y -ss 4.03 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=4.3dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/antique/key_05.ogg`
- `space.ogg`: `ffmpeg -y -ss 4.77 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=4.1dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/antique/space.ogg`
- `enter.ogg`: `ffmpeg -y -ss 6.08 -t 0.85 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=4.3dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/antique/enter.ogg`
- `backspace.ogg`: `ffmpeg -y -ss 5.33 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=4.1dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/antique/backspace.ogg`
- `bell.ogg`: shared bell from `345955__knufds__typewriter-bell-carriage-reset.wav`

### C. Soft Set (5 keys, space, enter, backspace, bell)
Source: `246057__blukotek__typewriter.wav`
- `key_01.ogg`: `ffmpeg -y -ss 4.05 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,lowpass=f=4500,volume=8.2dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/soft/key_01.ogg`
- `key_02.ogg`: `ffmpeg -y -ss 4.56 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,lowpass=f=4500,volume=7.8dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/soft/key_02.ogg`
- `key_03.ogg`: `ffmpeg -y -ss 5.17 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,lowpass=f=4500,volume=7.7dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/soft/key_03.ogg`
- `key_04.ogg`: `ffmpeg -y -ss 5.88 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,lowpass=f=4500,volume=7.8dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/soft/key_04.ogg`
- `key_05.ogg`: `ffmpeg -y -ss 8.27 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,lowpass=f=4500,volume=7.8dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/soft/key_05.ogg`
- `space.ogg`: `ffmpeg -y -ss 1.33 -t 0.22 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,lowpass=f=4500,volume=8.1dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/soft/space.ogg`
- `enter.ogg`: `ffmpeg -y -ss 6.53 -t 0.70 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,lowpass=f=4500,volume=7.5dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/soft/enter.ogg`
- `backspace.ogg`: `ffmpeg -y -ss 12.86 -t 0.25 -i <src> -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,lowpass=f=4500,volume=8.0dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/soft/backspace.ogg`
- `bell.ogg`: `ffmpeg -y -ss 0.00 -t 0.88 -i docs/archive/bell-ramsamba-318687/318687__ramsamba__typewriter-bell.wav -af "silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak,volume=0.8dB" -ac 1 -ar 44100 -c:a libvorbis -q:a 4 sounds/soft/bell.ogg`

### D. Keyboard Set (5 keys, space, enter, backspace, bell)
Source: `docs/archive/keyboard-opengameart-soundpack-1/unicae_games_keyboard_soundpack_1/Single Keys/`
- `key_01.ogg` through `key_05.ogg`: cut from `keypress-001.wav` through `keypress-005.wav`
- `space.ogg`: cut from `keypress-020.wav` (longer spacebar strike)
- `enter.ogg`: cut from `keypress-030.wav` (heavier enter key return stroke)
- `backspace.ogg`: cut from `keypress-015.wav` (short backspace click)
- `bell.ogg`: shared bell from `345955__knufds__typewriter-bell-carriage-reset.wav`

---

## 4. Loudness Normalization Results

The volume detection filter (`volumedetect`) was executed over all 39 final `.ogg` assets.

| Set | Total Samples | Peak Min (dBFS) | Peak Max (dBFS) | Peak Spread (dB) | Status |
|---|---|---|---|---|---|
| **Classic** | 12 | -1.70 | -1.20 | 0.50 | PASS (< 1.0 dB) |
| **Antique** | 9 | -1.80 | -1.20 | 0.60 | PASS (< 1.0 dB) |
| **Soft** | 9 | -1.90 | -1.20 | 0.70 | PASS (< 1.0 dB) |
| **Keyboard** | 9 | -1.90 | -1.00 | 0.90 | PASS (< 1.0 dB) |
| **All Combined** | 39 | -1.90 | -1.00 | 0.90 | PASS (< 1.0 dB) |
