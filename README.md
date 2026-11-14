# Enk

Enk is a private, offline typewriter notebook for Android: an authentic mechanical writing experience (rail, carriage, type-slug, stamp, night lamp with control panel) paired with a modern glass-shell interface (Box with dividers, search, Setup, trash, home-screen widget), five interface themes plus Android dynamic color, archive export/import, and multiple acoustic typewriter sound sets.

## Architecture

Enk is architected into modular components:
- `:text`: Pure Kotlin/JVM grid layout, pagination, and struck-character revision model.
- `:design`: Design tokens, materials, grain, shell themes, glass shaders, and physics motion.
- `:audio`: Sound sets, audio manifests, synthesized key clicks, and acoustic playback.
- `:data`: Room database (FTS4), DataStore preferences, and repository layer.
- `:app`: Android application container, Jetpack Compose UI screens, view models, and Glance widget.

## Building and Verification

### Prerequisites
- **JDK 21** (`JAVA_HOME` pointing to a JDK 21 installation such as Eclipse Adoptium 21). Note: Backdrop and Android shaders utilize Java 21 bytecode.
- Android SDK with platform API 37 installed (`minSdk = 31`, `targetSdk = 35`, `compileSdk = 37`).

### Verification Gate
Enk enforces a strict verification gate on every change:
```bash
# Full verification gate
./gradlew clean build test :design:verifyRoborazziDebug :app:verifyRoborazziDebug \
  :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata \
  :app:assembleRelease :app:compileDebugAndroidTestKotlin

# Verify network permissions and F-Droid compliance
bash scripts/check-no-network.sh
bash scripts/check-fdroid-rules.sh
```

On Windows (PowerShell / Git Bash):
```powershell
$env:JAVA_HOME = "D:\jdks\eclipse_adoptium-21-amd64-windows.2"
.\gradlew.bat clean build test :design:verifyRoborazziDebug :app:verifyRoborazziDebug :app:processDebugManifest :app:checkDebugAarMetadata :app:checkReleaseAarMetadata :app:assembleRelease :app:compileDebugAndroidTestKotlin
& "C:\Program Files\Git\bin\bash.exe" scripts/check-no-network.sh
& "C:\Program Files\Git\bin\bash.exe" scripts/check-fdroid-rules.sh
```

## Running on a Device

To install the debug build on a connected device or emulator:
```bash
./gradlew :app:installDebug
```

To sideload the optimized release APK (`app-release-unsigned.apk`), refer to [docs/qa/release-smoke.md](docs/qa/release-smoke.md).

## Privacy Promise

Enk is built strictly offline:
- Zero network permissions: `android.permission.INTERNET`, `ACCESS_NETWORK_STATE`, and `ACCESS_WIFI_STATE` are stripped and forbidden.
- Zero analytics, telemetry, crash reporting libraries, or third-party tracking SDKs.
- Notes are stored solely on your device, in the app's private storage (a SQLite database). They are not encrypted by Enk.
- Data export and import utilize Android's Storage Access Framework without requiring external storage permissions.

## Documentation & Contributing

- Contributing guidelines: [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)
- Adding a sound set: [docs/adding-a-sound-set.md](docs/adding-a-sound-set.md)
- Release process: [docs/release/RELEASING.md](docs/release/RELEASING.md)
- F-Droid submission metadata: [docs/release/fdroid-submission.md](docs/release/fdroid-submission.md)
- Product & Design Specifications: [docs/superpowers/specs/](docs/superpowers/specs/)

## Licensing & Credits

Enk is free and open-source software licensed under the [Apache License, Version 2.0](LICENSE).
- Typewriter acoustic samples are CC0 Public Domain Dedication, archived in `docs/provenance.csv`.
- Fonts: Courier Prime and Inter are licensed under the SIL Open Font License 1.1 (`app/src/main/assets/licenses/`).
- Complete library attributions and sound provenance are available within the app under Setup > About > Credits and licenses, and tracked in `docs/libraries.csv` and `docs/provenance.csv`.
