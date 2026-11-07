#!/usr/bin/env bash
set -euo pipefail

MANIFESTS=$(find app/build/intermediates/merged_manifests/debug -type f -name "AndroidManifest.xml" 2>/dev/null || true)

if [ -z "$MANIFESTS" ]; then
    echo "ERROR: Merged AndroidManifest.xml not found under app/build/intermediates/merged_manifests/debug." >&2
    echo "Please run './gradlew :app:processDebugManifest' before executing this check." >&2
    exit 1
fi

FORBIDDEN_PERMISSIONS=(
    "android.permission.INTERNET"
    "android.permission.ACCESS_NETWORK_STATE"
    "android.permission.ACCESS_WIFI_STATE"
)

VIOLATIONS=0

for MANIFEST in $MANIFESTS; do
    echo "Checking manifest: $MANIFEST"
    for PERM in "${FORBIDDEN_PERMISSIONS[@]}"; do
        if grep -F "$PERM" "$MANIFEST" > /dev/null 2>&1; then
            echo "ERROR: Forbidden network permission declared in $MANIFEST: $PERM" >&2
            VIOLATIONS=$((VIOLATIONS + 1))
        fi
    done
done

if [ "$VIOLATIONS" -gt 0 ]; then
    echo "Network permission check FAILED ($VIOLATIONS violations found)." >&2
    exit 1
fi

echo "SUCCESS: No network permissions declared in merged manifest."
exit 0
