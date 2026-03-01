#!/bin/bash

echo "🚀 BillShare App Setup Script"
echo "=============================="

# Step 1: Download gradle-wrapper.jar
echo ""
echo "📥 Downloading gradle-wrapper.jar..."
mkdir -p gradle/wrapper
curl -L "https://github.com/gradle/gradle/raw/v8.0.0/gradle/wrapper/gradle-wrapper.jar" \
     -o gradle/wrapper/gradle-wrapper.jar

if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "❌ Failed to download gradle-wrapper.jar"
    echo "   Try manually: https://github.com/gradle/gradle/raw/v8.0.0/gradle/wrapper/gradle-wrapper.jar"
    exit 1
fi

echo "✅ gradle-wrapper.jar downloaded!"

# Step 2: Make gradlew executable
chmod +x gradlew
echo "✅ gradlew is now executable"

# Step 3: Check Java
echo ""
echo "☕ Checking Java..."
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Install it with:"
    echo "   sudo apt install openjdk-17-jdk"
    exit 1
fi
java -version
echo "✅ Java found!"

# Step 4: Check ANDROID_HOME
echo ""
echo "🤖 Checking Android SDK..."
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  ANDROID_HOME not set."
    echo "   Download Android command-line tools from:"
    echo "   https://developer.android.com/studio#command-tools"
    echo ""
    echo "   Then add to your ~/.bashrc:"
    echo '   export ANDROID_HOME=$HOME/Android/Sdk'
    echo '   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools'
    echo ""
    echo "   Then run: sdkmanager --licenses"
    echo "             sdkmanager \"platforms;android-34\" \"build-tools;34.0.0\" \"platform-tools\""
else
    echo "✅ ANDROID_HOME = $ANDROID_HOME"
fi

echo ""
echo "=============================="
echo "🎉 Setup done! Now run:"
echo "   ./gradlew assembleDebug"
echo ""
echo "📦 APK will be at:"
echo "   app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "📱 Install on phone via USB:"
echo "   adb install app/build/outputs/apk/debug/app-debug.apk"
