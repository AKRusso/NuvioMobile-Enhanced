# Changelog

All notable Nuvio Enhanced changes are recorded here. GitHub release notes use the
same user-facing summary so the in-app updater can display it before download.

## 0.4.5 - 2026-08-11

### Added

- Added Ko-fi donations at `ko-fi.com/nuvioenhanced`, with recent public
  supporters, approved avatars, profiles, messages, and supporter totals in the
  existing Supporters page.
- Added recent supporter avatars and direct Supporters navigation to the Trakt
  card when sponsored Trakt access is unavailable.
- Added a Cloudflare Worker and D1 backend for verified Ko-fi webhooks, public
  donation data, contributor data, profile administration, and privacy removals.
- Added release-safe public defaults for the donation, contributor, and Ko-fi
  URLs without embedding verification or administration secrets in the app.

### Changed

- Integrated the official Nuvio `0.4.4` codebase while retaining Enhanced
  features and adopting the current Trakt and Simkl tracking architecture.
- Displayed the Enhanced release version separately from the official Nuvio base
  version in Settings: Nuvio Enhanced `0.4.5`, based on Nuvio `0.4.4`.
- Opened Supporters by default and loaded each community tab only when needed.
- Updated the Cloudflare deployment toolchain to a supported release with no
  known npm audit vulnerabilities.

### Fixed

- Fixed Supporters navigation from the Trakt card when using native phone
  settings navigation.
- Hid monthly donation progress when Ko-fi does not provide a real monthly goal,
  instead of presenting an artificial zero-percent target.
- Hardened the webhook with bounded streaming input, constant-time token checks,
  idempotent inserts, approved avatar hosts, and authenticated donation removal.
- Stored only public donation names, messages, dates, and approved profile data;
  email addresses, amounts, payment data, and raw webhook payloads are not stored.

### Upstream

- Includes NuvioMobile `0.4.4` through commit `f9ad843b`.

## 0.4.4 - 2026-08-11

### Changed

- Integrated NuvioMobile 0.4.4 while retaining the Enhanced settings, Live TV,
  release radar, library health, subtitle sync, player clock, and episode shuffle.
- Adopted the new provider-neutral tracking and library architecture, including
  Trakt and Simkl support.
- Updated the Android application structure, plugin runtime, media engine, and
  notification services to the latest upstream implementations.
- Restored the complete Tracking page with Trakt and Simkl, Downloads navigation,
  native settings transitions, source-aware Home behavior, and upstream animations.
- Restored the original app icon picker with all six icon colorways.

### Fixed

- Preserved immediate subtitle synchronization, custom subtitle fonts, volume
  boost, memory-safe buffering, exact seeking, and stable libmpv playback.
- Reloaded Enhanced and Live TV settings correctly when switching profiles.
- Restored Enhanced Live TV navigation and incoming playlist, stream, and magnet
  routing after the upstream navigation migration.
- Kept the Enhanced Android package, launcher, signing continuity, notification
  routing, iOS bundle identifiers, primary icon, and packaged Compose resources.
- Reconciled the Enhanced Home experience with the latest upstream caching and
  next-up behavior.
- Restored upstream PiP control handling, parental-guide behavior, autoplay
  fallback preference, transient Next Up retries, and Hebrew language selection.

### Upstream

- Includes NuvioMobile `0.4.4` through commit `f9ad843b`.

## 0.3.4 - 2026-07-27

### Changed

- Reorganized Enhanced settings into a compact, wrapping category selector.
- Added a dedicated New category for subtitle sync, player time, and episode shuffle.
- Removed decorative icons from the three new player settings.
- Added a translated setting to choose between the Enhanced and Nuvio subtitle
  selectors.
- Kept subtitle synchronization as a dedicated player action instead of showing
  it again inside the subtitle selector.
- Made the Nuvio Enhanced artwork the default application icon.
- Removed the alternate app icon picker and added a one-time migration back to
  the default icon to avoid launcher component conflicts.
- Made the update repository configurable for local builds and forks.
- Pointed release badges, in-app project links, and default update checks to
  `AKRusso/NuvioMobile-Enhanced`.
- Allowed release builds to read Trakt credentials from GitHub Actions secrets.

### Fixed

- New player settings are highlighted once per feature revision and stay dismissed
  after the user marks them as seen.
- Fork releases can be discovered by the existing in-app updater.
- Release builds no longer depend on Trakt credentials being stored only in
  `local.properties`.
- Release publishing now stops when an APK is not signed with the certificate
  used by existing Nuvio Enhanced installations.
- The player clock now respects the top safe area, leaves more room below header
  actions, and uses stronger contrast over video.
- Expanded navigation no longer resets to Adaptive when an older sync payload
  does not contain a navigation style.
- Selecting a subtitle line in Sync now applies it immediately at the captured
  playback position.
- The player clock now stays directly below the header actions without overlapping
  them on different screen sizes.
- Restored smooth hero artwork, metadata, and indicator transitions while swiping
  or auto-advancing.

### Upstream

- Includes NuvioMobile `cmp-rewrite` through commit `88d3cbdf`.
