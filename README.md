# DeepTutor Android (MVP)

A small **unofficial** Android WebView client for a self-hosted [DeepTutor](https://github.com/HKUDS/DeepTutor) instance. This is an independent client, not affiliated with the DeepTutor developers.

## Scope (v0.1.0)

- Configure an HTTPS DeepTutor server URL at first launch; reload or switch servers later.
- Native Android WebView with JavaScript, DOM storage, and persistent cookies for sessions.
- Android system file picker for `<input type=file>` uploads.
- Same-origin HTTPS downloads through Android DownloadManager (with session cookies).
- External links opened outside the app; same-origin DeepTutor navigation stays in the app.
- WebView back navigation, reload, and basic renderer-crash recovery.

## Build

Open this folder in **Android Studio** using **JDK 17**, install Android SDK 35, and sync Gradle. Android Studio will download Gradle as needed. There is **no Gradle wrapper** in this initial source archive; for CLI use, run `gradle wrapper --gradle-version 8.11.1` from an installed Gradle version, then `./gradlew assembleDebug`.

Package: `dev.qssn70.deeptutor`. Min Android API 26. Requires a publicly trusted HTTPS certificate (or a certificate trusted by the OS/WebView).

## Architecture

- `config/ServerConfig`: URL validation and persistence.
- `web/NavigationPolicy`: origin containment.
- `web/DeepTutorWebClient`: navigation, load errors, renderer recovery.
- `files/FilePicker`: upload callback lifecycle.
- `MainActivity`: thin integration shell and deliberately minimal controls.

### Important limits / roadmap

1. No native JavaScript bridge is exposed: unnecessary for v0.1 and safer by default.
2. Downloads via `blob:` URLs, POST-backed downloads, OAuth external popup flows, camera capture, and Android shares are **not implemented**.
3. Android layout is intentionally minimal and is **not** a solution to upstream DeepTutor's mobile responsive layout issues.
4. The current server setting accepts HTTPS origins with paths; set the DeepTutor site root URL unless deployed under a supported base path.
5. Initial MVP uses platform UI APIs to minimize dependencies. Migrate to Compose + DataStore when the settings screen grows; do not rewrite the WebView boundary.
6. Switching servers preserves the shared WebView cookie jar. A multi-account/multi-server feature must implement deliberate profile/session isolation.
7. No APK is attached: this environment did not run an Android Gradle build. Test on Android 8+ and modern WebView before release.

## Security

Do not disable TLS verification or add arbitrary `addJavascriptInterface()` methods. Third-party cookies, mixed content, and file access are disabled. Navigation to other origins is external. Use a dedicated server or trusted HTTPS reverse proxy and keep DeepTutor itself patched.
