#!/bin/bash

./gradlew clean assembleStandardRelease

TOOLS="$(ls -d ${ANDROID_HOME}/build-tools/* | tail -1)"
export ARTIFACT="tachiyomi-${TRAVIS_TAG}.apk"

${TOOLS}/zipalign -v -p 4 app/build/outputs/apk/standard/release/app-standard-release-unsigned.apk app-aligned.apk
${TOOLS}/apksigner sign --ks $STORE_PATH --ks-key-alias $STORE_ALIAS --ks-pass env:STORE_PASS --key-pass env:KEY_PASS --out $ARTIFACT app-aligned.apk
