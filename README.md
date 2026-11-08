# Enk

Enk is a free, open-source Android typewriter-style notes app: an authentic vintage writing screen (rail, carriage, type-slug, stamp, night lamp with control panel) married with a modern glass-shell interface (Box with dividers, search, Setup, trash, widget), five interface themes plus Android dynamic color, export/import, and multiple acoustic typewriter sound sets.

## Architecture

Enk is architected into modular components:
- `:text`: Pure Kotlin/JVM grid layout, pagination, and strike model.
- `:design`: Design tokens, materials, shadows, grain, shell themes, glass shaders, and physics motion.
- `:audio`: Sound sets, manifests, and audio playback.
- `:data`: Room database (FTS4), DataStore persistence, and repository layer.
- `:app`: Android application container, Compose UI screens, view models, and home screen widget.

## Building and Testing

Prerequisites:
- JDK 17
- Android SDK with platform API 35 (or 36) installed (`minSdk = 31`)

Build commands:
```bash
# Build all modules and generate debug artifacts
./gradlew build

# Run unit tests across all modules
./gradlew test
```

On Windows:
```powershell
.\gradlew.bat build
.\gradlew.bat test
```

## Privacy & Network Compliance

Enk is built strictly offline: it does not request `android.permission.INTERNET` or any network state permissions. A build-time manifest verification test and CI script enforce that zero network permissions are declared or merged into the final application.

## Specifications

- Product & Writer Spec: [docs/superpowers/specs/2026-11-07-enk-design.md](docs/superpowers/specs/2026-11-07-enk-design.md)
- Design System: [docs/superpowers/specs/2026-11-07-enk-design-system.md](docs/superpowers/specs/2026-11-07-enk-design-system.md)
- Logo & Start Animation: [docs/superpowers/specs/2026-11-07-enk-logo.md](docs/superpowers/specs/2026-11-07-enk-logo.md) and [docs/superpowers/specs/2026-11-07-enk-start-animation.md](docs/superpowers/specs/2026-11-07-enk-start-animation.md)

## License

Enk is licensed under the [Apache License, Version 2.0](LICENSE).
