# Controlled pilot acceptance gates

- [ ] Compile debug APK successfully with reproducible dependency versions.
- [ ] Configure trusted HTTPS and confirm Android rejects untrusted certificate and HTTP.
- [ ] Test sign-in, token expiry, revocation and recovery after app restart.
- [ ] Test Room encryption, key-loss lock state and upgrade from prior pilot without data deletion.
- [ ] Download 3 eligible school days; verify PIS/PP Saturday rules and administrator-edited holidays.
- [ ] Mark mock attendance offline; force-close, reboot phone and verify queue persists.
- [ ] Repeat changes to one student; test retry, duplicate event ID, late replay, out-of-order arrival and concurrent Supervisor correction.
- [ ] Verify receipt and effective Supervisor attendance separately, with conflicts surfaced for review.
- [ ] Validate attendance counts and ensure no unexpected KM changes.
- [ ] Confirm rollback and database backup procedures before a limited supervised pilot.

Never use real student records for initial tests.
