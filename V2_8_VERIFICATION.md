# V2.8 verification

Static readiness checks (not Android compilation):

```
PASS app manifest exists
PASS Android Gradle plugin configured
PASS SDK version specified
PASS Kotlin kapt for Room
PASS SQLCipher included
PASS Keystore key wrapping
PASS no plaintext fallback
PASS cleartext traffic disabled
PASS HTTPS URL enforced
PASS sequence sent with events
PASS placeholder server still present (must configure before pilot)
PASS Gradle CI workflow exists
12/12 static checks passed
```

Exit code: 0
