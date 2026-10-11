from pathlib import Path
import shutil, os
root=Path(__file__).resolve().parent
android=root/'android'
api=(android/'app/src/main/java/org/podar/busattendant/MainActivity.kt').read_text()
checks={
 'Android project exists':(android/'settings.gradle.kts').exists(),
 'Gradle wrapper complete':all((android/p).exists() for p in ('gradlew','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties')),
 'Android SDK installed':bool((os.getenv('ANDROID_HOME') or os.getenv('ANDROID_SDK_ROOT')) and Path(os.getenv('ANDROID_HOME') or os.getenv('ANDROID_SDK_ROOT')).exists()),
 'Gradle executable installed':bool(shutil.which('gradle')),
 'Trusted production HTTPS configured':'.example.invalid' not in api,
 'Room outbox source exists':(android/'app/src/main/java/org/podar/busattendant/LocalStore.kt').exists(),
 'Android manifest exists':(android/'app/src/main/AndroidManifest.xml').exists(),
}
for name,ok in checks.items():print(('PASS' if ok else 'BLOCKED')+' '+name)
print('Static readiness only: no APK, TLS handshake, or phone restart test was performed.')
