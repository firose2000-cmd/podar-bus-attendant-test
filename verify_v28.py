from pathlib import Path
import re, sys
p=Path(__file__).parent
a=p/'android'
source=(a/'app/src/main/java/org/podar/busattendant/MainActivity.kt').read_text()
api=(a/'app/src/main/java/org/podar/busattendant/ServerApi.kt').read_text()
manifest=(a/'app/src/main/AndroidManifest.xml').read_text()
gradle=(a/'app/build.gradle.kts').read_text()
checks={
 'app manifest exists': (a/'app/src/main/AndroidManifest.xml').exists(),
 'Android Gradle plugin configured': 'com.android.application' in (a/'build.gradle.kts').read_text(),
 'SDK version specified': 'compileSdk = 35' in gradle,
 'Kotlin kapt for Room': 'org.jetbrains.kotlin.kapt' in gradle,
 'SQLCipher included': 'android-database-sqlcipher' in gradle,
 'Keystore key wrapping': 'EncryptedDbKey.passphrase' in source,
 'no plaintext fallback': 'openHelperFactory(SupportFactory(passphrase))' in source,
 'cleartext traffic disabled': 'usesCleartextTraffic="false"' in manifest,
 'HTTPS URL enforced': 'base.startsWith("https://"' in api,
 'sequence sent with events': '.put("client_sequence",sequence)' in api,
 'placeholder server still present (must configure before pilot)': 'school-bus-server.example.invalid' in source,
 'Gradle CI workflow exists': (p/'.github/workflows/android-debug-build.yml').exists(),
}
for k,v in checks.items():print(('PASS' if v else 'FAIL')+' '+k)
print(f'{sum(checks.values())}/{len(checks)} static checks passed')
sys.exit(0 if all(checks.values()) else 1)
