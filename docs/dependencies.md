# Third-Party Dependencies and Provenance

## Verified Libraries for Batch 2

### 1. Backdrop
- **Coordinate:** `io.github.kyant0:backdrop:1.0.4`
- **Repository:** https://github.com/Kyant0/AndroidLiquidGlass
- **License:** Apache License, Version 2.0 (verified from `backdrop-1.0.4.pom`)
- **Min SDK:** API 21 (`minSdkVersion="21"` verified in AAR manifest; Enk `minSdk = 31`)
- **Target / Compile SDK Compatibility:**
  - Tested versions `2.0.1` and `1.0.5+`: Require `compileSdk >= 36` / `37` and AGP `9.1.0+` in AAR metadata, which fails AGP 8.13.2's `CheckAarMetadata` check under the pinned `compileSdk = 35`.
  - Tested version `1.0.4`: Passes `checkDebugAarMetadata` and `checkReleaseAarMetadata` cleanly with 0 issues under AGP `8.13.2` and `compileSdk = 35`.
- **Glass Implementation Role:**
  - **API 33+ (Refraction):** Backdrop lens/refraction on top of its blur effect.
  - **API 31-32 (Blur):** Backdrop's blur effect on the captured backdrop layer without refraction.

---

## Considered, Not Used

### Haze (`dev.chrisbanes.haze:haze`)
- **Reason Dropped:** Plan listed Haze, but testing demonstrated:
  - Haze `2.1.0`, `2.0.1`, `2.0.0` fail `checkDebugAarMetadata` and `checkReleaseAarMetadata` with 23 issues (require `compileSdk >= 37` and `AGP >= 9.1.0`).
  - Haze `1.7.0` to `1.7.3` fail with 3 to 22 issues (require `compileSdk >= 36` for transitive `androidx.activity:1.11.0`).
  - While Haze `1.6.10` passes, Backdrop 1.0.4's own blur natively covers the API 31-32 Blur path on the captured backdrop layer without needing an additional third-party library. Haze was therefore dropped from the catalog, build dependencies, and CREDITS.md to maintain a minimal, robust dependency footprint.
