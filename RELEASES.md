# Android release setup

## Trakt

Create a Trakt API application and use this redirect URI:

```text
nuvioenhanced://auth/trakt
```

For local builds, add these values to `local.properties`:

```properties
TRAKT_CLIENT_ID=your_client_id
TRAKT_CLIENT_SECRET=your_client_secret
TRAKT_REDIRECT_URI=nuvioenhanced://auth/trakt
```

For GitHub Actions releases, add repository secrets named:

- `TRAKT_CLIENT_ID`
- `TRAKT_CLIENT_SECRET`

The workflow passes the secrets directly to Gradle and does not print their values.
Forks do not inherit Actions secrets, so every fork that publishes an APK must
configure its own credentials.

## Update source

GitHub Actions builds automatically use the owner of the repository running the
workflow. A local build can override the update source in `local.properties`:

```properties
NUVIO_UPDATE_GITHUB_OWNER=AKRusso
NUVIO_UPDATE_GITHUB_REPO=NuvioMobile-Enhanced
```

The app checks published, non-prerelease GitHub Releases at startup. A release is
offered only when it contains a compatible APK and has a version newer than the
installed app. The GitHub release body is displayed as the in-app changelog.

## Release checklist

1. Update `CHANGELOG.md`.
2. Increment `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` in
   `iosApp/Configuration/Version.xcconfig`.
3. Build and test `fullRelease`.
4. Confirm the release contents with the repository owner.
5. Run the Android release workflow in `publish` mode.
