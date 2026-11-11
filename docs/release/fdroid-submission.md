# F-Droid Submission Draft: Enk

This draft metadata recipe prepares Enk for inclusion in the official `fdroiddata` repository.

## Anti-Features Evaluation
Enk declares **zero anti-features**:
- **No Ads**: Completely ad-free.
- **No Tracking**: Zero analytics, zero telemetry, zero crash reporters.
- **No Non-Free Network Services**: Zero network permissions declared in AndroidManifest (`scripts/check-no-network.sh` passes).
- **No Non-Free Dependencies**: All dependencies are verified free software under Apache-2.0, SIL OFL 1.1, or CC0 (`scripts/check-fdroid-rules.sh` passes).
- **No Known Vulnerabilities / Tracking SDKs**: No Google Play Services, Firebase, or proprietary SDKs included.

---

## Metadata Draft (`metadata/app.enk.notes.yml`)

```yaml
Categories:
  - Writing
  - Office
License: Apache-2.0
AuthorName: Enk Authors
SourceCode: https://github.com/AhmedKaram50/enk
IssueTracker: https://github.com/AhmedKaram50/enk/issues
Changelog: https://github.com/AhmedKaram50/enk/releases

AutoName: Enk
Summary: A private, offline typewriter notebook for focused writing.

Description: |-
  Enk is an offline, private typewriter notebook for Android designed for focused,
  deliberate writing.

  Features:
  * Mechanical typewriter writing experience with tactile acoustic feedback, moving carriage rail, margin return bell, and night lamp.
  * Authentic struck-through revision model: backspace strikes through characters without erasing your thinking process, or clean-erase when preferred.
  * Physical note organization: file notes into Dividers in the Box, search by text match, and review trashed pages.
  * Completely offline: zero internet permission declared in the manifest, zero background tracking, and zero analytics.
  * Clean export and import: backup your notes as standard ZIP archives at any time.

RepoType: git
Repo: https://github.com/AhmedKaram50/enk.git

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0
    subdir: app
    gradle:
      - yes
    output: build/outputs/apk/release/app-release-unsigned.apk
    prebuild: sed -i -e '/dependenciesInfo/,/}/d' build.gradle.kts
    build:
      - $$GRADLE$$ :app:assembleRelease

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
CurrentVersion: 1.0.0
CurrentVersionCode: 1
```

---

## Reproducibility Verification

The release APK is 100% reproducible:
- Build 1 SHA-256: `92a9bec89f6865a70783f4779729866c5796833fac13a643109350b11a42bd2d`
- Build 2 SHA-256: `92a9bec89f6865a70783f4779729866c5796833fac13a643109350b11a42bd2d`
- Status: **Match (Bit-for-bit identical)**
