# Mochi

An unofficial Android client for tracking anime & manga, backed by **MyAnimeList**. Built with Kotlin, Jetpack Compose, and Material 3 Expressive.

Mochi syncs your MAL list, lets you update progress/status/score, and discover new titles by genre — talking directly to MyAnimeList's own servers, with no third-party account or database in between.

---

## ✨ Features

- **Sign in with MyAnimeList** — OAuth2 + PKCE, opens in your browser (not an in-app WebView).
- **My List** — your synced anime & manga library, status filters (Watching/Completed/On Hold/Dropped/Plan to Watch, etc.), quick +1 progress from the list itself.
- **Discover** — genre-filtered search across anime and manga, via the Tenrai (Jikan-compatible) API, since MAL's own API doesn't support genre filtering.
- **Detail** — synopsis, genres, score/rank/popularity, and a full status/progress/score editor that writes straight back to MAL.
- **Profile** — anime stats from the official API, plus manga stats (days read, chapters, status breakdown) via an optional MAL *website* sign-in — MAL's API has no manga-statistics field at all, so this is the only way to get them.

## 🛠️ Built with

Kotlin · Jetpack Compose · Material 3 Expressive · Hilt · Retrofit · OkHttp · kotlinx.serialization · Coil 3 · Navigation Compose (type-safe routes) · DataStore Preferences · Jsoup

## 🌐 Data sources

- **MyAnimeList API** — accounts, list sync, updates, search, title detail.
- **Tenrai API** — genre-filtered discovery/search (a Jikan-compatible mirror).
- **myanimelist.net (scraped)** — manga stats only, and only after an explicit, separate website sign-in. Nothing else is scraped.

Mochi is not affiliated with, sponsored by, or endorsed by MyAnimeList.

---

## 📁 Project structure

```
app/src/main/java/io/github/mochi/
├── MainActivity.kt              Entry point: sets Compose content, handles the OAuth deep link
├── MochiApp.kt                  @HiltAndroidApp Application class
│
├── data/
│   ├── api/                     Retrofit interfaces + @Serializable DTOs
│   │   ├── MalService.kt            Official MAL API v2 (list, detail, update, profile)
│   │   ├── MalOAuthService.kt       OAuth2 token endpoint
│   │   ├── TenraiService.kt         Tenrai/Jikan-compatible discovery API
│   │   └── *Dto.kt                  Wire-format models for each of the above
│   ├── auth/
│   │   └── AuthTokenStore.kt        DataStore-backed OAuth access/refresh token storage
│   ├── error/
│   │   ├── MochiError.kt            Sealed hierarchy of user-facing failures
│   │   ├── ErrorMapper.kt           Throwable -> MochiError
│   │   └── SafeCall.kt              safeCall {} wrapper used by every repository
│   ├── model/                   Domain models, decoupled from any single API's wire format
│   │   ├── MediaItem.kt             Anime/manga item, used by both MAL and Tenrai mappers
│   │   ├── MalProfile.kt            Anime + manga profile stats
│   │   ├── MediaType.kt / Genre.kt  Enums and small value types
│   │   └── *Mappers.kt              DTO -> domain model conversion
│   ├── repository/
│   │   ├── AuthRepository.kt        PKCE auth-URL, code exchange, sign-out
│   │   ├── MalRepository.kt         List fetch/update/delete, detail, profile
│   │   ├── DiscoverRepository.kt    Tenrai search + genre lists, with rate-limit backoff
│   │   └── ProfileRepository.kt     Combines API profile + scraped manga stats
│   └── scrape/
│       ├── MalSessionCookieStore.kt DataStore-backed MAL *website* session cookie
│       └── MalProfileScrapeApi.kt   Scrapes manga stats off the profile page (Jsoup)
│
├── di/
│   ├── NetworkModule.kt          OkHttp/Retrofit/Json Hilt bindings (separate clients for
│   │                             the MAL API, MAL OAuth, and Tenrai — see Auth model below)
│   └── MalAuthenticator.kt       OkHttp Authenticator: refreshes the access token on 401
│
└── ui/
    ├── MainViewModel.kt          App-level state machine (see App flow below)
    ├── MochiRoot.kt              Root composable — branches on MainViewModel's state
    ├── theme/                    Material 3 Expressive theme + color scheme
    ├── navigation/                Type-safe nav graph (kotlinx.serialization routes) + bottom nav
    ├── components/                Shared composables: MediaCard, PosterCard, MalLoginWebView
    └── screens/
        ├── onboarding/            OnboardingScreen, LibraryLoadingScreen
        ├── list/                  My List
        ├── discover/              Discover
        ├── detail/                Title detail + editor
        ├── profile/               Profile + stats
        └── login/                 WebView MAL website sign-in (manga stats only)
```

