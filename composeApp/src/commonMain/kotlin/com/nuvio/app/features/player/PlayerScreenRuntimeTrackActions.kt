package com.nuvio.app.features.player

internal val PlayerScreenRuntime.subtitleStyle: SubtitleStyleState
    get() = playerSettingsUiState.subtitleStyle

internal val PlayerScreenRuntime.activeAddonSubtitleType: String
    get() = contentType ?: parentMetaType

internal val PlayerScreenRuntime.addonSubtitleFetchKey: String?
    get() = buildAddonSubtitleFetchKey(
        addons = addonsUiState.addons,
        type = activeAddonSubtitleType,
        videoId = activeVideoId,
    )

internal val PlayerScreenRuntime.visibleAddonSubtitles: List<AddonSubtitle>
    get() = filterAddonSubtitlesForSettings(
        subtitles = addonSubtitles,
        settings = playerSettingsUiState,
    )

internal val PlayerScreenRuntime.selectedAddonSubtitle: AddonSubtitle?
    get() = visibleAddonSubtitles.firstOrNull { subtitle ->
        subtitle.selectionKey == selectedAddonSubtitleId || subtitle.url == selectedAddonSubtitleId
    }

internal fun PlayerScreenRuntime.updateTrackPreference(
    update: (PersistedPlayerTrackPreference) -> PersistedPlayerTrackPreference,
) {
    if (parentMetaId.isBlank()) return
    val current = PlayerTrackPreferenceStorage.load(parentMetaId) ?: PersistedPlayerTrackPreference()
    PlayerTrackPreferenceStorage.save(parentMetaId, update(current))
}

internal fun PlayerScreenRuntime.persistAudioPreference(track: AudioTrack?) {
    updateTrackPreference { current ->
        current.copy(
            audioLanguage = track?.language,
            audioName = track?.label,
            audioTrackId = track?.id,
        )
    }
}

internal fun PlayerScreenRuntime.persistInternalSubtitlePreference(track: SubtitleTrack?) {
    updateTrackPreference { current ->
        current.copy(
            subtitleType = if (track == null) {
                PersistedSubtitleSelectionType.DISABLED
            } else {
                PersistedSubtitleSelectionType.INTERNAL
            },
            subtitleLanguage = track?.language,
            subtitleName = track?.label,
            subtitleTrackId = track?.id,
            addonSubtitleId = null,
            addonSubtitleUrl = null,
            addonSubtitleAddonName = null,
        )
    }
}

internal fun PlayerScreenRuntime.persistAddonSubtitlePreference(subtitle: AddonSubtitle) {
    updateTrackPreference { current ->
        current.copy(
            subtitleType = PersistedSubtitleSelectionType.ADDON,
            subtitleLanguage = subtitle.language,
            subtitleName = subtitle.display,
            subtitleTrackId = null,
            addonSubtitleId = subtitle.selectionKey,
            // Add-on links are episode-scoped and often short-lived. Never restore this
            // URL for another episode; keep only the user's descriptive preference.
            addonSubtitleUrl = null,
            addonSubtitleAddonName = subtitle.addonName,
        )
    }
}

internal fun PlayerScreenRuntime.restorePersistedTrackPreferenceIfNeeded() {
    if (trackPreferenceRestoreApplied) return
    val preference = PlayerTrackPreferenceStorage.load(parentMetaId)
    if (preference == null) {
        trackPreferenceRestoreApplied = true
        return
    }

    if (
        audioTracks.isNotEmpty() &&
        (!preference.audioTrackId.isNullOrBlank() ||
            !preference.audioLanguage.isNullOrBlank() ||
            !preference.audioName.isNullOrBlank())
    ) {
        val restoredAudioIndex = findPersistedAudioTrackIndex(audioTracks, preference)
        if (restoredAudioIndex >= 0 && restoredAudioIndex != selectedAudioIndex) {
            playerController?.selectAudioTrack(restoredAudioIndex)
            selectedAudioIndex = restoredAudioIndex
        }
        preferredAudioSelectionApplied = true
    }

    when (preference.subtitleType) {
        PersistedSubtitleSelectionType.DISABLED -> {
            playerController?.selectSubtitleTrack(-1)
            selectedSubtitleIndex = -1
            selectedAddonSubtitleId = null
            useCustomSubtitles = false
            preferredSubtitleSelectionApplied = true
        }
        PersistedSubtitleSelectionType.INTERNAL -> {
            if (subtitleTracks.isNotEmpty()) {
                val restoredSubtitleIndex = findPersistedSubtitleTrackIndex(subtitleTracks, preference)
                if (restoredSubtitleIndex >= 0) {
                    if (useCustomSubtitles) {
                        playerController?.clearExternalSubtitleAndSelect(restoredSubtitleIndex)
                    } else {
                        playerController?.selectSubtitleTrack(restoredSubtitleIndex)
                    }
                    selectedSubtitleIndex = restoredSubtitleIndex
                    selectedAddonSubtitleId = null
                    useCustomSubtitles = false
                    preferredSubtitleSelectionApplied = true
                }
            }
        }
        PersistedSubtitleSelectionType.ADDON -> {
            // Add-on URLs and IDs are episode-scoped. Let the automatic policy evaluate
            // the current episode instead of reviving a prior episode's selection.
            selectedAddonSubtitleId = null
            useCustomSubtitles = false
        }
    }

    trackPreferenceRestoreApplied = true
}

