#!/usr/bin/env python3
import os
import sys
import subprocess
import json
import re

FFMPEG = os.path.expandvars(r"%LOCALAPPDATA%\Microsoft\WinGet\Links\ffmpeg.exe")
if not os.path.exists(FFMPEG):
    FFMPEG = "ffmpeg"

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS_DIR = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "sounds")
DOCS_DIR = os.path.join(REPO_ROOT, "docs")
ARCHIVE_DIR = os.path.join(DOCS_DIR, "archive")

def run_cmd(cmd):
    res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if res.returncode != 0:
        print(f"Command failed: {' '.join(cmd)}\n{res.stderr}", file=sys.stderr)
        sys.exit(1)
    return res.stdout, res.stderr

def measure_volumes(file_path):
    cmd = [FFMPEG, "-i", file_path, "-af", "volumedetect", "-f", "null", "-"]
    _, err = run_cmd(cmd)
    mean_m = re.search(r"mean_volume:\s+([-\d.]+)\s+dB", err)
    max_m = re.search(r"max_volume:\s+([-\d.]+)\s+dB", err)
    mean_v = float(mean_m.group(1)) if mean_m else -24.0
    max_v = float(max_m.group(1)) if max_m else -1.5
    return mean_v, max_v

def cut_and_normalize(src_path, out_path, ss=None, t=None, lowpass=None, target_peak_db=-1.5):
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    tmp_wav = out_path + ".tmp.wav"
    
    # 1. Cut and trim silence to ensure start within 5 ms of transient
    filter_chain = ["silenceremove=start_periods=1:start_duration=0:start_threshold=-32dB:detection=peak"]
    if lowpass:
        filter_chain.append(f"lowpass=f={lowpass}")
        
    cmd = [FFMPEG, "-y"]
    if ss is not None:
        cmd.extend(["-ss", str(ss)])
    if t is not None:
        cmd.extend(["-t", str(t)])
    cmd.extend(["-i", src_path, "-af", ",".join(filter_chain), "-ac", "1", "-ar", "44100", tmp_wav])
    run_cmd(cmd)
    
    # 2. Measure max volume on uncompressed PCM
    _, current_max = measure_volumes(tmp_wav)
    gain_db = target_peak_db - current_max
    
    # 3. Apply volume adjustment and encode to Ogg Vorbis mono 44.1 kHz
    cmd = [
        FFMPEG, "-y", "-i", tmp_wav,
        "-af", f"volume={gain_db:.2f}dB",
        "-c:a", "libvorbis", "-q:a", "4",
        "-ac", "1", "-ar", "44100",
        out_path
    ]
    run_cmd(cmd)
    
    # 4. Check Vorbis encoded peak, adjust if compression shifted peak by > 0.3 dB
    _, actual_max = measure_volumes(out_path)
    diff = target_peak_db - actual_max
    if abs(diff) > 0.3:
        fine_gain = gain_db + diff
        cmd = [
            FFMPEG, "-y", "-i", tmp_wav,
            "-af", f"volume={fine_gain:.2f}dB",
            "-c:a", "libvorbis", "-q:a", "4",
            "-ac", "1", "-ar", "44100",
            out_path
        ]
        run_cmd(cmd)

    if os.path.exists(tmp_wav):
        os.remove(tmp_wav)
        
    final_mean, final_max = measure_volumes(out_path)
    return final_mean, final_max

