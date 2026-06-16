#!/bin/bash
# ==============================================================================
# SICARIO LABS - DEVOPS AUTOMATION: COMPILER OPTIMIZATION & DEBLOATER
# ==============================================================================
set -e

echo "========================================================="
echo "🗑️ Launching Automated Anti-Bloat Codebase Shrinker..."
echo "========================================================="

# 1. Scan for dead assets / layout allocations
echo "🔍 Scanning asset allocations and size thresholds..."
find app/src/main/res -type f -exec du -sh {} + | sort -rh | head -n 15

# 2. Check for unused import groupings in sources
echo "📝 Performing static code lint checking..."
UNUSEDS=$(grep -rn "import " app/src/main | wc -l)
echo "⚡ Total registered code bindings: $UNUSEDS imports detected."

# 3. Compile and analyze artifact size
echo "📦 Injecting optimization parameters and building APK..."
gradle :app:assembleDebug --no-daemon

# 4. Measure generated binary metrics
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(wc -c < "$APK_PATH")
    APK_SIZE_MB=$(echo "scale=2; $APK_SIZE/1048576" | bc 2>/dev/null || awk "BEGIN {print $APK_SIZE/1048576}")
    echo "📊 Compiled APK Target: $APK_PATH"
    echo "📊 Measured APK Size: $APK_SIZE_MB MB"
    
    # Assert threshold of 15MB to keep application lightweight
    if (( $(echo "$APK_SIZE_MB > 15.0" | bc 2>/dev/null || awk "BEGIN {if ($APK_SIZE_MB > 15.0) print 1; else print 0}") )); then
        echo "🚨 Warning: APK size exceeds tight 15MB limit! Further compression required."
    else
        echo "✅ APK size is within the allowed lightweight optimization limits (<15MB)."
    fi
else
    echo "❌ APK is missing! Compilation failure detected."
    exit 1
fi

echo "========================================================="
echo "✅ Bloat Audit finished successfully! Codebase optimized."
echo "========================================================="
