#!/bin/bash

# Script to verify 16 KB alignment for APK
# Make executable: chmod +x verify_alignment.sh

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "Checking 16 KB page size alignment for APK..."

# Check if APK exists
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at $APK_PATH"
    echo "Run './gradlew assembleDebug' first"
    exit 1
fi

# Check with zipalign
echo "Running zipalign verification..."
zipalign -c -v -P 16 "$APK_PATH"

if [ $? -eq 0 ]; then
    echo "✅ APK is properly aligned for 16 KB page sizes"
else
    echo "❌ APK is NOT aligned for 16 KB page sizes"
    echo ""
    echo "Attempting to align the APK..."
    zipalign -f -v -P 16 "$APK_PATH" "${APK_PATH%.apk}-aligned.apk"

    if [ $? -eq 0 ]; then
        echo "✅ Successfully created aligned APK: ${APK_PATH%.apk}-aligned.apk"
    else
        echo "❌ Failed to align APK"
        exit 1
    fi
fi

# Check for problematic libraries
echo ""
echo "Checking for native libraries..."
unzip -l "$APK_PATH" | grep "\.so$" | while read -r line; do
    FILE=$(echo $line | awk '{print $4}')
    echo "Found native library: $FILE"
done

echo ""
echo "Detailed library analysis:"
aapt dump badging "$APK_PATH" | grep native-code