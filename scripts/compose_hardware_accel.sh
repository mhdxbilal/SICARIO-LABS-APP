#!/bin/bash
# ==============================================================================
# SICARIO LABS - JETPACK COMPOSE HARDWARE ACCELERATION ENFORCER
# ==============================================================================
set -e

echo "========================================================="
echo "⚡ Validating Jetpack Compose Render Performance Configs..."
echo "========================================================="

# 1. Verify Manifest acceleration
echo "🖼️ Checking hardware accelerated flags in application nodes..."
ACCEL_ENABLED=$(grep -o 'android:hardwareAccelerated="true"' app/src/main/AndroidManifest.xml | wc -l)
if [ "$ACCEL_ENABLED" -eq 0 ]; then
    echo "🚨 Error: Application level hardware acceleration is not explicitly configured!"
    exit 1
else
    echo "✅ Hardware acceleration flag is positive: $ACCEL_ENABLED activities."
fi

# 2. Verify Compose runtime dependencies
echo "📦 Verifying Jetpack Compose runtime & compiler versions..."
gradle :app:dependencyInsight --dependency androidx.compose.runtime:runtime --configuration debugRuntimeClasspath --no-daemon

# 3. Check for heavy layout compositions
echo "🔬 Inspecting Composable definitions for key patterns..."
STATE_COUNT=$(grep -rn "mutableStateOf" app/src/main/java | wc -l)
REM_COUNT=$(grep -rn "remember" app/src/main/java | wc -l)

echo "📉 State telemetry: found $STATE_COUNT dynamic states, $REM_COUNT remember containers."

echo "========================================================="
echo "🚀 Hardware acceleration rendering assertions complete."
echo "========================================================="