internal fun PlayerScreenRuntime.refreshTracks() {
    val ctrl = playerController ?: return
    audioTracks = ctrl.getAudioTracks()
    subtitleTracks = ctrl.getSubtitleTracks()
    val selectedAudio = audioTracks.firstOrNull { it.isSelected }
    if (selectedAudio != null) selectedAudioIndex = selectedAudio.index
    val selectedSub = subtitleTracks.firstOrNull { it.isSelected }
    if (!useCustomSubtitles) {
        if (manualSubtitleSelectionLocked) {
            if (selectedSubtitleIndex < 0) {
                if (selectedSub != null) playerController?.selectSubtitleTrack(-1)
            } else if (selectedSub?.index != selectedSubtitleIndex) {
                playerController?.selectSubtitleTrack(selectedSubtitleIndex)
            }
        } else if (selectedSub != null) {
            selectedSubtitleIndex = selectedSub.index
        }
    }

    restorePersistedTrackPreferenceIfNeeded()

    val preferredAudioTargets = resolvePreferredAudioLanguageTargets(
        preferredAudioLanguage = playerSettingsUiState.preferredAudioLanguage,
        secondaryPreferredAudioLanguage = playerSettingsUiState.secondaryPreferredAudioLanguage,
        deviceLanguages = DeviceLanguagePreferences.preferredLanguageCodes(),
        contentOriginalLanguage = resolveContentLanguage(
            language = metaUiState.meta?.language,
            country = metaUiState.meta?.country,
        ) ?: args.contentLanguage,
    )

    if (!preferredAudioSelectionApplied) {
        if (preferredAudioTargets.isEmpty()) {
            preferredAudioSelectionApplied = true
        } else if (audioTracks.isNotEmpty()) {
            val preferredAudioIndex = findPreferredTrackIndex(
                tracks = audioTracks,
                targets = preferredAudioTargets,
                language = ::resolveAudioTrackLanguageTarget,
            )
            if (preferredAudioIndex >= 0 && preferredAudioIndex != selectedAudioIndex) {
                playerController?.selectAudioTrack(preferredAudioIndex)
                selectedAudioIndex = preferredAudioIndex
            }
            preferredAudioSelectionApplied = true
        }
    }

    if (!preferredSubtitleSelectionApplied) {
        val preferredSubtitleLanguage = normalizeLanguageCode(
            playerSettingsUiState.preferredSubtitleLanguage,
        )
        val preferredSubtitleTargets = if (
            preferredSubtitleLanguage == SubtitleLanguageOption.NONE ||
            preferredSubtitleLanguage == SubtitleLanguageOption.FORCED
        ) {
            emptyList()
        } else {
            resolvePreferredSubtitleLanguageTargets(
                preferredSubtitleLanguage = playerSettingsUiState.preferredSubtitleLanguage,
                secondaryPreferredSubtitleLanguage = playerSettingsUiState.secondaryPreferredSubtitleLanguage,
                deviceLanguages = DeviceLanguagePreferences.preferredLanguageCodes(),
            )
        }
        val selectedAudioTrack = audioTracks.firstOrNull { track -> track.index == selectedAudioIndex }
            ?: audioTracks.firstOrNull { it.isSelected }
        val selectionPlan = resolveSubtitleAutoSelectionPlan(
            selectedAudioLanguage = resolveAudioTrackLanguageTarget(selectedAudioTrack),
            preferredAudioTargets = preferredAudioTargets,
            preferredSubtitleTargets = preferredSubtitleTargets,
            useForcedSubtitles = subtitleStyle.useForcedSubtitles,
        )
        if (selectionPlan == null) {
            disableAutomaticSubtitleSelection()
            return
        }

        if (selectionPlan.targets.isEmpty()) {
            disableAutomaticSubtitleSelection()
            preferredSubtitleSelectionApplied = true
            autoAddonFallbackPending = false
        } else {
            val preferredSubtitleIndex = findPreferredSubtitleTrackIndex(
                tracks = subtitleTracks,
                targets = selectionPlan.targets,
                mode = selectionPlan.mode,
            )
            if (preferredSubtitleIndex >= 0 && preferredSubtitleIndex != selectedSubtitleIndex) {
                playerController?.selectSubtitleTrack(preferredSubtitleIndex)
                selectedSubtitleIndex = preferredSubtitleIndex
                selectedAddonSubtitleId = null
                useCustomSubtitles = false
                autoAddonFallbackPending = false
            } else if (preferredSubtitleIndex < 0) {
                if (
                    selectionPlan.mode == SubtitleAutoSelectionMode.FORCED_ONLY
                ) {
                    disableAutomaticSubtitleSelection()
                    autoAddonFallbackPending = false
                } else {
                    // The player has finished loading tracks and no normal built-in track
                    // matches. Add-ons are considered only after this point.
                    autoAddonFallbackPending = true
                }
            }
            preferredSubtitleSelectionApplied = true
        }
    }
}

private fun PlayerScreenRuntime.disableAutomaticSubtitleSelection() {
    if (selectedSubtitleIndex != -1 || subtitleTracks.any { it.isSelected }) {
        playerController?.selectSubtitleTrack(-1)
    }
    selectedSubtitleIndex = -1
    selectedAddonSubtitleId = null
    useCustomSubtitles = false
    autoAddonFallbackPending = false
}
