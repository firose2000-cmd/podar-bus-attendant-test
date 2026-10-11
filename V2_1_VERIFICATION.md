# V2.1 verification record

- 20 isolated Python tests passed on 2026-10-11.
- Python sources compiled; no production school database accessed.
- Sequence metadata added to Android event payload and server's separate `mobile_event_sequence` table.
- Android Gradle/SDK not installed here: no APK compilation performed.
- Trusted HTTPS not configured; phone restart and end-to-end testing pending.

## Blocking limitations

1. The count updater `_rc113_attendance_counts` still scans raw mobile events by receipt time; it must use the effective sequence-aware projection before a pilot.
2. Multiple phones sharing an attendant username cannot use comparable rowid sequences; a stable device identity and device-scoped sequencing protocol are needed.
3. Legacy browser and Android mixed writes are not fully ordered.
4. Supervisor correction race conditions require transaction-level integration tests.
5. Android Room query for SQLite `rowid` has not been compiled and must be verified in an Android build environment.

**Do not deploy to the school server or install on phones.**
