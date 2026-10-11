# Bus Attendant V1.5 — encrypted offline database (SOURCE TEST ONLY)

## Implemented
- SQLCipher-backed Room database (`attendant_v15_encrypted.db`) with a 256-bit random passphrase wrapped by a non-exportable Android Keystore AES-GCM key.
- No cleartext database fallback; failure to decrypt produces a locked-state warning rather than erasing queued attendance.
- Explicit guard against silently moving from V1.4 plaintext database; migration/reconciliation is required if that test database exists.
- Android cleartext HTTP remains disabled; server address still requires trusted HTTPS.
- V1.4 event IDs, durable queue, three-working-day manifests and server receipt/effective-attendance checks retained.

## Not yet implemented / critical limits
- This is NOT an APK. Android Gradle compilation, dependency resolution and physical-device tests have NOT been performed.
- Existing server uses an in-memory attendant cookie session. This app still requires fresh login after process restart to sync. Persistent, revocable credentials/tokens need coordinated SERVER changes; do not store attendant passwords.
- Trusted TLS certificate and hostname for school LAN are NOT configured. The `.example.invalid` URL is intentionally unusable.
- No tested migration from V1.4. Keystore loss or device restore may make the encrypted DB unreadable; a tested backup/recovery plan is needed.
- Stronger server-side event ordering and Supervisor conflict semantics, holiday UI, full sync integration tests and actual overnight tests remain pending.
- Database encryption does not replace device lock, privacy policy, or server authentication. Never use real student data in this test build.

## Safe next milestone
Coordinate server-issued revocable mobile authentication and verified sync receipts, then compile and run emulator/phone tests before pilot APK. Do not modify the operational school server or its database.
