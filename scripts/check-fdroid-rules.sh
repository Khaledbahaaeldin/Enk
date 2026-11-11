#!/usr/bin/env bash
# scripts/check-fdroid-rules.sh
# Verifies Enk meets all F-Droid inclusion rules and release integrity standards:
# (a) Runtime dependencies against allowlist and prohibited SDKs
# (b) Merged release manifest has zero network permissions
# (c) All media and font assets are tracked in provenance CSVs
# (d) Dependency-info blob is disabled in build file and APK

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

FAILED=0

# Determine gradle command
if [ -x "./gradlew" ]; then
    GRADLE="./gradlew"
else
    GRADLE="gradle"
fi

# -----------------------------------------------------------------------------
# Rule (a): Resolved runtime dependencies vs allow-list & prohibited SDKs
# -----------------------------------------------------------------------------
ALLOWLIST_FILE="scripts/fdroid-allowlist.txt"
if [ ! -f "$ALLOWLIST_FILE" ]; then
    echo "VIOLATION (a): Allow-list file $ALLOWLIST_FILE not found."
    FAILED=1
else
    # Build clean allowlist set (strip comments, empty lines, and trailing whitespace)
    ALLOWLIST_ENTRIES=$(grep -v '^[[:space:]]*#' "$ALLOWLIST_FILE" | sed -e 's/#.*//' -e 's/[[:space:]]*$//' | grep -v '^[[:space:]]*$' || true)

    # Collect dependencies for releaseRuntimeClasspath
    DEPS_TMP=$(mktemp)
    trap 'rm -f "$DEPS_TMP"' EXIT

    # Run gradle dependencies if needed or use gradle output
    if ! $GRADLE :app:dependencies --configuration releaseRuntimeClasspath --console=plain > "$DEPS_TMP" 2>&1; then
        echo "VIOLATION (a): Gradle could not resolve releaseRuntimeClasspath, so dependencies were not checked:"
        tail -n 15 "$DEPS_TMP"
        exit 1
    fi

    # Extract resolved coordinates (group:artifact:version)
    RESOLVED_DEPS=$(grep -E '^[| ]*[+\\]--- ' "$DEPS_TMP" | grep -v 'project :' | sed -E 's/\{[^}]*\}//g' | sed -E 's/^[| ]*[+\\]--- //' | sed -E 's/ \(\*\)//' | sed -E 's/ \(c\)//' | awk '{
        if ($0 ~ /->/) {
            split($0, parts, "->");
            sub(/[[:space:]]+$/, "", parts[1]);
            split(parts[1], ga, ":");
            sub(/^[[:space:]]+/, "", parts[2]);
            split(parts[2], ver, " ");
            print ga[1] ":" ga[2] ":" ver[1];
        } else {
            split($1, ga, ":");
            if (ga[3] != "") {
                print ga[1] ":" ga[2] ":" ga[3];
            }
        }
    }' | grep -v '^:' | sort -u || true)

    # A dependency list this short means the output was not parsed, not that the app has no dependencies.
    RESOLVED_COUNT=$(printf '%s
' "$RESOLVED_DEPS" | grep -c . || true)
    if [ "$RESOLVED_COUNT" -lt 20 ]; then
        echo "VIOLATION (a): Parsed only $RESOLVED_COUNT runtime dependencies; the Gradle output format may have changed."
        FAILED=1
    fi

    # Check prohibited tracking, ad, proprietary SDKs
    PROHIBITED_REGEX='(com\.google\.android\.gms|com\.google\.firebase|com\.crashlytics|com\.android\.billingclient|com\.facebook|AppLovin|Mixpanel|Sentry|Amplitude|play-services|firebase)'
    
    while IFS= read -r dep; do
        [ -z "$dep" ] && continue
        if echo "$dep" | grep -E -q "$PROHIBITED_REGEX"; then
            echo "VIOLATION (a): Prohibited proprietary/tracking dependency found: $dep"
            FAILED=1
        fi

        # Check against allowlist
        if ! echo "$ALLOWLIST_ENTRIES" | grep -F -x -q "$dep"; then
            echo "VIOLATION (a): Dependency not in allowlist: $dep"
            FAILED=1
        fi
    done <<< "$RESOLVED_DEPS"
fi

# -----------------------------------------------------------------------------
# Rule (b): Zero network permissions in merged release manifest
# -----------------------------------------------------------------------------
RELEASE_MANIFEST="app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml"
if [ ! -f "$RELEASE_MANIFEST" ]; then
    RELEASE_MANIFEST="app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml"
fi

if [ ! -f "$RELEASE_MANIFEST" ]; then
    # Generate release manifest if missing
    $GRADLE :app:processReleaseManifest --console=plain > /dev/null 2>&1 || true
    RELEASE_MANIFEST="app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml"
    if [ ! -f "$RELEASE_MANIFEST" ]; then
        RELEASE_MANIFEST="app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml"
    fi
fi

if [ ! -f "$RELEASE_MANIFEST" ]; then
    echo "VIOLATION (b): Merged release manifest not found at $RELEASE_MANIFEST."
    FAILED=1
else
    # Scan for network permissions
    NETWORK_PERMS=$(grep -E -i 'uses-permission.*(INTERNET|ACCESS_NETWORK_STATE|ACCESS_WIFI_STATE|CHANGE_NETWORK_STATE|CHANGE_WIFI_STATE)' "$RELEASE_MANIFEST" || true)
    if [ -n "$NETWORK_PERMS" ]; then
        echo "VIOLATION (b): Network permission found in merged release manifest:"
        echo "$NETWORK_PERMS"
        FAILED=1
    fi
fi

# -----------------------------------------------------------------------------
# Rule (c): All media and font files tracked in provenance CSVs
# -----------------------------------------------------------------------------
PROVENANCE_SOUNDS="docs/provenance.csv"
PROVENANCE_OTHER="docs/provenance-other.csv"

if [ ! -f "$PROVENANCE_SOUNDS" ]; then
    echo "VIOLATION (c): Provenance file $PROVENANCE_SOUNDS missing."
    FAILED=1
fi
if [ ! -f "$PROVENANCE_OTHER" ]; then
    echo "VIOLATION (c): Provenance file $PROVENANCE_OTHER missing."
    FAILED=1
fi

# Sound files are listed in provenance.csv by their path under assets/; everything else by repo path
# in provenance-other.csv. Match the whole path in the first column, never just the file name.
listed_in() { # csv, path
    [ -f "$1" ] && cut -d, -f1 "$1" | grep -F -x -q -- "$2"
}

while IFS= read -r -d '' f; do
    rel="${f#./}"
    asset_rel="${rel#app/src/main/assets/}"
    if listed_in "$PROVENANCE_SOUNDS" "$asset_rel" || listed_in "$PROVENANCE_OTHER" "$rel"; then
        continue
    fi
    echo "VIOLATION (c): Unlisted media or font file: $rel"
    FAILED=1
done < <(find app/src/main design/src/main audio/src/main fastlane -type f \(     -iname "*.ogg" -o -iname "*.wav" -o -iname "*.mp3" -o -iname "*.flac" -o     -iname "*.ttf" -o -iname "*.otf" -o -iname "*.woff" -o     -iname "*.png" -o -iname "*.webp" -o -iname "*.jpg" -o -iname "*.svg" \) -print0 2>/dev/null)

# -----------------------------------------------------------------------------
# Rule (d): Dependency-info blob disabled in build file and release APK
# -----------------------------------------------------------------------------
BUILD_FILE="app/build.gradle.kts"
if grep -q "includeInApk[[:space:]]*=[[:space:]]*true" "$BUILD_FILE" || \
   grep -q "includeInBundle[[:space:]]*=[[:space:]]*true" "$BUILD_FILE"; then
    echo "VIOLATION (d): dependenciesInfo has includeInApk or includeInBundle enabled in $BUILD_FILE."
    FAILED=1
fi

if ! grep -q "dependenciesInfo" "$BUILD_FILE"; then
    echo "VIOLATION (d): dependenciesInfo block missing in $BUILD_FILE."
    FAILED=1
fi

# Check release APK if present
RELEASE_APK=$(find app/build/outputs/apk/release -type f -name "*.apk" 2>/dev/null | head -n 1 || true)
if [ -n "$RELEASE_APK" ] && [ -f "$RELEASE_APK" ]; then
    DEP_ENTRIES=$(unzip -l "$RELEASE_APK" | grep -E -i '(dependencies/|package-dependencies|dependency-info|dependenciesInfo)' || true)
    if [ -n "$DEP_ENTRIES" ]; then
        echo "VIOLATION (d): Dependency-info blob entry found inside release APK $RELEASE_APK:"
        echo "$DEP_ENTRIES"
        FAILED=1
    fi
fi

# -----------------------------------------------------------------------------
# Final result
# -----------------------------------------------------------------------------
if [ "$FAILED" -ne 0 ]; then
    exit 1
fi

echo "OK"
exit 0
