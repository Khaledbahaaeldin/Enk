# F-Droid Submission Draft: Enk

This draft metadata recipe prepares Enk for inclusion in the official `fdroiddata` repository.

## Anti-Features Evaluation
Enk declares **zero anti-features**:
- **No Ads**: Completely ad-free.
- **No Tracking**: Zero analytics, zero telemetry, zero crash reporters.
- **No Non-Free Network Services**: Zero network permissions declared in AndroidManifest (`scripts/check-no-network.sh` passes).
- **No Non-Free Dependencies**: All dependencies are verified free software under Apache-2.0, SIL OFL 1.1, or CC0 (`scripts/check-fdroid-rules.sh` passes).
- **No tracking or proprietary SDKs**: no Google Play Services, Firebase or other proprietary SDKs; `scripts/check-fdroid-rules.sh` rule (a) fails on them. This is not a vulnerability audit.

---

## Metadata Draft (`metadata/app.enk.notes.yml`)

```yaml
Categories:
  - Writing
  - Office
License: Apache-2.0
AuthorName: Enk Authors
SourceCode: https://github.com/Khaledbahaaeldin/Enk
IssueTracker: https://github.com/Khaledbahaaeldin/Enk/issues
Changelog: https://github.com/Khaledbahaaeldin/Enk/releases

AutoName: Enk
Summary: A private, offline typewriter notebook for focused writing.

Description: |-
  Enk is an offline, private typewriter notebook for Android designed for focused,
  deliberate writing.

  Features:
  * Mechanical typewriter writing experience with tactile acoustic feedback, moving carriage rail, margin return bell, and night lamp.
  * Two ways to correct yourself: clean erase (the default) or strike-through, which keeps corrections on the page.
  * Physical note organization: file notes into Dividers in the Box, search by text match, and review trashed pages.
  * Completely offline: zero internet permission declared in the manifest, zero background tracking, and zero analytics.
  * Clean export and import: backup your notes as standard ZIP archives at any time.

RepoType: git
Repo: https://github.com/Khaledbahaaeldin/Enk.git

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0
    subdir: app
    gradle:
      - yes
    output: build/outputs/apk/release/app-release-unsigned.apk
    build:
      - $$GRADLE$$ :app:assembleRelease

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
CurrentVersion: 1.0.0
CurrentVersionCode: 1
```

---

## Reproducibility

Two clean builds of the same commit on the same machine and JDK produced byte-identical unsigned release APKs
(checked at audit time; the hashes are recorded in `docs/audit/batch-10-handoff.md`, and the method is in
`docs/release/RELEASING.md`). That has not been checked across different machines or JDK builds, so do not
promise F-Droid's own build will match until it has been compared. Applications that want F-Droid to publish
the upstream signature need a `Binaries:` entry and a matching reproducible build; this draft does not claim it.
