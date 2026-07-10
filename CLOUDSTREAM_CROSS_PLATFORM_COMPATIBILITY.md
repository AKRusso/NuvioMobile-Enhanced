# CloudStream cross-platform compatibility

This document describes the experimental CloudStream repository compatibility work on
`feature/cloudstream-cross-platform-compatibility`. It is intentionally isolated from the
released `enhanced` branch.

## Verified package format

CloudStream repository manifests point to one or more `plugins.json` lists. Standard `.cs3`
packages in the Kraptor reference repository are ZIP archives containing `manifest.json` and
`classes.dex`. Current CloudStream loads the declared plugin class with Android's
`PathClassLoader`. The package is therefore Android/JVM executable code, not Kotlin
Multiplatform bytecode or a portable script format.

Nuvio Enhanced does not pretend that unchanged DEX can execute on iOS. iOS has no Android
class loader, and App Store Review Guideline 2.5.2 prohibits downloading code that changes app
functionality. Downloaded `.cs3` packages are treated as versioned, hash-verifiable compatibility
artifacts only. Runtime behavior is supplied by reviewed adapters compiled into the app for both
Android and iOS.

## Compatibility model

| Capability | Android full | iOS full |
| --- | --- | --- |
| Add standard repository URL | Supported | Supported |
| Parse `repo.json` and all `pluginLists` | Supported | Supported |
| List standard plugin metadata | Supported | Supported |
| Download unchanged `.cs3` to private storage | Supported | Supported |
| Verify `sha256-*` `fileHash` | Supported | Supported |
| Execute arbitrary downloaded `classes.dex` | Not supported | Not possible |
| Run a reviewed built-in cross-platform adapter | Supported | Supported |
| Show explicit compatibility for unsupported plugins | Supported | Supported |

The first reviewed adapter targets `KickTR` from `https://github.com/Kraptor123/cs-kraptor`.
It is implemented from the public service contract rather than copied or decompiled provider
source. The same shared provider models, search/detail flow, and playback source mapping are used
on both platforms.

## Security properties

- No repository is bundled or silently installed.
- Repository addition and plugin installation require explicit user actions.
- Package downloads use temporary files and are committed only after validation.
- A provided SHA-256 hash is mandatory for a verified installation.
- Archive paths are validated before a package is accepted.
- Package files stay in private app storage.
- Downloaded DEX is never executed.
- Cookies, authorization headers, and tokens are excluded from logs.
- One provider failure is isolated from other providers.

## Integration direction

The common layer owns repository parsing, plugin identity, status/version handling, compatibility,
provider models, and Nuvio model conversion. Full-distribution implementations own persistence,
package storage, and network-backed provider adapters. Play Store/App Store source sets expose
disabled stubs so store builds cannot accidentally ship the experimental runtime.

CloudStream details are converted to Nuvio `MetaDetails`. Resolved links are embedded as existing
`StreamItem` values, preserving request headers, referer, subtitles, HLS/DASH flags, and source
labels. This deliberately reuses the existing Android and iOS player pipeline.

## Review workflow

Review and test this branch directly. Do not merge it into `enhanced` until Android full and iOS
full device validation has been completed and the compatibility limitations are accepted. A later
pull request should remain review-only until that approval is explicit.
