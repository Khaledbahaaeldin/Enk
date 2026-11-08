#!/usr/bin/env python3
"""Cuts the Enk sound sets from the archived CC0 originals (see tools/CutSamples.md).

Needs Python 3, numpy and ffmpeg (on PATH, or set the FFMPEG environment variable).
Run from anywhere:  python tools/cut_samples.py

For every sample the script
  1. decodes the archived original to mono float,
  2. finds the real attack near the approximate time in the table below (the first sample within 12 dB
     of the local peak) and starts the cut 2 ms before it,
  3. fades the end, and
  4. scales all samples to one loudness: RMS over the first 100 ms of the file, one common target for
     every sample of every set, limited so the peak stays at -1.5 dBFS.
Then it encodes Ogg Vorbis mono 44.1 kHz, re-measures the decoded files and fails if a rule is broken.
"""
import csv
import json
import os
import shutil
import subprocess
import sys
import tempfile
import wave

import numpy as np

SR = 44100
PEAK_TARGET_DB = -1.5
LOUD_MS = 100
ATTACK_DB = 12.0   # attack = first sample within this many dB of the local peak
LEAD_MS = 2.0      # cut starts this long before the attack
FADE_IN_MS = 1.0

FFMPEG = os.environ.get("FFMPEG") or shutil.which("ffmpeg") or "ffmpeg"
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets", "sounds")
ARCHIVE = "docs/archive"

SRC = {
    "classic": ("classic-keithpeter-123344/123344__keithpeter__typewriter-olivetti-lettra-22.wav",
                "https://freesound.org/people/keithpeter/sounds/123344/", "keithpeter"),
    "antique": ("antique-monotraum-434027/434027__monotraum__old-typewriter-working.wav",
                "https://freesound.org/people/monotraum/sounds/434027/", "monotraum"),
    "soft": ("soft-blukotek-246057/246057__blukotek__typewriter.wav",
             "https://freesound.org/people/blukotek/sounds/246057/", "blukotek"),
    "bell": ("bell-knufds-345955/345955__knufds__typewriter-bell-carriage-reset.wav",
             "https://freesound.org/people/knufds/sounds/345955/", "knufds"),
    "bell2": ("bell-ramsamba-318687/318687__ramsamba__typewriter-bell.wav",
              "https://freesound.org/people/ramsamba/sounds/318687/", "ramsamba"),
    "return": ("bell-ramsamba-318686/318686__ramsamba__typewriter-carriage-return.wav",
               "https://freesound.org/people/ramsamba/sounds/318686/", "ramsamba"),
}
OGA = "keyboard-opengameart-soundpack-1/unicae_games_keyboard_soundpack_1/Single Keys/keypress-%03d.wav"
OGA_URL = "https://opengameart.org/content/keyboard-soundpack-1-typing-and-single-keystrokes"

# (source key or OGA keypress number, approximate attack time in s, length in s, end fade in ms, filter)
# Times come from the onset table printed by the analysis (see CutSamples.md). Roles are assigned by
# measurement (see CutSamples.md), not by listening.
SOFT_LP = "lowpass=f=4500"
SETS = {
    "classic": {
        "keys": [("classic", t, 0.20, 8, None) for t in (13.174, 14.450, 17.544, 20.190, 22.402, 24.476, 28.682, 37.812)],
        "space": ("classic", 31.984, 0.30, 10, None),
        "enter": ("return", 0.592, 0.45, 12, None),
        "backspace": ("classic", 39.288, 0.20, 8, None),
        "bell": ("bell", 0.0, 1.20, 30, None),
    },
    "antique": {
        "keys": [("antique", t, 0.20, 8, None) for t in (3.556, 6.280, 6.854, 10.864, 12.252)],
        "space": ("antique", 22.824, 0.30, 10, None),
        "enter": ("return", 0.592, 0.45, 12, None),
        "backspace": ("antique", 12.056, 0.19, 10, None),
        "bell": ("bell", 0.0, 1.20, 30, None),
    },
    "soft": {
        "keys": [("soft", t, 0.20, 8, SOFT_LP) for t in (4.064, 4.576, 5.182, 5.900, 7.302)],
        "space": ("soft", 6.738, 0.19, 12, SOFT_LP),
        "enter": ("return", 0.592, 0.45, 12, SOFT_LP),
        "backspace": ("soft", 7.588, 0.20, 8, SOFT_LP),
        "bell": ("bell2", 0.0, 0.88, 30, None),
    },
    "keyboard": {
        "keys": [(n, 0.0, 0.20, 8, None) for n in (1, 4, 6, 13, 19)],
        "space": (3, 0.0, 0.25, 10, None),
        "enter": (17, 0.0, 0.25, 10, None),
        "backspace": (23, 0.0, 0.20, 8, None),
        "bell": ("bell", 0.0, 1.20, 30, None),
    },
}
NAMES = {"classic": "Classic", "antique": "Antique", "soft": "Soft", "keyboard": "Keyboard"}


