# Batch 11 device walk: toolchain upgrade

Goal: the app on the new toolchain (AGP 9.5.1, compileSdk 37, Compose 1.12, Backdrop 2.0.1) looks and behaves exactly like `main` at `639064f`. Nothing in this batch changes a screen. Run it on the Android 17 emulator (`Enk_API_37`); on a phone if one is at hand.

Run it twice, on two installs: the **debug** build and the **release** build (R8). The release build is the one that can crash at launch.

## Install

```bash
# debug
./gradlew :app:installDebug

# release: sign a copy with the debug key, outside the repo
BT=$(ls -d "$LOCALAPPDATA"/Android/Sdk/build-tools/* | sort -V | tail -1)
"$BT/zipalign.exe" -f -p 4 app/build/outputs/apk/release/app-release-unsigned.apk /tmp/rel-aligned.apk
"$BT/apksigner.bat" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android --key-pass pass:android --out /tmp/rel-signed.apk /tmp/rel-aligned.apk
adb uninstall app.enk.notes      # a debug install and a release install cannot replace each other
adb install /tmp/rel-signed.apk
```

Reset the phone's theme first: `adb shell cmd uimode night no`.

## Walk (each line: what to do, what you must see)

1. **Launch.** Start animation plays, then the writer: dark top bar with "enk_", the page counter, the Box button at the right, cream paper, rail with the carriage at the left. No crash, no blank screen.
2. **Type** "hello" and a space. Letters appear on the paper and the carriage moves. Sound plays (not on the emulator: it runs with `-no-audio`).
3. **Box button** (top right, tray icon). The Box opens: title "The Box", the note count, the divider strip, "New note", your note, "Hold a note for options", Trash. The glass nav bar sits at the bottom with Box selected.
4. **Hold the note** for about one second. The context menu opens with "Move to divider" and "Move to trash". Tap outside to close it.
5. **Find.** Tap Find in the nav bar. Title "Find", the search field, "Type to search your notes." Type a letter of your note: the note appears as a result.
6. **Setup.** Tap Setup. Appearance tiles, Light/Dark/System, **Paper in dark mode** (Ask, Normal, Lamp on), Writing, Sound (with "Keyboard click"), Display, Data. Change the theme to Apricot, then back to Graphite: colours change on every screen, with no crash.
7. **Dark mode.** `adb shell cmd uimode night yes`, then open the writer. The pill "Your phone is in dark mode. Start with the lamp on?" appears with two buttons. Tap "Lamp on": the lamp lights and the lamp control panel appears under the rail. Set `uimode night no`: the lamp turns off by itself.
8. **Lamp panel.** Hold the marker (the carriage) about half a second to toggle the lamp. Drag a fader: the beam changes under it.
9. **Trash and Credits.** Move a note to Trash from the context menu, open Trash, restore it. Open Setup, then Credits and licenses at the bottom: the list shows names and licences, not file paths.
10. **Export and import** (Setup, Data). Export creates a zip through the system picker; import reads one back.
11. **Widget.** Add the Enk widget on the home screen: it shows the latest note, and "New note" opens the writer. (Emulator launcher only; a phone launcher is the real check.)

## Compare against `main`

Install `main` (`git worktree add ../enk-main main`) beside this build, take a screenshot of the same screen on each (`adb exec-out screencap -p > shot.png`), and compare. Look at:

- the glass nav bar (blur, tint, the white lens under the selected tab)
- the context menu and the bottom sheets (New divider, Move to divider)
- the Setup rows and the Box cards
- the lamp beam

A difference you cannot explain is a finding. The emulator draws in software, so refraction cannot be judged there; that check needs a phone.

## Release-only checks

- `adb logcat -d | grep -E "FATAL|ClassNotFound|NoSuchMethod"` returns nothing after the whole walk.
- Rotate the phone in the Box and in the writer: no crash, text stays.

## Not covered by this walk

Real glass refraction on a phone, the widget on a real launcher, TalkBack, a build on a second machine.
