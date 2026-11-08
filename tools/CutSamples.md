# Enk sound sets: cutting and levelling (`tools/CutSamples.md`)

Everything in `app/src/main/assets/sounds/` is produced by one repeatable script:

```
python tools/cut_samples.py      # needs Python 3, numpy and ffmpeg (on PATH, or set FFMPEG)
```

The script reads the archived originals in `docs/archive/`, writes the Ogg files, `manifest.json` and
`docs/provenance.csv`, and exits with an error if a rule below is broken. No machine paths are stored.

## 1. Sources (all CC0, retrieved 2026-11-08)

| Used for | Source URL | Author | Archived copy |
|---|---|---|---|
| Classic set | https://freesound.org/people/keithpeter/sounds/123344/ | keithpeter | `docs/archive/classic-keithpeter-123344/` (9.4 MB) |
| Antique set | https://freesound.org/people/monotraum/sounds/434027/ | monotraum | `docs/archive/antique-monotraum-434027/` (lossless working copy, see 1a) |
| Soft set | https://freesound.org/people/blukotek/sounds/246057/ | blukotek | `docs/archive/soft-blukotek-246057/` |
| Keyboard set | https://opengameart.org/content/keyboard-soundpack-1-typing-and-single-keystrokes | unicaegames | `docs/archive/keyboard-opengameart-soundpack-1/` (Single Keys only, see 1b) |
| Bell (Classic, Antique, Keyboard) | https://freesound.org/people/knufds/sounds/345955/ | knufds | `docs/archive/bell-knufds-345955/` |
| Bell (Soft) | https://freesound.org/people/ramsamba/sounds/318687/ | ramsamba | `docs/archive/bell-ramsamba-318687/` |
| Enter of the three typewriter sets | https://freesound.org/people/ramsamba/sounds/318686/ | ramsamba | `docs/archive/bell-ramsamba-318686/` |

**1a. Antique original.** `434027__monotraum__old-typewriter.wav` (11,760,044 bytes) is over the 10 MB limit,
so it is not in git. `docs/archive/antique-monotraum-434027/434027__monotraum__old-typewriter-working.wav`
is a lossless cut of its first 25.08 s (PCM 24-bit stereo 44.1 kHz, 6,635,596 bytes); every Antique sample
is cut from it. Full original SHA-256: `A692295D3262435C681ACD5E61ED89721DF3F73CE0009526CCA088C33F4B04CA`.

**1b. OpenGameArt pack.** Only `Single Keys/` (32 files) and `readme.txt` are in git. The typing runs and
`preview.ogg` are not used and are kept out (git-ignored; the zip is 8,515,609 bytes, SHA-256
`935eae2fa5c3742eacdd38c4ea0e9047f3887faa0701996498492c256db1b351`).

## 2. What the script does to every sample

1. Decodes the archived file to mono (Soft: 4.5 kHz low-pass first; the bells and the Enter of the other sets are not filtered).
2. Finds the real attack near the approximate time in the script's table: the first sample within 12 dB of the local peak. The cut starts 2 ms before it, with a 1 ms fade-in.
3. Cuts a fixed length (keys 0.19 to 0.20 s, space 0.19 to 0.30 s, Enter 0.25 to 0.45 s, bells 0.88 or 1.2 s) and fades the end (8 to 30 ms).
4. **Levels** all 39 samples to one loudness (section 3).
5. Encodes Ogg Vorbis, mono, 44.1 kHz, quality 4, decodes it again, and corrects the gain until the decoded file is on target.
6. Fails the run if a decoded sample has its attack later than 5 ms (measured against the first strike, peak of the first 60 ms), a peak above -0.3 dBFS, or if the loudness spread is over 1 dB.

The attack times are in the table below (column "attack ms", 0.2 to 2.5 ms). The earlier cut had Space, Backspace and Enter
starting 180 to 640 ms before the strike; this version fixes that.

## 3. Loudness

Loudness is the RMS level of the first 100 ms of the file. Every sample of every set is scaled to the same
target, **-28.43 dBFS**, which is the highest level every sample can reach with its peak below -1.5 dBFS.
Result after encoding: spread **0.59 dB** over all 39 samples (table below). Method note: for very short
sounds "loudness" depends on the window; measured over 20 ms or 150 ms the same files differ by several dB
because their decays differ. The 100 ms window was chosen because the ear integrates over roughly that time.
Peaks therefore differ (-1.2 to -15 dBFS) while the 100 ms energy is equal. The player multiplies by volume and
gain with a cap of 1.0, so these files play at their stored level; the level can be raised later with a master
gain if a device test shows them too quiet.

