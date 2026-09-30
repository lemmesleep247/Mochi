# Changelog

All notable changes to Mochi will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Each GitHub Release's notes are generated from the section of this file matching its tag —
see `.github/workflows/release.yml`.

## [Unreleased]

### Added
- MyAnimeList sign-in via OAuth2 + PKCE, opened in an external browser.
- My List: synced anime/manga library, status filter chips, quick +1 progress.
- Discover: genre-filtered search across anime and manga via the Tenrai API.
- Title detail screen with a full status/progress/score editor.
- Profile: anime stats from the official API, plus manga stats via an optional
  MAL website sign-in (scraped, since the API doesn't expose them).
- Material 3 Expressive theming, light and dark.
