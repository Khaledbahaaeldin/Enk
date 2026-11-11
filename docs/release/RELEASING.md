# Release Guide for Enk

This document outlines the release process, signing configuration, reproducibility verification, and distribution channels for Enk.

## Signing Configuration

Enk produces unsigned release binaries by default when no signing credentials are provided.

### Local Builds with a Private Keystore

To sign release builds locally, either:

1. Create a `keystore.properties` file in the project root (this file is ignored by git):
   ```properties
   storeFile=/path/to/release.keystore
   storePassword=your_keystore_password
   keyAlias=your_key_alias
   keyPassword=your_key_password
   ```

2. Or define the corresponding environment variables:
   - `ENK_KEYSTORE_PATH`
   - `ENK_KEYSTORE_PASSWORD`
   - `ENK_KEY_ALIAS`
   - `ENK_KEY_PASSWORD`

When configured, running `./gradlew :app:assembleRelease` produces a signed release APK under `app/build/outputs/apk/release/`.

### Automated GitHub Releases

Tagged commits matching `v*` (e.g. `v1.0.0`) trigger `.github/workflows/release.yml`.
The workflow signs the release APK using the following repository secrets:
- `ENK_RELEASE_KEYSTORE_BASE64`: Base64-encoded Java keystore (`.jks` / `.keystore`)
- `ENK_RELEASE_KEYSTORE_PASSWORD`: Keystore protection password
- `ENK_RELEASE_KEY_ALIAS`: Key alias name
- `ENK_RELEASE_KEY_PASSWORD`: Private key password

If these secrets are absent, the workflow publishes the unsigned release APK alongside its computed SHA-256 checksum.

---

## Reproducible Builds

Enk guarantees bit-for-bit reproducible builds on clean working trees:
- R8 minification and resource shrinking are deterministic.
- Version control info embedding is disabled (`vcsInfo.include = false`).
- Dependency metadata blobs are disabled (`dependenciesInfo { includeInApk = false; includeInBundle = false }`).
- Build timestamp injections are suppressed.
- Gradle distribution is checksum-pinned via `distributionSha256Sum`.

To verify reproducibility locally:
```bash
./gradlew clean :app:assembleRelease
sha256sum app/build/outputs/apk/release/app-release-unsigned.apk > hash1.txt

./gradlew clean :app:assembleRelease
sha256sum app/build/outputs/apk/release/app-release-unsigned.apk > hash2.txt

diff hash1.txt hash2.txt
```
Both hashes will match identically.

---

## F-Droid Compliance Verification

Before any release tag, run:
```bash
bash scripts/check-fdroid-rules.sh
bash scripts/check-no-network.sh
```
Both must output `OK` and `SUCCESS` with zero violations.