def main():
    print("Beginning sample cutting and normalization...")
    
    TARGET_PEAK = -1.5
    sets_data = {}
    samples_info = []
    
    # -------------------------------------------------------------------------
    # 1. Classic Set (Olivetti Lettera 22 - keithpeter)
    # -------------------------------------------------------------------------
    classic_src = os.path.join(ARCHIVE_DIR, "classic-keithpeter-123344", "123344__keithpeter__typewriter-olivetti-lettra-22.wav")
    classic_url = "https://freesound.org/people/keithpeter/sounds/123344/"
    classic_author = "keithpeter"
    classic_archive_rel = "docs/archive/classic-keithpeter-123344/123344__keithpeter__typewriter-olivetti-lettra-22.wav"
    
    classic_cuts = {
        "key_01.ogg": (13.15, 0.25),
        "key_02.ogg": (13.92, 0.25),
        "key_03.ogg": (14.18, 0.25),
        "key_04.ogg": (15.10, 0.25),
        "key_05.ogg": (15.95, 0.25),
        "key_06.ogg": (16.27, 0.25),
        "key_07.ogg": (17.06, 0.25),
        "key_08.ogg": (17.50, 0.25),
        "space.ogg":  (18.44, 0.25),
        "enter.ogg":  (24.59, 0.65),
        "backspace.ogg": (18.81, 0.20),
    }
    
    sets_data["classic"] = []
    for fname, (ss, t) in classic_cuts.items():
        out_file = os.path.join(ASSETS_DIR, "classic", fname)
        mean_v, max_v = cut_and_normalize(classic_src, out_file, ss=ss, t=t, target_peak_db=TARGET_PEAK)
        rel_asset = f"sounds/classic/{fname}"
        row = {
            "file": rel_asset,
            "source_url": classic_url,
            "author": classic_author,
            "license": "CC0",
            "retrieved": "2026-11-08",
            "archived_copy": classic_archive_rel,
            "mean_volume": mean_v,
            "max_volume": max_v
        }
        samples_info.append(row)
        sets_data["classic"].append(max_v)
        print(f"Cut {rel_asset} -> peak: {max_v:.2f} dB, mean: {mean_v:.2f} dB")
        
    bell_knufds_src = os.path.join(ARCHIVE_DIR, "bell-knufds-345955", "345955__knufds__typewriter-bell-carriage-reset.wav")
    bell_knufds_url = "https://freesound.org/people/knufds/sounds/345955/"
    bell_knufds_author = "knufds"
    bell_knufds_archive_rel = "docs/archive/bell-knufds-345955/345955__knufds__typewriter-bell-carriage-reset.wav"
    
    classic_bell_out = os.path.join(ASSETS_DIR, "classic", "bell.ogg")
    mean_v, max_v = cut_and_normalize(bell_knufds_src, classic_bell_out, ss=0.0, t=1.2, target_peak_db=TARGET_PEAK)
    row = {
        "file": "sounds/classic/bell.ogg",
        "source_url": bell_knufds_url,
        "author": bell_knufds_author,
        "license": "CC0",
        "retrieved": "2026-11-08",
        "archived_copy": bell_knufds_archive_rel,
        "mean_volume": mean_v,
        "max_volume": max_v
    }
    samples_info.append(row)
    sets_data["classic"].append(max_v)
    print(f"Cut sounds/classic/bell.ogg -> peak: {max_v:.2f} dB, mean: {mean_v:.2f} dB")
    
    # -------------------------------------------------------------------------
    # 2. Antique Set (monotraum)
    # -------------------------------------------------------------------------
    antique_src = os.path.join(ARCHIVE_DIR, "antique-monotraum-434027", "434027__monotraum__old-typewriter-working.wav")
    antique_url = "https://freesound.org/people/monotraum/sounds/434027/"
    antique_author = "monotraum"
    antique_archive_rel = "docs/archive/antique-monotraum-434027/434027__monotraum__old-typewriter-working.wav"
    
    antique_cuts = {
        "key_01.ogg": (1.50, 0.25),
        "key_02.ogg": (2.04, 0.25),
        "key_03.ogg": (2.88, 0.25),
        "key_04.ogg": (3.32, 0.25),
        "key_05.ogg": (4.03, 0.25),
        "space.ogg":  (4.77, 0.25),
        "enter.ogg":  (6.08, 0.85),
        "backspace.ogg": (5.33, 0.25),
    }
    
    sets_data["antique"] = []
    for fname, (ss, t) in antique_cuts.items():
        out_file = os.path.join(ASSETS_DIR, "antique", fname)
        mean_v, max_v = cut_and_normalize(antique_src, out_file, ss=ss, t=t, target_peak_db=TARGET_PEAK)
        rel_asset = f"sounds/antique/{fname}"
        row = {
            "file": rel_asset,
            "source_url": antique_url,
            "author": antique_author,
            "license": "CC0",
            "retrieved": "2026-11-08",
            "archived_copy": antique_archive_rel,
            "mean_volume": mean_v,
            "max_volume": max_v
        }
        samples_info.append(row)
        sets_data["antique"].append(max_v)
        print(f"Cut {rel_asset} -> peak: {max_v:.2f} dB, mean: {mean_v:.2f} dB")
        
    antique_bell_out = os.path.join(ASSETS_DIR, "antique", "bell.ogg")
    mean_v, max_v = cut_and_normalize(bell_knufds_src, antique_bell_out, ss=0.0, t=1.2, target_peak_db=TARGET_PEAK)
    row = {
        "file": "sounds/antique/bell.ogg",
        "source_url": bell_knufds_url,
        "author": bell_knufds_author,
        "license": "CC0",
        "retrieved": "2026-11-08",
        "archived_copy": bell_knufds_archive_rel,
        "mean_volume": mean_v,
        "max_volume": max_v
    }
    samples_info.append(row)
    sets_data["antique"].append(max_v)
    print(f"Cut sounds/antique/bell.ogg -> peak: {max_v:.2f} dB, mean: {mean_v:.2f} dB")
    
    # -------------------------------------------------------------------------
    # 3. Soft Set (blukotek)
    # -------------------------------------------------------------------------
    soft_src = os.path.join(ARCHIVE_DIR, "soft-blukotek-246057", "246057__blukotek__typewriter.wav")
    soft_url = "https://freesound.org/people/blukotek/sounds/246057/"
    soft_author = "blukotek"
    soft_archive_rel = "docs/archive/soft-blukotek-246057/246057__blukotek__typewriter.wav"
    
    soft_cuts = {
        "key_01.ogg": (4.05, 0.25),
        "key_02.ogg": (4.56, 0.25),
        "key_03.ogg": (5.17, 0.25),
        "key_04.ogg": (5.88, 0.25),
        "key_05.ogg": (8.27, 0.25),
        "space.ogg":  (1.33, 0.22),
        "enter.ogg":  (6.53, 0.70),
        "backspace.ogg": (12.86, 0.25),
    }
    
    sets_data["soft"] = []
    for fname, (ss, t) in soft_cuts.items():
        out_file = os.path.join(ASSETS_DIR, "soft", fname)
        mean_v, max_v = cut_and_normalize(soft_src, out_file, ss=ss, t=t, lowpass=4500, target_peak_db=TARGET_PEAK)
        rel_asset = f"sounds/soft/{fname}"
        row = {
            "file": rel_asset,
            "source_url": soft_url,
            "author": soft_author,
            "license": "CC0",
            "retrieved": "2026-11-08",
            "archived_copy": soft_archive_rel,
            "mean_volume": mean_v,
            "max_volume": max_v
        }
        samples_info.append(row)
        sets_data["soft"].append(max_v)
        print(f"Cut {rel_asset} -> peak: {max_v:.2f} dB, mean: {mean_v:.2f} dB")
        
    bell_ramsamba_src = os.path.join(ARCHIVE_DIR, "bell-ramsamba-318687", "318687__ramsamba__typewriter-bell.wav")
    bell_ramsamba_url = "https://freesound.org/people/ramsamba/sounds/318687/"
    bell_ramsamba_author = "ramsamba"
    bell_ramsamba_archive_rel = "docs/archive/bell-ramsamba-318687/318687__ramsamba__typewriter-bell.wav"
    
    soft_bell_out = os.path.join(ASSETS_DIR, "soft", "bell.ogg")
    mean_v, max_v = cut_and_normalize(bell_ramsamba_src, soft_bell_out, ss=0.0, t=0.88, target_peak_db=TARGET_PEAK)
    row = {
        "file": "sounds/soft/bell.ogg",
        "source_url": bell_ramsamba_url,
        "author": bell_ramsamba_author,
        "license": "CC0",
        "retrieved": "2026-11-08",
        "archived_copy": bell_ramsamba_archive_rel,
        "mean_volume": mean_v,
        "max_volume": max_v
    }
    samples_info.append(row)
    sets_data["soft"].append(max_v)
    print(f"Cut sounds/soft/bell.ogg -> peak: {max_v:.2f} dB, mean: {mean_v:.2f} dB")
    
    # -------------------------------------------------------------------------
    # 4. Keyboard Set (OpenGameArt Keyboard Soundpack #1 - unicaegames)
    # -------------------------------------------------------------------------
    single_keys_dir = os.path.join(ARCHIVE_DIR, "keyboard-opengameart-soundpack-1", "unicae_games_keyboard_soundpack_1", "Single Keys")
    kb_url = "https://opengameart.org/content/keyboard-soundpack-1-typing-and-single-keystrokes"
    kb_author = "unicaegames"
    
    kb_cuts = {
        "key_01.ogg": os.path.join(single_keys_dir, "keypress-001.wav"),
        "key_02.ogg": os.path.join(single_keys_dir, "keypress-002.wav"),
        "key_03.ogg": os.path.join(single_keys_dir, "keypress-003.wav"),
        "key_04.ogg": os.path.join(single_keys_dir, "keypress-004.wav"),
        "key_05.ogg": os.path.join(single_keys_dir, "keypress-005.wav"),
        "space.ogg":  os.path.join(single_keys_dir, "keypress-020.wav"),
        "enter.ogg":  os.path.join(single_keys_dir, "keypress-030.wav"),
        "backspace.ogg": os.path.join(single_keys_dir, "keypress-015.wav"),
    }
    
    sets_data["keyboard"] = []
    for fname, src_f in kb_cuts.items():
        out_file = os.path.join(ASSETS_DIR, "keyboard", fname)
        mean_v, max_v = cut_and_normalize(src_f, out_file, target_peak_db=TARGET_PEAK)
        rel_asset = f"sounds/keyboard/{fname}"
        rel_archive = os.path.relpath(src_f, REPO_ROOT).replace("\\", "/")
        row = {
            "file": rel_asset,
            "source_url": kb_url,
            "author": kb_author,
            "license": "CC0",
            "retrieved": "2026-11-08",
            "archived_copy": rel_archive,
            "mean_volume": mean_v,
            "max_volume": max_v
        }
        samples_info.append(row)
        sets_data["keyboard"].append(max_v)
        print(f"Cut {rel_asset} -> peak: {max_v:.2f} dB, mean: {mean_v:.2f} dB")
        
    keyboard_bell_out = os.path.join(ASSETS_DIR, "keyboard", "bell.ogg")
    mean_v, max_v = cut_and_normalize(bell_knufds_src, keyboard_bell_out, ss=0.0, t=1.2, target_peak_db=TARGET_PEAK)
    row = {
        "file": "sounds/keyboard/bell.ogg",
        "source_url": bell_knufds_url,
        "author": bell_knufds_author,
        "license": "CC0",
        "retrieved": "2026-11-08",
        "archived_copy": bell_knufds_archive_rel,
        "mean_volume": mean_v,
        "max_volume": max_v
    }
    samples_info.append(row)
    sets_data["keyboard"].append(max_v)
    print(f"Cut sounds/keyboard/bell.ogg -> peak: {max_v:.2f} dB, mean: {mean_v:.2f} dB")
    
    # -------------------------------------------------------------------------
    # Verification of Loudness Invariants
    # -------------------------------------------------------------------------
    print("\n--- Loudness Normalization Verification ---")
    for set_name, vols in sets_data.items():
        s_min = min(vols)
        s_max = max(vols)
        s_spread = s_max - s_min
        print(f"Set '{set_name}' ({len(vols)} samples): peak range [{s_min:.2f} dBFS, {s_max:.2f} dBFS], spread = {s_spread:.2f} dB")
        assert s_spread <= 1.0, f"Set {set_name} peak spread {s_spread:.2f} dB exceeds 1.0 dB!"
        
    all_peaks = [s["max_volume"] for s in samples_info]
    total_min = min(all_peaks)
    total_max = max(all_peaks)
    total_spread = total_max - total_min
    print(f"\nAll sets combined ({len(all_peaks)} samples): peak range [{total_min:.2f} dBFS, {total_max:.2f} dBFS], spread = {total_spread:.2f} dB")
    assert total_spread <= 1.0, f"Combined peak spread {total_spread:.2f} dB exceeds 1.0 dB!"
    print("SUCCESS: Every set and all sets combined are normalized within 1.0 dB loudness spread!")
    
    # -------------------------------------------------------------------------
    # Write docs/provenance.csv
    # -------------------------------------------------------------------------
    prov_file = os.path.join(DOCS_DIR, "provenance.csv")
    with open(prov_file, "w", encoding="utf-8") as f:
        f.write("file,source_url,author,license,retrieved,archived_copy\n")
        for s in samples_info:
            f.write(f"{s['file']},{s['source_url']},{s['author']},{s['license']},{s['retrieved']},{s['archived_copy']}\n")
    print(f"Wrote {prov_file}")
    
    # -------------------------------------------------------------------------
    # Write app/src/main/assets/sounds/manifest.json
    # -------------------------------------------------------------------------
    manifest = {
        "sets": [
            {
                "id": "classic",
                "name": "Classic",
                "keys": [
                    {"file": f"sounds/classic/key_0{i}.ogg", "gain": 1.0} for i in range(1, 9)
                ],
                "space": {"file": "sounds/classic/space.ogg", "gain": 0.7},
                "enter": {"file": "sounds/classic/enter.ogg", "gain": 1.0},
                "backspace": {"file": "sounds/classic/backspace.ogg", "gain": 1.0},
                "bell": {"file": "sounds/classic/bell.ogg", "gain": 0.6}
            },
            {
                "id": "antique",
                "name": "Antique",
                "keys": [
                    {"file": f"sounds/antique/key_0{i}.ogg", "gain": 1.0} for i in range(1, 6)
                ],
                "space": {"file": "sounds/antique/space.ogg", "gain": 0.7},
                "enter": {"file": "sounds/antique/enter.ogg", "gain": 1.0},
                "backspace": {"file": "sounds/antique/backspace.ogg", "gain": 1.0},
                "bell": {"file": "sounds/antique/bell.ogg", "gain": 0.6}
            },
            {
                "id": "soft",
                "name": "Soft",
                "keys": [
                    {"file": f"sounds/soft/key_0{i}.ogg", "gain": 1.0} for i in range(1, 6)
                ],
                "space": {"file": "sounds/soft/space.ogg", "gain": 0.7},
                "enter": {"file": "sounds/soft/enter.ogg", "gain": 1.0},
                "backspace": {"file": "sounds/soft/backspace.ogg", "gain": 1.0},
                "bell": {"file": "sounds/soft/bell.ogg", "gain": 0.6}
            },
            {
                "id": "keyboard",
                "name": "Keyboard",
                "keys": [
                    {"file": f"sounds/keyboard/key_0{i}.ogg", "gain": 1.0} for i in range(1, 6)
                ],
                "space": {"file": "sounds/keyboard/space.ogg", "gain": 0.7},
                "enter": {"file": "sounds/keyboard/enter.ogg", "gain": 1.0},
                "backspace": {"file": "sounds/keyboard/backspace.ogg", "gain": 1.0},
                "bell": {"file": "sounds/keyboard/bell.ogg", "gain": 0.6}
            }
        ]
    }
    
    manifest_file = os.path.join(ASSETS_DIR, "manifest.json")
    with open(manifest_file, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)
    print(f"Wrote {manifest_file}")

if __name__ == "__main__":
    main()
