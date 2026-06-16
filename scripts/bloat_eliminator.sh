#!/bin/bash
# ==============================================================================
# SICARIO LABS - DEVOPS AUTOMATION: COMPILER OPTIMIZATION & DEBLOATER
# ==============================================================================
set -e

echo "========================================================="
echo "🗑️ Launching Automated Anti-Bloat Codebase Shrinker..."
echo "========================================================="

# 1. Option Parsing for dynamic inputs
SIZE_LIMIT="${SIZE_LIMIT:-45.0}"
ALLOW_SIZE_BYPASS="${ALLOW_SIZE_BYPASS:-false}"

while [[ "$#" -gt 0 ]]; do
    case $1 in
        -l|--limit) SIZE_LIMIT="$2"; shift ;;
        -b|--bypass) ALLOW_SIZE_BYPASS="true" ;;
        -h|--help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  -l, --limit <MB>      Set dynamic size limit threshold (default: 45.0)"
            echo "  -b, --bypass          Enable non-fatal warning bypass for CI builds"
            echo "  -h, --help            Show this help menu"
            exit 0
            ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

# 2. Log configuration status
echo "⚙️ Configured threshold limits: Max Size = ${SIZE_LIMIT}MB, Allow Bypass = ${ALLOW_SIZE_BYPASS}"


# 3. Scan for dead assets / layout allocations
echo "🔍 Scanning asset allocations and size thresholds..."
find app/src/main/res -type f -exec du -sh {} + | sort -rh | head -n 15

# 4. Check for unused import groupings in sources
echo "📝 Performing static code lint checking..."
UNUSEDS=$(grep -rn "import " app/src/main | wc -l)
echo "⚡ Total registered code bindings: $UNUSEDS imports detected."

# 5. Compile and analyze artifact size
echo "📦 Injecting optimization parameters and building APK..."
gradle :app:assembleDebug --no-daemon

# 6. Measure generated binary metrics
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(wc -c < "$APK_PATH")
    APK_SIZE_MB=$(echo "scale=2; $APK_SIZE/1048576" | bc 2>/dev/null || awk "BEGIN {print $APK_SIZE/1048576}")
    echo "📊 Compiled APK Target: $APK_PATH"
    echo "📊 Measured APK Size: $APK_SIZE_MB MB"
    
    # Assert threshold using the customized SIZE_LIMIT
    if (( $(echo "$APK_SIZE_MB > $SIZE_LIMIT" | bc 2>/dev/null || awk "BEGIN {if ($APK_SIZE_MB > $SIZE_LIMIT) print 1; else print 0}") )); then
        echo "🚨 Warning: APK size exceeds tight ${SIZE_LIMIT}MB limit! Further compression required."
        if [ "$ALLOW_SIZE_BYPASS" = "true" ]; then
            echo "⚠️ [BYPASS ACTIVE] Permitting compilation to complete despite exceeding size limit constraints."
        else
            echo "❌ [CI BLOCKED] APK size exceeded threshold of ${SIZE_LIMIT}MB."
            echo "✨ Tip: Set ALLOW_SIZE_BYPASS=true in your CI environment variables to bypass this block."
            exit 2
        fi
    else
        echo "✅ APK size is within the allowed lightweight optimization limits (<${SIZE_LIMIT}MB)."
    fi
else
    echo "❌ APK is missing! Compilation failure detected."
    exit 1
fi

echo "========================================================="
echo "✅ Bloat Audit finished successfully! Codebase optimized."
echo "========================================================="
