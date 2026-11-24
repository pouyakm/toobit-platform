# Toobit Platform — Android client

Native Kotlin + Jetpack Compose client for the existing .NET API (`backend/`). It has the same screens as the web dashboard:
Scanner (explainable score, pin), Positions (confirmed close), Orders (state trail, `Unknown` highlighted), Signals, Risk (pauses),
Settings (kill switch with typed `HALT`/`RESUME`, feature flags). English + Persian (RTL).

## Security model
- The phone talks ONLY to our .NET API. **No Toobit keys, no exchange endpoints, no secrets in the APK.**
- Access token: memory only. Refresh token + server URL: `EncryptedSharedPreferences` (Android Keystore). Backup/transfer disabled.
- Refresh tokens are single-use/rotating on the server, so 401s are collapsed into ONE refresh (mutex); a rejected refresh logs out.
- Release builds: HTTPS only. Debug builds allow cleartext (`src/debug/res/xml/network_security_config.xml`) for `http://10.0.2.2:8080`.
- `FLAG_SECURE` (no screenshots), optional biometric/device-lock on resume, no HTTP body logging.
- The server enforces RBAC; the UI only hides controls (Viewer: read-only; Trader: pin/close; Admin: everything).

## Build and run (needs Android Studio or SDK 35 + JDK 17)
```
cd android
gradle wrapper --gradle-version 8.9       # once; generates ./gradlew (not committed)
./gradlew test                            # JVM unit tests (no emulator)
./gradlew assembleDebug                   # app/build/outputs/apk/debug/app-debug.apk
```
Emulator -> server on your PC: enter `http://10.0.2.2:8080` at login. Real phone: use an HTTPS URL (or debug build + LAN IP).
Release signing is NOT configured in the repo; supply a keystore via CI secrets.

## Status — read this
Written without being compiled: the authoring sandbox blocked Google/Maven/Gradle hosts and has no Kotlin compiler or Android SDK.
Not verified: Gradle sync, compilation, unit tests, UI on a device. Verified: XML well-formed, en/fa string parity, every `R.string` used exists.
Expect a few compile fixes on first build. Known gaps: no push notifications (needs FCM + backend), no launcher PNG/adaptive icon,
pin state is local (server list not exposed), polling instead of WebSocket, no instrumented UI tests, no committed Gradle wrapper.
