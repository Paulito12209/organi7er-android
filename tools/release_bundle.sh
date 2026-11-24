#!/bin/zsh
# Zusammenhang: app/build.gradle.kts (versionCode = Anzahl der Commits). Gleicher Ablauf wie bei Paralist.
set -e
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
[ -z "$(git status --porcelain -- app gradle build.gradle.kts settings.gradle.kts)" ] || { echo "Ungespeicherte Änderungen, erst committen."; exit 1; }
code=$(git rev-list --count HEAD)
keys="$HOME/.android-keys"
out="$keys/out/organi7er-$code.aab"
mkdir -p "$keys/out"
./gradlew -q bundleRelease
ORGANI7ER_STOREPASS="$(security find-generic-password -s organi7er-upload-keystore -a organi7er-upload -w)"
export ORGANI7ER_STOREPASS
"$JAVA_HOME/bin/jarsigner" -keystore "$keys/organi7er-upload.jks" -storepass:env ORGANI7ER_STOREPASS -keypass:env ORGANI7ER_STOREPASS \
    -signedjar "$out" app/build/outputs/bundle/release/app-release.aab upload > /dev/null
unset ORGANI7ER_STOREPASS
"$JAVA_HOME/bin/jarsigner" -verify "$out" > /dev/null
cp "$out" "$HOME/Downloads/"
echo "$HOME/Downloads/organi7er-$code.aab"