def run(cmd):
    r = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if r.returncode != 0:
        sys.exit("command failed: %s\n%s" % (" ".join(cmd), r.stderr.decode("utf-8", "replace")))
    return r.stdout


def decode(path, af=None):
    cmd = [FFMPEG, "-v", "error", "-i", path]
    if af:
        cmd += ["-af", af]
    cmd += ["-ac", "1", "-ar", str(SR), "-f", "f32le", "-"]
    return np.frombuffer(run(cmd), dtype=np.float32).astype(np.float64)


def db(v):
    return 20.0 * np.log10(max(float(v), 1e-9))


def find_attack(x, t, whole_file):
    if whole_file:
        a, b = 0, len(x)
    else:
        a, b = max(0, int((t - 0.02) * SR)), min(len(x), int((t + 0.06) * SR))
    peak = np.abs(x[a:b]).max()
    idx = np.nonzero(np.abs(x[a:b]) >= peak * 10 ** (-ATTACK_DB / 20))[0][0]
    return a + int(idx)


def cut(x, t, length, fade_ms, whole_file):
    start = max(0, find_attack(x, t, whole_file) - int(LEAD_MS / 1000 * SR))
    y = x[start:start + int(length * SR)].copy()
    n_in = int(FADE_IN_MS / 1000 * SR)
    y[:n_in] *= np.linspace(0, 1, n_in)
    n_out = int(fade_ms / 1000 * SR)
    y[-n_out:] *= np.linspace(1, 0, n_out)
    return y


def loudness_db(y):
    seg = y[: int(LOUD_MS / 1000 * SR)]
    seg = np.concatenate([seg, np.zeros(max(0, int(LOUD_MS / 1000 * SR) - len(seg)))])
    return db(np.sqrt((seg ** 2).mean()))


def attack_ms(y):
    head = np.abs(y[: int(0.06 * SR)])  # the first strike: peak within the first 60 ms
    return float(np.nonzero(head >= head.max() * 10 ** (-ATTACK_DB / 20))[0][0]) / SR * 1000


def write_wav(path, y):
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((np.clip(y, -1, 1) * 32767).astype("<i2").tobytes())


