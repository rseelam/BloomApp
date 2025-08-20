#!/bin/bash

echo "🧹 Cleaning..."
./gradlew --stop
./gradlew clean
rm -rf app/build

echo "🔨 Building..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo "📱 Installing..."
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    
    if [ $? -eq 0 ]; then
        echo "🚀 Launching app..."
        adb shell am start -n com.bloom.familytasks/.MainActivity
    fi
else
    echo "❌ Build failed"
fi
