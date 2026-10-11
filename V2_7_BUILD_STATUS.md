# V2.7 first-APK build status

- Android build was **attempted as an environment preflight** in the available workspace; no Gradle executable or Android SDK installed. Consequently the APK compile could not run.
- The included GitHub Actions workflow is a build recipe, **not evidence of a successful cloud build**. It requires a repository run and inspection of the build log.
- Source patch: encrypted database key-loss guard refuses to create a new passphrase when the encrypted database exists but its wrapped key is missing.
- The project remains **TEST ONLY**, with placeholder HTTPS host, no validated school certificate, no on-device install, and no end-to-end live sync tests.
- Do not deploy or change the operational school server.