| Set | Sample | Loudness dBFS (100 ms RMS) | Peak dBFS | Attack ms |
|---|---|---|---|---|
| classic | key_01 | -28.34 | -3.63 | 2.00 |
| classic | key_02 | -28.55 | -3.07 | 2.06 |
| classic | key_03 | -28.49 | -8.95 | 0.54 |
| classic | key_04 | -28.53 | -12.86 | 0.27 |
| classic | key_05 | -28.56 | -11.92 | 0.61 |
| classic | key_06 | -28.53 | -8.26 | 0.52 |
| classic | key_07 | -28.53 | -10.97 | 0.32 |
| classic | key_08 | -28.55 | -8.41 | 0.36 |
| classic | space | -28.54 | -11.07 | 0.36 |
| classic | enter | -28.49 | -13.26 | 2.00 |
| classic | backspace | -28.52 | -10.13 | 2.00 |
| classic | bell | -28.51 | -11.62 | 2.00 |
| antique | key_01 | -28.50 | -5.22 | 2.00 |
| antique | key_02 | -28.10 | -6.58 | 2.00 |
| antique | key_03 | -28.69 | -7.74 | 1.34 |
| antique | key_04 | -28.49 | -7.26 | 2.00 |
| antique | key_05 | -28.47 | -6.23 | 2.02 |
| antique | space | -28.53 | -14.53 | 0.20 |
| antique | enter | -28.49 | -13.26 | 2.00 |
| antique | backspace | -28.57 | -6.24 | 1.88 |
| antique | bell | -28.51 | -11.62 | 2.00 |
| soft | key_01 | -28.54 | -1.58 | 2.00 |
| soft | key_02 | -28.58 | -1.24 | 2.00 |
| soft | key_03 | -28.64 | -2.98 | 2.00 |
| soft | key_04 | -28.53 | -2.77 | 2.00 |
| soft | key_05 | -28.32 | -1.49 | 2.00 |
| soft | space | -28.62 | -1.84 | 2.00 |
| soft | enter | -28.45 | -14.72 | 2.00 |
| soft | backspace | -28.37 | -1.99 | 1.97 |
| soft | bell | -28.52 | -15.15 | 2.00 |
| keyboard | key_01 | -28.34 | -4.30 | 2.02 |
| keyboard | key_02 | -28.34 | -6.59 | 2.49 |
| keyboard | key_03 | -28.53 | -8.66 | 2.00 |
| keyboard | key_04 | -28.49 | -6.66 | 2.00 |
| keyboard | key_05 | -28.51 | -6.36 | 2.00 |
| keyboard | space | -28.52 | -10.56 | 2.00 |
| keyboard | enter | -28.56 | -6.31 | 1.84 |
| keyboard | backspace | -28.51 | -8.91 | 2.00 |
| keyboard | bell | -28.51 | -11.62 | 2.00 |

## 4. Which moment is which key

Roles were assigned from measurements (onset table, spectral centroid, decay), **not by listening**. Please audition each
set and tell me which samples to swap.

- **Classic / Antique / Soft keys:** loud, isolated strikes (peak within 7.5 dB of full scale in the source, at least 0.2 s before the next strike where possible, mid-range centroid). The first run also used quiet key-release clicks as keys; those are gone.
- **Space:** the strike with the lowest spectral centroid / most low-frequency energy (Classic 31.98 s, Antique 22.82 s, Soft 6.74 s).
- **Backspace:** a lighter, brighter single strike (Classic 39.29 s, Antique 12.06 s, Soft 7.59 s).
- **Enter (Classic, Antique, Soft):** the end-stop thunk and rattle of the carriage return from ramsamba #318686 (0.59 s into the file), shared by the three typewriter sets; Soft applies its low-pass. It is not synthesised.
- **Keyboard set:** the pack's 32 single keypresses are all 0.250 s long, so none is "longer". Keys are keypress 001, 004, 006, 013, 019 (strong peak, mid centroid). Space is keypress 003 (lowest centroid, longest decay), Enter is 017 (strongest peak), Backspace is 023 (lightest, brightest). Nothing is synthesised, but these three are ordinary keypresses: they differ from the keys in weight and brightness only, because the pack has no separate Space, Enter or Backspace sound.
- **Bells:** knufds #345955, first 1.2 s (Classic, Antique, Keyboard); ramsamba #318687 (Soft). The alternate return #318686 is also used for Enter, as above.
