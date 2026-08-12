<div align="center">

  <img src="https://github.com/tapframe/NuvioTV/blob/main/assets/brand/app_logo_wordmark.png" alt="NuvioMobile Enhanced" width="320" />

  <h1>NuvioMobile Enhanced</h1>

  <p><strong>An independent continuation of NuvioMobile, maintained by AKRusso.</strong></p>

  <p>
    This fork keeps Nuvio up to date while adding improvements to the user experience,
    playback, navigation, tracking, and community support.
  </p>

  <p>
    <a href="https://github.com/AKRusso/NuvioMobile-Enhanced/releases/latest"><img src="https://img.shields.io/github/v/release/AKRusso/NuvioMobile-Enhanced?style=for-the-badge&label=Latest%20Release" alt="Latest release" /></a>
    <a href="https://github.com/AKRusso/NuvioMobile-Enhanced/releases"><img src="https://img.shields.io/github/downloads/AKRusso/NuvioMobile-Enhanced/total?style=for-the-badge&label=Downloads" alt="Downloads" /></a>
    <a href="https://github.com/AKRusso/NuvioMobile-Enhanced/actions/workflows/android-release.yml"><img src="https://img.shields.io/github/actions/workflow/status/AKRusso/NuvioMobile-Enhanced/android-release.yml?style=for-the-badge&label=Android%20Build" alt="Android build status" /></a>
    <a href="https://github.com/AKRusso/NuvioMobile-Enhanced/blob/enhanced/LICENSE"><img src="https://img.shields.io/github/license/AKRusso/NuvioMobile-Enhanced?style=for-the-badge" alt="License" /></a>
  </p>

  <p>
    <a href="#download">Download</a> |
    <a href="#what-i-maintain">What I maintain</a> |
    <a href="#features">Features</a> |
    <a href="#contributing">Contributing</a> |
    <a href="#credits-and-attribution">Credits</a>
  </p>

</div>

## Current Status

This repository is maintained by **AKRusso** as an independent continuation of
NuvioMobile Enhanced. The goal is to keep the fork aligned with the original
project, fix bugs, improve the Android experience, and provide easy-to-install
release builds.

