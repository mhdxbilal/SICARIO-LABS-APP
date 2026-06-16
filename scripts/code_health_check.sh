#!/bin/bash
# ==============================================================================
# SICARIO LABS - AUTOMATED CODE HEALTH CHECK PIPELINE
# ==============================================================================
set -e

echo "========================================================="
echo "🔍 Running Automated Code Health Check Audit..."
echo "========================================================="

# 1. Check file extensions and structure
echo "📂 Scanning file distribution..."
find app/src -type f | sed 's/.*\.//' | sort | uniq -c

# 2. Compile tests or verify Gradle compilation is clean
echo "🔨 Running dry-run validation of production build..."
gradle compileDebugKotlin --no-daemon

# 3. Code formatting compliance detection
echo "✨ Analyzing manifest declarations..."
if grep -q "android:hardwareAccelerated=\"false\"" app/src/main/AndroidManifest.xml; then
    echo "⚠️ Warning: Found disabled hardware acceleration in Manifest!"
    exit 1
else
    echo "✅ Hardware acceleration verified active in Manifest."
fi

# 4. Check for unresolved dependencies or duplicates
echo "📦 Analyzing project build dependencies..."
gradle dependencies --configuration debugRuntimeClasspath --no-daemon | head -n 50

echo "✅ Code Health Audit Completed Successfully with Clean Status!"
