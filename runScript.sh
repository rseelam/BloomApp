#!/bin/bash

echo "1. Cleaning..."
./gradlew clean
rm -rf app/build

echo "2. Checking structure..."
find app/src/main -name "*.kt" | head -5

echo "3. Creating minimal MainActivity..."
mkdir -p app/src/main/java/com/bloom/familytasks
echo 'package com.bloom.familytasks
import android.os.Bundle
import androidx.activity.ComponentActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}' > app/src/main/java/com/bloom/familytasks/MainActivity.kt

echo "4. Building..."
./gradlew assembleDebug --info 2>&1 | tee build_output.txt

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    ~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
    ~/Library/Android/sdk/platform-tools/adb shell am start -n com.bloom.familytasks/.MainActivity
else
    echo "❌ Build failed. Error details:"
    grep "e: " build_output.txt
fi
