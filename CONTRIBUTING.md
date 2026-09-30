# Contributing to Mochi

Thanks for your interest in contributing — bug reports, feature ideas, and pull requests are all welcome.

## Getting set up

See the ["Building from source"](README.md#-building-from-source) section of the README for cloning, registering a MyAnimeList API client, and building the project. Take a look at [CLAUDE.md](CLAUDE.md) too — it covers the project's architecture and the non-obvious conventions worth knowing before you touch auth, networking, or navigation code.

## Reporting bugs / requesting features

Open a [GitHub Issue](../../issues). For bugs, include:

- What you did and what you expected to happen
- What actually happened (screenshots/screen recordings help a lot for UI issues)
- Your Android version and device (or emulator) if it seems device-specific
- Relevant logcat output (`FATAL EXCEPTION` traces are the most useful part — see the README/CLAUDE.md for what to filter for)

## Making changes

1. Fork the repo and create a branch off `main`.
2. Keep pull requests focused — one fix or feature per PR is much easier to review than a bundle of unrelated changes.
3. Match the existing code style:
   - Kotlin, idiomatic Compose (state hoisted into `@HiltViewModel`s, screens are thin).
   - New repository/API calls that hit the network go through `safeCall { }` (`data/error/SafeCall.kt`) so failures map to a `MochiError` with a message that's already safe to show in a snackbar.
   - New DTOs go in `data/api/`, domain models in `data/model/`, with an explicit mapper — don't let a DTO leak into the `ui/` layer.
   - Comments explain *why*, not *what* — skip comments that just restate the code.
4. Build and sanity-check on a device or emulator before opening the PR (`./gradlew assembleDebug`). There's no automated test suite yet, so manual verification of the flow you touched matters.
5. Open the PR against `main` with a clear description of what changed and why. Link any related issue.

## What's especially useful right now

- Testing on a range of real devices/OEM skins (there's already been at least one report of an OEM (ColorOS) system-service log line being mistaken for an app bug — device-specific quirks are worth flagging).
- Anything under the "known gaps" umbrella: no automated tests yet, no CI build-verification, ProGuard/R8 rules for release builds are new and could use real-device testing.
- Accessibility passes on the Compose screens.

## License

By contributing, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).
