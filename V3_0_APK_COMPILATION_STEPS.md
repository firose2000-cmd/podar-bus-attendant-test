# V3.0 — How to compile the APK using GitHub Actions (Windows, no command line)

This is a TEST source bundle, not an APK. Do not upload student records or server secrets.

1. Sign in to GitHub (https://github.com) and create a **private** repository named `podar-bus-attendant-test`.
2. Download and extract the V3.0 ZIP on your computer. Open the folder `Bus_Attendant_V3_0_Cloud_APK_Build_TEST`.
3. Upload **the contents of that folder** to the repository root, retaining the `android` and `.github` folders. GitHub's browser upload may not preserve hidden `.github` folders reliably; if `.github/workflows/android-debug-build.yml` does not appear in the repository, create it through GitHub's Add file > Create new file using that exact path, then paste the file contents.
4. Open repository **Actions**. If prompted, enable Actions for this private repository.
5. Select **Android debug APK compile (TEST ONLY)** > **Run workflow** > **Run workflow**.
6. Wait for the run to finish. If green, open the run and download the artifact `attendant-v30-debug-NOT-FOR-SCHOOL` from **Artifacts**. It contains the debug APK.
7. If red, open the same run and download `android-build-log`. Share that build log here so compiler errors can be fixed. No need to troubleshoot yourself.

**Important**: A compiled APK is not ready for real school attendance. The Android server URL is a deliberately nonworking `.example.invalid` placeholder. HTTPS, login, encrypted-data recovery, real-device offline restart and server sync still need verification. Do not install on production school devices.

**Safety**: Do not upload live databases, student lists, passwords, API tokens, certificates, or private keys. The included server folder is for development only. Do not replace the operating school server.
