# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew assembleDebug              # Build debug APK -> app/build/outputs/apk/debug/
./gradlew installDebug                # Build and install on a connected device/emulator
./gradlew test                        # Unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "io.github.mochi.SomeClassTest"   # Single unit test
./gradlew connectedAndroidTest        # Instrumented tests (app/src/androidTest) — needs a device/emulator
./gradlew lint                        # Lint -> app/build/reports/lint-results-debug.html
./gradlew clean
```

Requires a MyAnimeList API client ID in `local.properties` (`MAL_CLIENT_ID=...`), registered at myanimelist.net/apiconfig with redirect URI `mochi://oauth/callback`. See README.md for the full setup.

## Architecture

Layered by concern under `app/src/main/java/io/github/mochi/`: `data/` (api → repository, plus `error/`, `auth/`, `scrape/`, `model/`), `di/` (Hilt modules), `ui/` (screens, one package per destination, each with its own `@HiltViewModel`).

### Two independent auth mechanisms, against two different faces of MAL

- **OAuth token** (PKCE, no client secret) authenticates against `api.myanimelist.net`, MAL's official REST API. Stored in `AuthTokenStore` (DataStore). Covers list sync, updates, search, detail, and anime stats.
- **Website session cookie** authenticates a plain `myanimelist.net` page load, captured via an in-app WebView login (`ui/components/MalLoginWebView.kt` + `data/scrape/MalSessionCookieStore.kt`). The *only* thing this is for is scraping manga stats (`data/scrape/MalProfileScrapeApi.kt`, via Jsoup) — MAL's official API has no `manga_statistics` field at all, so there is no API-only way to get this data. `ProfileRepository` layers scraped manga stats onto the API-sourced profile, falling back silently to API-only data if scraping fails or the cookie is missing/expired.

Both credentials are excluded from Android backup/device-transfer in `data_extraction_rules.xml` / `backup_rules.xml` — don't remove those exclusions when touching DataStore file names (`mal_auth`, `mal_cookie`).

### Network layer (`di/NetworkModule.kt`)

Three separate Retrofit instances, each backed by its own qualified `OkHttpClient`, to avoid a DI cycle: the MAL API client (`@MalApiOkHttp`) attaches the bearer token and uses `MalAuthenticator` to transparently refresh on 401; the MAL OAuth client (unqualified/base) has no auth headers, since it's what `MalAuthenticator` itself calls to refresh — if it depended on the authenticated client, that would cycle. Tenrai's client also reuses the unqualified base client (public API, no auth). All three DTOs decode via kotlinx.serialization (`retrofit2.converter.kotlinx.serialization.asConverterFactory`), not Gson/Moshi.

### Error handling convention

Every repository suspend function wraps its network work in `safeCall { }` (`data/error/SafeCall.kt`), which maps the raw exception into a `MochiError` sealed type (`data/error/MochiError.kt`) with a message that's already safe to display as-is. ViewModels never need to format exception messages themselves — `catch { e -> ... e.message ... }` already gets clean text. When adding a new repository call, wrap it in `safeCall` rather than letting raw `HttpException`/`IOException` propagate.

### One domain model across two source APIs

`data/model/MediaItem.kt` is the single domain type the whole UI layer consumes for anime/manga entries, regardless of whether it came from the official MAL API (`data/model/MalMappers.kt`) or from Tenrai's Jikan-compatible search (`data/model/TenraiMappers.kt`). Tenrai-sourced items always have `inList = false` / `listStatus = ListStatus.planFor(type)` since Tenrai search isn't tied to the signed-in user's list — `DetailScreen`'s Save button already upserts via `PATCH .../my_list_status` regardless of prior list membership, so "Save" doubles as "Add to List" for free.

### App-level state machine (`ui/MainViewModel.kt`, `ui/MochiRoot.kt`)

`AppUiState` (`SignedOut` → `LoadingLibrary` → `Ready`) gates which root composable renders. The transition to `LoadingLibrary` is guarded on the *previous* state being `SignedOut` specifically (not just "signed in is true"), because `AuthTokenStore.isSignedIn` can re-emit the same boolean value on unrelated DataStore writes (e.g. saving the PKCE verifier mid sign-in touches the same file) — guarding on `!is Ready` instead would double-trigger the warm-up fetch.

### Navigation (`ui/navigation/`)

Type-safe routes via kotlinx.serialization (`@Serializable object`/`data class` route types + `composable<T>` + `entry.toRoute<T>()`), not string routes. Bottom nav visibility is computed from `NavDestination.hasRoute(Route::class)`, not by tracking selected-tab state separately.

### Window insets — nested Scaffold gotcha

`MochiRoot` → `MochiNavHost` → each screen is three levels of `Scaffold`. Only one level per edge should reserve a system-bar inset, or they stack (this happened once — see git history). Current convention: `MochiRoot` and `MochiNavHost`'s own `Scaffold`s reserve **zero** insets (`WindowInsets(0,0,0,0)`); the real `NavigationBar` in `MochiNavHost` pads itself internally; `ListScreen`/`DiscoverScreen`/`ProfileScreen` (which always show under the bottom nav) reserve **top only** (`WindowInsets.safeDrawing.only(WindowInsetsSides.Top)`); `DetailScreen`/`MalLoginScreen` (no bottom nav on those routes) keep the **default** (both edges). `OnboardingScreen`/`LibraryLoadingScreen`, which render with no `Scaffold` of their own, apply `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` directly. When adding a new top-level screen, follow whichever of these three patterns matches whether it shows under the bottom nav.

### Material 3 Expressive

Theme entry point is `MaterialExpressiveTheme` (`ui/theme/Theme.kt`), not `MaterialTheme` — this is deliberate, not a typo, and is what the project is standardized on for the expressive visual language (bouncier motion/shape defaults). Color scheme is hand-authored in `ui/theme/Color.kt` (not a Material Theme Builder export) with dynamic color support gated behind Android 12+.
