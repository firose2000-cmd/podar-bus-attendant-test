from pathlib import Path
import re, shutil, subprocess
root=Path(__file__).parent
android=root/'android'
checks={
 'Android app module exists':(android/'app/build.gradle.kts').exists(),
 'Manifest exists':(android/'app/src/main/AndroidManifest.xml').exists(),
 'Kotlin activity exists':(android/'app/src/main/java/org/podar/busattendant/MainActivity.kt').exists(),
 'Room schema exists':(android/'app/src/main/java/org/podar/busattendant/LocalStore.kt').exists(),
 'Keystore database encryption source exists':(android/'app/src/main/java/org/podar/busattendant/EncryptedDbKey.kt').exists(),
 'Cloud build workflow exists':(root/'.github/workflows/android-debug-build.yml').exists(),
 'No insecure cleartext opt-in':'usesCleartextTraffic="true"' not in (android/'app/src/main/AndroidManifest.xml').read_text(),
 'HTTPS default':'https://school-bus-server.example.invalid' in (android/'app/src/main/java/org/podar/busattendant/MainActivity.kt').read_text(),
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL')+' '+name)
print('ANDROID_SDK_AVAILABLE',bool(shutil.which('sdkmanager')))
print('GRADLE_AVAILABLE',bool(shutil.which('gradle')))
if not all(checks.values()):raise SystemExit(1)