def main():
    cache = {}

    def source(key, af):
        k = (key, af)
        if k not in cache:
            path = os.path.join(ROOT, ARCHIVE, OGA % key if isinstance(key, int) else SRC[key][0])
            cache[k] = decode(path, af)
        return cache[k]

    items = []  # (set, name, file, source key, samples)
    for sid, spec in SETS.items():
        order = [("key_%02d" % (i + 1), c) for i, c in enumerate(spec["keys"])]
        order += [(n, spec[n]) for n in ("space", "enter", "backspace", "bell")]
        for name, (key, t, length, fade, af) in order:
            y = cut(source(key, af), t, length, fade, isinstance(key, int))
            items.append((sid, name, key, y))

    # One common loudness target: the loudest level every sample can reach with its peak at -1.5 dBFS.
    reach = [loudness_db(y) + (PEAK_TARGET_DB - db(np.abs(y).max())) for _, _, _, y in items]
    target = min(reach)
    print("common loudness target: %.2f dBFS RMS over the first %d ms" % (target, LOUD_MS))

    tmp = tempfile.mkdtemp()
    rows, report = [], []
    try:
        for (sid, name, key, y), _ in zip(items, reach):
            wav = os.path.join(tmp, "s.wav")
            out = os.path.join(ASSETS, sid, name + ".ogg")
            os.makedirs(os.path.dirname(out), exist_ok=True)
            gain = target - loudness_db(y)
            best = None
            for _ in range(10):  # Vorbis changes the level a little: correct until the decoded file is on target
                write_wav(wav, y * 10 ** (gain / 20))
                run([FFMPEG, "-y", "-v", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", "-ac", "1", "-ar", str(SR), out])
                back = decode(out)
                err = target - loudness_db(back)
                if os.environ.get('CUT_DEBUG'): print(sid, name, 'gain %.2f err %.2f peak %.2f' % (gain, err, db(np.abs(back).max())))
                if db(np.abs(back).max()) < -0.5 and (best is None or abs(err) < best[0]):
                    best = (abs(err), gain, open(out, "rb").read())
                if abs(err) < 0.15:
                    break
                gain += 0.5 * err
            if best is None:
                sys.exit("%s/%s: cannot reach the loudness target without clipping" % (sid, name))
            with open(out, "wb") as f:
                f.write(best[2])
            back = decode(out)
            report.append((sid, name, loudness_db(back), db(np.abs(back).max()), attack_ms(back)))
            if isinstance(key, int):
                url, author = OGA_URL, "unicaegames"
                arch = "%s/%s" % (ARCHIVE, OGA % key)
            else:
                url, author = SRC[key][1], SRC[key][2]
                arch = "%s/%s" % (ARCHIVE, SRC[key][0])
            rows.append(["sounds/%s/%s.ogg" % (sid, name), url, author, "CC0", "2026-11-08", arch])
    finally:
        shutil.rmtree(tmp, ignore_errors=True)

    print("%-10s %-10s %9s %9s %9s" % ("set", "sample", "loud dB", "peak dB", "attack ms"))
    for r in report:
        print("%-10s %-10s %9.2f %9.2f %9.2f" % r)
    louds = [r[2] for r in report]
    spread = max(louds) - min(louds)
    print("loudness spread over all %d samples: %.2f dB" % (len(report), spread))
    problems = []
    if spread > 1.0:
        problems.append("loudness spread %.2f dB > 1 dB" % spread)
    for sid, name, lo, pk, at in report:
        if at > 5.0:
            problems.append("%s/%s attack at %.1f ms > 5 ms" % (sid, name, at))
        if pk > -0.3:
            problems.append("%s/%s peak %.2f dBFS" % (sid, name, pk))
    if problems:
        sys.exit("FAILED:\n" + "\n".join(problems))

    with open(os.path.join(ROOT, "docs", "provenance.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["file", "source_url", "author", "license", "retrieved", "archived_copy"])
        w.writerows(rows)

    manifest = {"sets": []}
    for sid, spec in SETS.items():
        def s(name):
            return {"file": "sounds/%s/%s.ogg" % (sid, name), "gain": 1.0}
        manifest["sets"].append({
            "id": sid, "name": NAMES[sid],
            "keys": [s("key_%02d" % (i + 1)) for i in range(len(spec["keys"]))],
            "space": s("space"), "enter": s("enter"), "backspace": s("backspace"), "bell": s("bell"),
        })
    with open(os.path.join(ASSETS, "manifest.json"), "w", encoding="utf-8", newline="") as f:
        json.dump(manifest, f, indent=2)
        f.write("\n")
    print("OK")


if __name__ == "__main__":
    main()