## 🔁 App flow

1. **Onboarding** — signed out → `OnboardingScreen`. "Sign in with MyAnimeList" opens MAL's OAuth authorize page in an external browser tab (Custom Tabs), using PKCE (`code_challenge_method=plain`, per MAL's requirements).
2. **OAuth callback** — MAL redirects to `mochi://oauth/callback`; `MainActivity`'s intent-filter catches it and hands the URI to `MainViewModel`, which exchanges the code for tokens through `AuthRepository`.
3. **First load** — once signed in, `MainViewModel` fetches the anime + manga lists once (`LibraryLoadingScreen`) before revealing the main app, so you don't land on an empty list.
4. **Main app** — three-tab bottom navigation:
   - **My List** — synced library, quick +1 progress, status filter chips.
   - **Discover** — genre-filtered browse/search; tapping a result opens Detail, where "Add to List" upserts it straight onto your MAL list.
   - **Profile** — anime stats always; manga stats once you've also signed into the MAL website via the in-app WebView (prompted from this screen).
5. **Detail** — edit status/progress/score; saved directly to MAL via `PATCH .../my_list_status`.

## 🔐 Auth model

Mochi uses two independent credentials against two different faces of MAL:

| | Talks to | Stored in | Used for |
|---|---|---|---|
| OAuth token (PKCE) | `api.myanimelist.net` (official API) | `AuthTokenStore` (DataStore) | List sync, updates, search, detail, anime stats |
| Website session cookie | `myanimelist.net` (plain website) | `MalSessionCookieStore` (DataStore) | Manga stats only (scraped — no API for this) |

Both are excluded from Android's cloud backup and device-transfer (see `app/src/main/res/xml/data_extraction_rules.xml` / `backup_rules.xml`) since they're revocable, install-specific credentials that shouldn't leave the device.

## ⚠️ Error handling

Every network call funnels through `safeCall {}` (`data/error/SafeCall.kt`), which maps raw exceptions into a `MochiError` sealed type whose message is already safe to show in a snackbar as-is: `NoConnection`, `Timeout`, `Unauthorized`, `RateLimited`, `NotFound`, `ScrapeSessionExpired`, `ServerError`, `Unknown`. No screen ever shows a raw OkHttp/Retrofit exception message.

---

## 🧱 Building from source

**Requirements:** Android Studio, JDK 17, Android SDK Platform 36, a MyAnimeList API client ID.

```bash
git clone git@github.com:lemmesleep247/Mochi.git
cd Mochi
```

Register a client at [myanimelist.net/apiconfig](https://myanimelist.net/apiconfig) with the app redirect URI set to `mochi://oauth/callback`. Then copy the local config template and fill in your client ID:

```bash
cp local.properties.copy local.properties
```

```properties
# local.properties
MAL_CLIENT_ID=your_client_id_here
```

`local.properties` is git-ignored — it's never committed. Do **not** put a client secret in it either — Mochi is a public client using PKCE, and MAL's API only needs the client ID.

Build:

```bash
./gradlew assembleDebug   # macOS/Linux
gradlew.bat assembleDebug # Windows
```

APK output: `app/build/outputs/apk/debug/`.

## 🚀 Releases

Pushing a tag matching `v*.*.*` (e.g. `v1.2.0`) runs `.github/workflows/release.yml`, which builds release APKs for every ABI (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`) plus a universal APK, and opens a **draft** GitHub Release with all of them attached and its body filled in from the matching section of [CHANGELOG.md](CHANGELOG.md). Nothing is published automatically — review the draft and hit "Publish" yourself.

Release APKs are only signed if the repo has `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` secrets configured; without them the workflow still runs and attaches unsigned APKs. An optional `MAL_CLIENT_ID` repo secret bakes a working OAuth client ID into these builds — otherwise released APKs build fine but sign-in won't work until a user supplies their own via `local.properties`, which isn't possible for a prebuilt APK.

## 🤝 Contributing

Contributions are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md).

## 📄 License

MIT — see [LICENSE](LICENSE).