| Track | Version |
| --- | --- |
| Nuvio Enhanced | `0.4.7 (111)` |
| Official NuvioMobile base | `0.4.4` |
| Fork main branch | [`enhanced`](https://github.com/AKRusso/NuvioMobile-Enhanced/tree/enhanced) |
| Maintainer | [AKRusso](https://github.com/AKRusso) |

The Enhanced version is kept separate from the official version so it is clear
which work comes from the original Nuvio project and which work belongs to this
fork.

## Download

### Android

Always download from the official fork release:

**[Download Nuvio Enhanced 0.4.7](https://github.com/AKRusso/NuvioMobile-Enhanced/releases/tag/0.4.7)**

For most Android phones, choose:

**[ARM64-v8a - recommended](https://github.com/AKRusso/NuvioMobile-Enhanced/releases/download/0.4.7/androidApp-full-arm64-v8a-release.apk)**

Other architectures are available on the release page:

- `armeabi-v7a`: older 32-bit Android devices.
- `x86_64`: 64-bit Intel emulators or compatible devices.
- `x86`: 32-bit Intel emulators or compatible devices.

Release APKs are built by GitHub Actions, use the update-compatible certificate
from previous Enhanced builds, and include SHA-256 hashes in the release notes.

## What I Maintain

- Keeping this fork aligned with stable NuvioMobile releases.
- Fixing bugs and regressions found on Android.
- Building and publishing signed Android releases through GitHub Actions.
- Improving navigation, playback, library, Live TV, and tracking.
- Maintaining clear documentation, changelogs, and release notes.
- Supporting the community through Ko-fi without storing private payment data.

## Features

| Area | Enhanced improvements |
| --- | --- |
| Playback | Android libmpv playback, tap-to-seek, progress synchronization, and stability improvements. |
| Live TV | M3U navigation, favorites, channel switching, filters, XMLTV EPG, and recent channels. |
| Tracking | Updated Trakt and Simkl authentication and synchronization flows. |
| Library | Release calendar, clearer status handling, and refined navigation. |
| AI assistant | Gemini, OpenRouter, Cerebras, and Groq integrations with formatted responses. |
| Community | Supporters, contributors, Ko-fi donations, and approved supporter avatars. |
| UX | More consistent visuals, smoother transitions, and less flicker on dynamic screens. |

## Roadmap

- Continue tracking official NuvioMobile releases.
- Fix community-reported issues and improve Android compatibility.
- Keep releases signed, verifiable, and easy to install.
- Improve technical documentation and contribution workflows.

Features may change as upstream evolves and community feedback arrives. Specific
changes are recorded in [`CHANGELOG.md`](CHANGELOG.md) and each release's notes.

## Support And Feedback

- [Report a bug](https://github.com/AKRusso/NuvioMobile-Enhanced/issues/new/choose)
- [View open issues](https://github.com/AKRusso/NuvioMobile-Enhanced/issues)
- [View builds and workflows](https://github.com/AKRusso/NuvioMobile-Enhanced/actions)
- [Support development on Ko-fi](https://ko-fi.com/nuvioenhanced)
- [Join the community Discord](https://discord.gg/at8xffxuRU)

When reporting an issue, include the Enhanced version, device architecture,
Android version, reproduction steps, and relevant logs without personal data.

## Build From Source

```bash
git clone https://github.com/AKRusso/NuvioMobile-Enhanced.git
cd NuvioMobile-Enhanced
git checkout enhanced
./gradlew :androidApp:assembleFullDebug
```

On Windows:

```powershell
git clone https://github.com/AKRusso/NuvioMobile-Enhanced.git
cd NuvioMobile-Enhanced
git checkout enhanced
.\gradlew.bat :androidApp:assembleFullDebug
```

To run the main validation tasks:

```powershell
.\gradlew.bat allTests :androidApp:lintFullDebug
```

Credentials, tokens, and private configuration must stay in `local.properties` or
GitHub Actions secrets. Never commit them to Git.

## Contributing

Pull requests and issues are welcome. Before contributing:

1. Confirm that the change belongs in Enhanced rather than the original upstream project.
2. Keep changes focused and explain the expected behavior.
3. Run the relevant tests and lint checks.
4. Update the changelog when the change affects users.
5. Never include tokens, passwords, credentials, or private files.

For larger changes, open an issue first so the direction can be discussed.

## Credits And Attribution

This is an independent community fork. **NuvioMobile Enhanced is not the original
project and does not speak on behalf of the upstream maintainers.**

- Fork maintainer: [AKRusso](https://github.com/AKRusso)
- Original project: [NuvioMedia/NuvioMobile](https://github.com/NuvioMedia/NuvioMobile)
- Upstream organization: [NuvioMedia](https://github.com/NuvioMedia)
- Brand asset used here: [tapframe/NuvioTV](https://github.com/tapframe/NuvioTV)

Fork-specific changes are documented in the Git history, changelog, and release
notes. Original code remains subject to its license and attribution requirements.

## Legal And DMCA

NuvioMobile Enhanced is a client-side interface for browsing metadata and playing
media through user-installed extensions and/or user-provided sources. Use it only
with content you own or are authorized to access.

The project does not host, store, or distribute media content and is not affiliated
with third-party extensions, catalogs, sources, or content providers.

- [Legal policy and disclaimer](https://nuvioapp.space/legal)
- [GPL-3.0 license](LICENSE)

## Star History

<a href="https://www.star-history.com/#AKRusso/NuvioMobile-Enhanced&type=date&legend=top-left">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=AKRusso/NuvioMobile-Enhanced&type=date&theme=dark&legend=top-left" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=AKRusso/NuvioMobile-Enhanced&type=date&legend=top-left" />
    <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=AKRusso/NuvioMobile-Enhanced&type=date&legend=top-left" />
  </picture>
</a>
