# Changelog

All notable Nuvio Enhanced changes are recorded here. GitHub release notes use the
same user-facing summary so the in-app updater can display it before download.

## 0.3.3 - 2026-07-27

### Changed

- Reorganized Enhanced settings into a compact, wrapping category selector.
- Added a dedicated New category for subtitle sync, player time, and episode shuffle.
- Removed decorative icons from the three new player settings.
- Made the update repository configurable for local builds and forks.
- Allowed release builds to read Trakt credentials from GitHub Actions secrets.

### Fixed

- New player settings are highlighted once per feature revision and stay dismissed
  after the user marks them as seen.
- Fork releases can be discovered by the existing in-app updater.
- Release builds no longer depend on Trakt credentials being stored only in
  `local.properties`.

### Upstream

- Includes NuvioMobile `cmp-rewrite` through commit `88d3cbdf`.
