@file:Suppress("DEPRECATION") // LocalClipboardManager is the clipboard API exposed by this Compose dependency.

package com.nuvio.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.build.TrailerPlaybackMode
import com.nuvio.app.core.diagnostics.CrashDiagnostics
import com.nuvio.app.core.network.DnsOverHttpsProvider
import com.nuvio.app.core.network.DnsOverHttpsSettingsRepository
import com.nuvio.app.core.sync.ProfileSettingsSync
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.appIconPainter
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import com.nuvio.app.features.downloads.DownloadsExternalFolderPlatform
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.player.AndroidPlaybackEngine
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.isIos
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.*
import nuvio.composeapp.generated.resources.settings_advanced_doh_description
import nuvio.composeapp.generated.resources.settings_advanced_doh_selected
import nuvio.composeapp.generated.resources.settings_advanced_hero_auto_scroll
import nuvio.composeapp.generated.resources.settings_advanced_hero_auto_scroll_description
import nuvio.composeapp.generated.resources.settings_advanced_hero_motion_preview
import nuvio.composeapp.generated.resources.settings_advanced_hero_motion_preview_description
import nuvio.composeapp.generated.resources.settings_meta_show_episode_ratings
import nuvio.composeapp.generated.resources.settings_meta_show_episode_ratings_description
import nuvio.composeapp.generated.resources.settings_nuvio_enhanced_title
import org.jetbrains.compose.resources.stringResource

private const val NuvioEnhancedGithubUrl = "https://github.com/AKRusso/NuvioMobile-Enhanced"
private const val NuvioEnhancedDiscordUrl = "https://discord.gg/at8xffxuRU"

private enum class EnhancedSettingsCategory {
    New,
    All,
    Core,
    Home,
    Player,
    System,
}

internal fun LazyListScope.nuvioEnhancedSettingsContent(
    isTablet: Boolean,
) {
    item {
        NuvioEnhancedSettingsPageContent(isTablet = isTablet)
    }
}

@Composable
private fun NuvioEnhancedSettingsPageContent(
    isTablet: Boolean,
) {
    val settings by remember {
        NuvioEnhancedSettingsRepository.ensureLoaded()
        NuvioEnhancedSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val homeSettings by remember {
        HomeCatalogSettingsRepository.snapshot()
        HomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val dnsOverHttpsSettings by remember {
        DnsOverHttpsSettingsRepository.ensureLoaded()
        DnsOverHttpsSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val detailSettings by remember {
        MetaScreenSettingsRepository.ensureLoaded()
        MetaScreenSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val playerSettings by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val externalFolderState by DownloadsExternalFolderPlatform.state.collectAsStateWithLifecycle()
    val lastCrashReport by remember {
        CrashDiagnostics.lastReport
    }.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var backupPayload by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importPayload by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    val hasNewPlayerTools = settings.isNew(NuvioEnhancedFeature.SubtitleSyncMenu) ||
        settings.isNew(NuvioEnhancedFeature.SubtitleSelectorStyle) ||
        settings.isNew(NuvioEnhancedFeature.AudioSelectorStyle) ||
        settings.isNew(NuvioEnhancedFeature.NextEpisodeButton) ||
        settings.isNew(NuvioEnhancedFeature.PlayerTimeOverlay) ||
        settings.isNew(NuvioEnhancedFeature.PersistentEpisodeShuffle)
    var selectedCategory by rememberSaveable {
        mutableStateOf(
            if (hasNewPlayerTools) {
                EnhancedSettingsCategory.New
            } else {
                EnhancedSettingsCategory.Core
            },
        )
    }
    val backupImportedMessage = stringResource(Res.string.nuvio_enhanced_toast_backup_imported)
    val invalidBackupPayloadMessage = stringResource(Res.string.nuvio_enhanced_toast_invalid_backup)
    val backupFileReadyMessage = stringResource(Res.string.nuvio_enhanced_toast_backup_ready)
    val backupExportFailedMessage = stringResource(Res.string.nuvio_enhanced_toast_backup_export_failed)
    val backupImportFailedMessage = stringResource(Res.string.nuvio_enhanced_toast_backup_import_failed)
    val backupCopiedMessage = stringResource(Res.string.nuvio_enhanced_toast_backup_copied)
    val crashCopiedMessage = stringResource(Res.string.nuvio_enhanced_toast_crash_copied)
    val externalFolderFailedMessage = stringResource(Res.string.nuvio_enhanced_external_folder_failed)
    val homeHeroVideoPreviewSupported = AppFeaturePolicy.heroTrailerPlaybackSupported &&
        AppFeaturePolicy.trailerPlaybackMode == TrailerPlaybackMode.IN_APP
    val detailHeroTrailerPlaybackSupported = AppFeaturePolicy.heroTrailerPlaybackSupported &&
        AppFeaturePolicy.trailerPlaybackMode == TrailerPlaybackMode.IN_APP

    LaunchedEffect(hasNewPlayerTools, selectedCategory) {
        if (!hasNewPlayerTools && selectedCategory == EnhancedSettingsCategory.New) {
            selectedCategory = EnhancedSettingsCategory.Player
        }
    }

    fun isNew(feature: NuvioEnhancedFeature): Boolean = settings.isNew(feature)
    fun markSeen(feature: NuvioEnhancedFeature) {
        NuvioEnhancedSettingsRepository.markFeatureSeen(feature)
    }
    fun importBackupPayload(payload: String) {
        ProfileSettingsSync.importBackupJson(payload)
            .onSuccess {
                NuvioToastController.show(backupImportedMessage)
                showImportDialog = false
                importPayload = ""
                importError = null
            }
            .onFailure { error ->
                importError = error.message ?: invalidBackupPayloadMessage
                NuvioToastController.show(importError ?: invalidBackupPayloadMessage)
            }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nuvio.spacing.listGap),
    ) {
        SettingsSection(
            title = stringResource(Res.string.settings_nuvio_enhanced_title),
            isTablet = isTablet,
        ) {
            EnhancedIntroCard(
                isTablet = isTablet,
                hasNewFeatures = settings.hasNewFeatures,
                onMarkAllSeen = NuvioEnhancedSettingsRepository::markAllFeaturesSeen,
            )
        }

        EnhancedSettingsCategoryBar(
            selected = selectedCategory,
            hasNewFeatures = hasNewPlayerTools,
            onSelected = { selectedCategory = it },
        )

        if (selectedCategory == EnhancedSettingsCategory.All ||
            selectedCategory == EnhancedSettingsCategory.Core
        ) {
        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_nuvio_experience),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_home_features_title),
                    description = stringResource(Res.string.nuvio_enhanced_home_features_desc),
                    checked = settings.enhancedHomeFeaturesEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        NuvioEnhancedSettingsRepository.setEnhancedHomeFeaturesEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_concierge_title),
                    description = stringResource(Res.string.nuvio_enhanced_concierge_desc),
                    checked = settings.nuvioConciergeEnabled,
                    enabled = settings.enhancedHomeFeaturesEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        NuvioEnhancedSettingsRepository.setNuvioConciergeEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_smart_resume_title),
                    description = stringResource(Res.string.nuvio_enhanced_smart_resume_desc),
                    checked = settings.smartResumeEnabled,
                    enabled = settings.enhancedHomeFeaturesEnabled && settings.nuvioConciergeEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.SmartResume2),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.SmartResume2)
                        NuvioEnhancedSettingsRepository.setSmartResumeEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_continue_watching_ready_badge_title),
                    description = stringResource(Res.string.settings_continue_watching_ready_badge_description),
                    checked = settings.showContinueWatchingReadyBadge,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        NuvioEnhancedSettingsRepository.setShowContinueWatchingReadyBadge(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_release_signals_title),
                    description = stringResource(Res.string.nuvio_enhanced_release_signals_desc),
                    checked = settings.releaseRadarHomeSignalsEnabled,
                    enabled = settings.enhancedHomeFeaturesEnabled && settings.nuvioConciergeEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        NuvioEnhancedSettingsRepository.setReleaseRadarHomeSignalsEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_profile_stats_title),
                    description = stringResource(Res.string.nuvio_enhanced_profile_stats_desc),
                    checked = settings.profileStatsEnabled,
                    enabled = settings.enhancedHomeFeaturesEnabled && settings.nuvioConciergeEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        NuvioEnhancedSettingsRepository.setProfileStatsEnabled(it)
                    },
                )
            }
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_premium_labs),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_smart_shelf_title),
                    description = stringResource(Res.string.nuvio_enhanced_smart_shelf_desc),
                    checked = settings.smartShelvesEnabled,
                    enabled = settings.enhancedHomeFeaturesEnabled && !settings.quietHomeModeEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.SmartShelfComposer),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.SmartShelfComposer)
                        NuvioEnhancedSettingsRepository.setSmartShelvesEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_release_digest_title),
                    description = stringResource(Res.string.nuvio_enhanced_release_digest_desc),
                    checked = settings.releaseRadarDigestEnabled,
                    enabled = settings.enhancedHomeFeaturesEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.ReleaseRadarDigest),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.ReleaseRadarDigest)
                        NuvioEnhancedSettingsRepository.setReleaseRadarDigestEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_quiet_home_title),
                    description = stringResource(Res.string.nuvio_enhanced_quiet_home_desc),
                    checked = settings.quietHomeModeEnabled,
                    enabled = settings.enhancedHomeFeaturesEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.QuietHomeMode),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.QuietHomeMode)
                        NuvioEnhancedSettingsRepository.setQuietHomeModeEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_library_health_title),
                    description = stringResource(Res.string.nuvio_enhanced_library_health_desc),
                    checked = settings.libraryHealthEnabled,
                    enabled = settings.enhancedHomeFeaturesEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.LibraryHealth),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.LibraryHealth)
                        NuvioEnhancedSettingsRepository.setLibraryHealthEnabled(it)
                    },
                )
            }
        }

        }

        if (selectedCategory == EnhancedSettingsCategory.All ||
            selectedCategory == EnhancedSettingsCategory.System
        ) {
        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_app_experience),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_status_bar_title),
                    description = stringResource(Res.string.nuvio_enhanced_status_bar_desc),
                    checked = settings.statusBarVisible,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.StatusBarVisibility),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.StatusBarVisibility)
                        NuvioEnhancedSettingsRepository.setStatusBarVisible(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_live_tv_title),
                    description = stringResource(Res.string.nuvio_enhanced_live_tv_desc),
                    checked = settings.liveTvEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.LiveTvControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.LiveTvControls)
                        NuvioEnhancedSettingsRepository.setLiveTvEnabled(it)
                    },
                )
            }
        }
        }

        if (selectedCategory == EnhancedSettingsCategory.New ||
            selectedCategory == EnhancedSettingsCategory.All ||
            selectedCategory == EnhancedSettingsCategory.Player
        ) {
            SettingsSection(
                title = stringResource(Res.string.nuvio_enhanced_section_player_tools),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    if (selectedCategory != EnhancedSettingsCategory.New) {
                        SettingsSwitchRow(
                            title = stringResource(Res.string.settings_playback_parental_guide),
                            description = stringResource(Res.string.settings_playback_parental_guide_description),
                            checked = playerSettings.showParentalGuide,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioEnhancedFeature.ContentWarnings),
                            onCheckedChange = {
                                markSeen(NuvioEnhancedFeature.ContentWarnings)
                                PlayerSettingsRepository.setShowParentalGuide(it)
                            },
                        )
                        SettingsGroupDivider(isTablet = isTablet)
                        SettingsSwitchRow(
                            title = stringResource(Res.string.nuvio_enhanced_source_pinning_title),
                            description = stringResource(Res.string.nuvio_enhanced_source_pinning_desc),
                            checked = settings.streamSourcePinningEnabled,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioEnhancedFeature.StreamSourcePinning),
                            onCheckedChange = {
                                markSeen(NuvioEnhancedFeature.StreamSourcePinning)
                                NuvioEnhancedSettingsRepository.setStreamSourcePinningEnabled(it)
                            },
                        )
                        SettingsGroupDivider(isTablet = isTablet)
                        SettingsSwitchRow(
                            title = stringResource(Res.string.nuvio_enhanced_background_stream_prefetch_title),
                            description = stringResource(Res.string.nuvio_enhanced_background_stream_prefetch_desc),
                            checked = settings.backgroundStreamPrefetchEnabled,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioEnhancedFeature.BackgroundStreamPrefetch),
                            onCheckedChange = {
                                markSeen(NuvioEnhancedFeature.BackgroundStreamPrefetch)
                                NuvioEnhancedSettingsRepository.setBackgroundStreamPrefetchEnabled(it)
                            },
                        )
                        SettingsGroupDivider(isTablet = isTablet)
                        SettingsSwitchRow(
                            title = stringResource(Res.string.nuvio_enhanced_player_status_overlay_title),
                            description = stringResource(Res.string.nuvio_enhanced_player_status_overlay_desc),
                            checked = settings.playerStatusOverlayEnabled,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioEnhancedFeature.PlayerStatusOverlay),
                            onCheckedChange = {
                                markSeen(NuvioEnhancedFeature.PlayerStatusOverlay)
                                NuvioEnhancedSettingsRepository.setPlayerStatusOverlayEnabled(it)
                            },
                        )
                        SettingsGroupDivider(isTablet = isTablet)
                    }
                    if (selectedCategory != EnhancedSettingsCategory.New || isNew(NuvioEnhancedFeature.SubtitleSyncMenu)) {
                    SettingsSwitchRow(
                        title = stringResource(Res.string.nuvio_enhanced_subtitle_sync_title),
                        description = stringResource(Res.string.nuvio_enhanced_subtitle_sync_desc),
                        checked = playerSettings.subtitleSyncMenuEnabled,
                        enabled = settings.enhancedHomeFeaturesEnabled,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.SubtitleSyncMenu),
                        onCheckedChange = {
                            markSeen(NuvioEnhancedFeature.SubtitleSyncMenu)
                            PlayerSettingsRepository.setSubtitleSyncMenuEnabled(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    }
                    if (selectedCategory != EnhancedSettingsCategory.New || isNew(NuvioEnhancedFeature.SubtitleSelectorStyle)) {
                    EnhancedChoiceRow(
                        title = stringResource(Res.string.nuvio_enhanced_subtitle_selector_title),
                        description = stringResource(Res.string.nuvio_enhanced_subtitle_selector_desc),
                        selected = settings.subtitleSelectorStyle,
                        options = listOf(
                            EnhancedChoiceOption(
                                NuvioSubtitleSelectorStyle.Enhanced,
                                stringResource(Res.string.nuvio_enhanced_subtitle_selector_enhanced),
                            ),
                            EnhancedChoiceOption(
                                NuvioSubtitleSelectorStyle.Nuvio,
                                stringResource(Res.string.nuvio_enhanced_subtitle_selector_nuvio),
                            ),
                        ),
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.SubtitleSelectorStyle),
                        onSelected = {
                            markSeen(NuvioEnhancedFeature.SubtitleSelectorStyle)
                            NuvioEnhancedSettingsRepository.setSubtitleSelectorStyle(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    }
                    if (selectedCategory != EnhancedSettingsCategory.New || isNew(NuvioEnhancedFeature.AudioSelectorStyle)) {
                    EnhancedChoiceRow(
                        title = stringResource(Res.string.nuvio_enhanced_audio_selector_title),
                        description = stringResource(Res.string.nuvio_enhanced_audio_selector_desc),
                        selected = settings.audioSelectorStyle,
                        options = listOf(
                            EnhancedChoiceOption(
                                NuvioAudioSelectorStyle.Enhanced,
                                stringResource(Res.string.nuvio_enhanced_subtitle_selector_enhanced),
                            ),
                            EnhancedChoiceOption(
                                NuvioAudioSelectorStyle.Nuvio,
                                stringResource(Res.string.nuvio_enhanced_subtitle_selector_nuvio),
                            ),
                        ),
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.AudioSelectorStyle),
                        onSelected = {
                            markSeen(NuvioEnhancedFeature.AudioSelectorStyle)
                            NuvioEnhancedSettingsRepository.setAudioSelectorStyle(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    }
                    if (selectedCategory != EnhancedSettingsCategory.New || isNew(NuvioEnhancedFeature.PlayerTimeOverlay)) {
                    SettingsSwitchRow(
                        title = stringResource(Res.string.nuvio_enhanced_player_clock_title),
                        description = stringResource(Res.string.nuvio_enhanced_player_clock_desc),
                        checked = playerSettings.playerClockEndTimeEnabled,
                        enabled = settings.enhancedHomeFeaturesEnabled,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.PlayerTimeOverlay),
                        onCheckedChange = {
                            markSeen(NuvioEnhancedFeature.PlayerTimeOverlay)
                            PlayerSettingsRepository.setPlayerClockEndTimeEnabled(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    }
                    if (selectedCategory != EnhancedSettingsCategory.New || isNew(NuvioEnhancedFeature.PersistentEpisodeShuffle)) {
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_playback_random_next_episode),
                        description = stringResource(Res.string.settings_playback_random_next_episode_description),
                        checked = playerSettings.randomNextEpisodeEnabled,
                        enabled = settings.enhancedHomeFeaturesEnabled,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.PersistentEpisodeShuffle),
                        onCheckedChange = {
                            markSeen(NuvioEnhancedFeature.PersistentEpisodeShuffle)
                            PlayerSettingsRepository.setRandomNextEpisodeEnabled(it)
                        },
                    )
                    }
                    if (selectedCategory != EnhancedSettingsCategory.New || isNew(NuvioEnhancedFeature.NextEpisodeButton)) {
                        if (selectedCategory != EnhancedSettingsCategory.New || isNew(NuvioEnhancedFeature.PersistentEpisodeShuffle)) {
                            SettingsGroupDivider(isTablet = isTablet)
                        }
                        SettingsSwitchRow(
                            title = stringResource(Res.string.nuvio_enhanced_next_episode_button_title),
                            description = stringResource(Res.string.nuvio_enhanced_next_episode_button_desc),
                            checked = settings.nextEpisodeButtonEnabled,
                            enabled = settings.enhancedHomeFeaturesEnabled,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioEnhancedFeature.NextEpisodeButton),
                            onCheckedChange = {
                                markSeen(NuvioEnhancedFeature.NextEpisodeButton)
                                NuvioEnhancedSettingsRepository.setNextEpisodeButtonEnabled(it)
                            },
                        )
                    }
                    if (selectedCategory != EnhancedSettingsCategory.New && !isIos) {
                        SettingsGroupDivider(isTablet = isTablet)
                        SettingsSwitchRow(
                            title = stringResource(Res.string.settings_playback_android_memory_safe_buffer),
                            description = stringResource(Res.string.settings_playback_android_memory_safe_buffer_description),
                            checked = playerSettings.androidMemorySafeBufferEnabled,
                            enabled = !playerSettings.externalPlayerEnabled &&
                                playerSettings.androidPlaybackEngine != AndroidPlaybackEngine.Libmpv,
                            isTablet = isTablet,
                            highlighted = isNew(NuvioEnhancedFeature.PlayerStatusOverlay),
                            onCheckedChange = PlayerSettingsRepository::setAndroidMemorySafeBufferEnabled,
                        )
                    }
                }
            }
        }

        if (selectedCategory == EnhancedSettingsCategory.All ||
            selectedCategory == EnhancedSettingsCategory.Home
        ) {
        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_hero_experience),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                EnhancedChoiceRow(
                    title = stringResource(Res.string.nuvio_enhanced_hero_display_title),
                    description = stringResource(Res.string.nuvio_enhanced_hero_display_desc),
                    selected = settings.heroDisplayMode,
                    options = listOf(
                        EnhancedChoiceOption(
                            NuvioHeroDisplayMode.Cinematic,
                            stringResource(Res.string.nuvio_enhanced_hero_mode_cinematic),
                        ),
                        EnhancedChoiceOption(
                            NuvioHeroDisplayMode.Balanced,
                            stringResource(Res.string.nuvio_enhanced_hero_mode_balanced),
                        ),
                        EnhancedChoiceOption(
                            NuvioHeroDisplayMode.InfoRich,
                            stringResource(Res.string.nuvio_enhanced_hero_mode_info_rich),
                        ),
                    ),
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                    onSelected = {
                        markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                        NuvioEnhancedSettingsRepository.setHeroDisplayMode(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                EnhancedChoiceRow(
                    title = stringResource(Res.string.nuvio_enhanced_hero_artwork_title),
                    description = stringResource(Res.string.nuvio_enhanced_hero_artwork_desc),
                    selected = settings.heroArtworkSource,
                    options = listOf(
                        EnhancedChoiceOption(
                            NuvioHeroArtworkSource.Backdrop,
                            stringResource(Res.string.nuvio_enhanced_hero_artwork_backdrop),
                        ),
                        EnhancedChoiceOption(
                            NuvioHeroArtworkSource.Poster,
                            stringResource(Res.string.nuvio_enhanced_hero_artwork_poster),
                        ),
                    ),
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                    onSelected = {
                        markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                        NuvioEnhancedSettingsRepository.setHeroArtworkSource(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_poster_hero_title),
                    description = stringResource(Res.string.nuvio_enhanced_poster_hero_desc),
                    checked = settings.posterArtHeroEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                        NuvioEnhancedSettingsRepository.setPosterArtHeroEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_showcase_hero_title),
                    description = stringResource(Res.string.nuvio_enhanced_showcase_hero_desc),
                    checked = settings.streamingShowcaseHeroEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                        NuvioEnhancedSettingsRepository.setStreamingShowcaseHeroEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_showcase_video_preview_title),
                    description = stringResource(Res.string.nuvio_enhanced_showcase_video_preview_desc),
                    checked = settings.streamingShowcaseVideoPreviewEnabled,
                    enabled = settings.streamingShowcaseHeroEnabled && homeHeroVideoPreviewSupported,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                        NuvioEnhancedSettingsRepository.setStreamingShowcaseVideoPreviewEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_showcase_video_preview_sound_title),
                    description = stringResource(Res.string.nuvio_enhanced_showcase_video_preview_sound_desc),
                    checked = settings.streamingShowcaseVideoPreviewSoundEnabled,
                    enabled = settings.streamingShowcaseHeroEnabled &&
                        settings.streamingShowcaseVideoPreviewEnabled &&
                        homeHeroVideoPreviewSupported,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                        NuvioEnhancedSettingsRepository.setStreamingShowcaseVideoPreviewSoundEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_compact_hero_title),
                    description = stringResource(Res.string.nuvio_enhanced_compact_hero_desc),
                    checked = settings.compactHeroMetadata,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                        NuvioEnhancedSettingsRepository.setCompactHeroMetadata(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_hero_ratings_title),
                    description = stringResource(Res.string.nuvio_enhanced_hero_ratings_desc),
                    checked = settings.showHeroRatings,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                        NuvioEnhancedSettingsRepository.setShowHeroRatings(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_hero_overview_title),
                    description = stringResource(Res.string.nuvio_enhanced_hero_overview_desc),
                    checked = settings.showHeroOverview,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                        NuvioEnhancedSettingsRepository.setShowHeroOverview(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_hero_refresh_haptics_title),
                    description = stringResource(Res.string.nuvio_enhanced_hero_refresh_haptics_desc),
                    checked = settings.heroRefreshHapticsEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                        NuvioEnhancedSettingsRepository.setHeroRefreshHapticsEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_advanced_hero_auto_scroll),
                    description = stringResource(Res.string.settings_advanced_hero_auto_scroll_description),
                    checked = homeSettings.heroAutoScrollEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                        HomeCatalogSettingsRepository.setHeroAutoScrollEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_advanced_hero_motion_preview),
                    description = stringResource(Res.string.settings_advanced_hero_motion_preview_description),
                    checked = homeSettings.heroMotionPreviewEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                        HomeCatalogSettingsRepository.setHeroMotionPreviewEnabled(it)
                    },
                )
            }
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_release_radar),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_library_only_radar_title),
                    description = stringResource(Res.string.nuvio_enhanced_library_only_radar_desc),
                    checked = settings.releaseRadarLibraryOnly,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.ReleaseRadarFilters),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.ReleaseRadarFilters)
                        NuvioEnhancedSettingsRepository.setReleaseRadarLibraryOnly(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                EnhancedChoiceRow(
                    title = stringResource(Res.string.nuvio_enhanced_radar_window_title),
                    description = stringResource(Res.string.nuvio_enhanced_radar_window_desc),
                    selected = settings.releaseRadarWindowDays,
                    options = listOf(
                        EnhancedChoiceOption(7, stringResource(Res.string.nuvio_enhanced_radar_window_7_days)),
                        EnhancedChoiceOption(30, stringResource(Res.string.nuvio_enhanced_radar_window_30_days)),
                    ),
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.ReleaseRadarFilters),
                    onSelected = {
                        markSeen(NuvioEnhancedFeature.ReleaseRadarFilters)
                        NuvioEnhancedSettingsRepository.setReleaseRadarWindowDays(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                EnhancedChoiceRow(
                    title = stringResource(Res.string.nuvio_enhanced_radar_content_title),
                    description = stringResource(Res.string.nuvio_enhanced_radar_content_desc),
                    selected = settings.releaseRadarContentFilter,
                    options = listOf(
                        EnhancedChoiceOption(
                            NuvioReleaseRadarContentFilter.All,
                            stringResource(Res.string.nuvio_enhanced_filter_all),
                        ),
                        EnhancedChoiceOption(
                            NuvioReleaseRadarContentFilter.Episodes,
                            stringResource(Res.string.nuvio_enhanced_filter_episodes),
                        ),
                        EnhancedChoiceOption(
                            NuvioReleaseRadarContentFilter.Movies,
                            stringResource(Res.string.nuvio_enhanced_filter_movies),
                        ),
                    ),
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.ReleaseRadarFilters),
                    onSelected = {
                        markSeen(NuvioEnhancedFeature.ReleaseRadarFilters)
                        NuvioEnhancedSettingsRepository.setReleaseRadarContentFilter(it)
                    },
                )
            }
        }
        }

        if (selectedCategory == EnhancedSettingsCategory.All ||
            selectedCategory == EnhancedSettingsCategory.Home
        ) {
        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_details_experience),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                if (detailHeroTrailerPlaybackSupported) {
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_meta_hero_trailer_playback),
                        description = stringResource(Res.string.settings_meta_hero_trailer_playback_description),
                        checked = detailSettings.heroTrailerPlayback,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.DetailExperienceControls),
                        onCheckedChange = {
                            markSeen(NuvioEnhancedFeature.DetailExperienceControls)
                            MetaScreenSettingsRepository.setHeroTrailerPlayback(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                }
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_meta_show_episode_ratings),
                    description = stringResource(Res.string.settings_meta_show_episode_ratings_description),
                    checked = detailSettings.showEpisodeRatings,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.DetailExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.DetailExperienceControls)
                        MetaScreenSettingsRepository.setShowEpisodeRatings(it)
                    },
                )
            }
        }
        }

        if (selectedCategory == EnhancedSettingsCategory.All ||
            selectedCategory == EnhancedSettingsCategory.System
        ) {
        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_network),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                DnsOverHttpsProvider.entries.forEachIndexed { index, provider ->
                    if (index > 0) {
                        SettingsGroupDivider(isTablet = isTablet)
                    }
                    EnhancedDnsProviderRow(
                        provider = provider,
                        selected = provider == dnsOverHttpsSettings.provider,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.NetworkControls),
                        onClick = {
                            markSeen(NuvioEnhancedFeature.NetworkControls)
                            DnsOverHttpsSettingsRepository.setProvider(provider)
                        },
                    )
                }
            }
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_backup_import),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.nuvio_enhanced_backup_download_title),
                    description = stringResource(Res.string.nuvio_enhanced_backup_download_desc),
                    icon = Icons.Rounded.Backup,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.BackupImport),
                    onClick = {
                        markSeen(NuvioEnhancedFeature.BackupImport)
                        val payload = ProfileSettingsSync.exportBackupJson()
                        NuvioEnhancedBackupFileBridge.exportBackup(
                            fileName = "nuvio-backup.json",
                            payload = payload,
                        ) { result ->
                            result
                                .onSuccess { NuvioToastController.show(backupFileReadyMessage) }
                                .onFailure { error -> NuvioToastController.show(error.message ?: backupExportFailedMessage) }
                        }
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.nuvio_enhanced_backup_import_file_title),
                    description = stringResource(Res.string.nuvio_enhanced_backup_import_file_desc),
                    icon = Icons.Rounded.Restore,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.BackupImport),
                    onClick = {
                        markSeen(NuvioEnhancedFeature.BackupImport)
                        importError = null
                        NuvioEnhancedBackupFileBridge.importBackup { result ->
                            result
                                .onSuccess(::importBackupPayload)
                                .onFailure { error -> NuvioToastController.show(error.message ?: backupImportFailedMessage) }
                        }
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.nuvio_enhanced_backup_copy_title),
                    description = stringResource(Res.string.nuvio_enhanced_backup_copy_desc),
                    icon = Icons.Rounded.ContentCopy,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.BackupImport),
                    onClick = {
                        markSeen(NuvioEnhancedFeature.BackupImport)
                        val payload = ProfileSettingsSync.exportBackupJson()
                        backupPayload = payload
                        clipboardManager.setText(AnnotatedString(payload))
                        NuvioToastController.show(backupCopiedMessage)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.nuvio_enhanced_backup_paste_title),
                    description = stringResource(Res.string.nuvio_enhanced_backup_paste_desc),
                    icon = Icons.Rounded.Download,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.BackupImport),
                    onClick = {
                        markSeen(NuvioEnhancedFeature.BackupImport)
                        importError = null
                        showImportDialog = true
                    },
                )
            }
        }

        if (!isIos) {
            SettingsSection(
                title = stringResource(Res.string.nuvio_enhanced_section_downloads),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = stringResource(
                            if (externalFolderState.uri.isNullOrBlank()) {
                                Res.string.nuvio_enhanced_external_folder_choose_title
                            } else {
                                Res.string.nuvio_enhanced_external_folder_change_title
                            },
                        ),
                        description = when {
                            externalFolderState.unavailable -> stringResource(
                                Res.string.nuvio_enhanced_external_folder_unavailable_description,
                            )
                            !externalFolderState.displayName.isNullOrBlank() -> externalFolderState.displayName.orEmpty()
                            else -> stringResource(Res.string.nuvio_enhanced_external_folder_description)
                        },
                        icon = Icons.Rounded.Folder,
                        isTablet = isTablet,
                        onClick = {
                            DownloadsExternalFolderPlatform.chooseFolder { result ->
                                result.onFailure { error ->
                                    NuvioToastController.show(error.message ?: externalFolderFailedMessage)
                                }
                            }
                        },
                    )
                    if (!externalFolderState.uri.isNullOrBlank()) {
                        SettingsGroupDivider(isTablet = isTablet)
                        SettingsNavigationRow(
                            title = stringResource(Res.string.nuvio_enhanced_external_folder_remove_title),
                            description = stringResource(Res.string.nuvio_enhanced_external_folder_remove_description),
                            icon = Icons.Rounded.Delete,
                            isTablet = isTablet,
                            onClick = DownloadsExternalFolderPlatform::clearFolder,
                        )
                    }
                }
            }
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_discovery),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_highlight_title),
                    description = stringResource(Res.string.nuvio_enhanced_highlight_desc),
                    checked = settings.featureHighlightsEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.FeatureHighlights),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.FeatureHighlights)
                        NuvioEnhancedSettingsRepository.setFeatureHighlightsEnabled(it)
                    },
                )
            }
        }

        EnhancedCommunityFooter(
            isTablet = isTablet,
            onGithubClick = {
                markSeen(NuvioEnhancedFeature.CommunityLinks)
                uriHandler.openUri(NuvioEnhancedGithubUrl)
            },
            onDiscordClick = {
                markSeen(NuvioEnhancedFeature.CommunityLinks)
                uriHandler.openUri(NuvioEnhancedDiscordUrl)
            },
        )

        if (CrashDiagnostics.reportsSupported) {
            SettingsSection(
                title = stringResource(Res.string.nuvio_enhanced_section_diagnostics),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.nuvio_enhanced_copy_last_crash_title),
                        description = stringResource(
                            if (lastCrashReport == null) {
                                Res.string.nuvio_enhanced_copy_last_crash_empty_desc
                            } else {
                                Res.string.nuvio_enhanced_copy_last_crash_desc
                            },
                        ),
                        icon = Icons.Rounded.ContentCopy,
                        enabled = lastCrashReport != null,
                        isTablet = isTablet,
                        onClick = {
                            val report = lastCrashReport ?: return@SettingsNavigationRow
                            clipboardManager.setText(AnnotatedString(report.details))
                            NuvioToastController.show(crashCopiedMessage)
                        },
                    )
                }
            }
        }
        }
    }

    backupPayload?.let { payload ->
        BackupPayloadDialog(
            payload = payload,
            onDismiss = { backupPayload = null },
            onCopy = {
                clipboardManager.setText(AnnotatedString(payload))
                NuvioToastController.show(backupCopiedMessage)
            },
        )
    }

    if (showImportDialog) {
        ImportBackupDialog(
            payload = importPayload,
            error = importError,
            onPayloadChange = {
                importPayload = it
                importError = null
            },
            onDismiss = {
                showImportDialog = false
                importPayload = ""
                importError = null
            },
            onImport = {
                importBackupPayload(importPayload)
            },
        )
    }
}

@Composable
private fun EnhancedDnsProviderRow(
    provider: DnsOverHttpsProvider,
    selected: Boolean,
    isTablet: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val iconSize = if (isTablet) 42.dp else 36.dp
    val rowShape = RoundedCornerShape(if (isTablet) NuvioTokens.Radius.lg else NuvioTokens.Radius.md)
    val rowColor = when {
        selected -> tokens.colors.accent.copy(alpha = 0.13f)
        highlighted -> tokens.colors.accent.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    val borderColor = when {
        selected -> tokens.colors.accent.copy(alpha = 0.86f)
        highlighted -> tokens.colors.accent.copy(alpha = 0.72f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowColor, rowShape)
            .border(tokens.borders.hairline, borderColor, rowShape)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (isTablet) 20.dp else 16.dp,
                vertical = if (isTablet) 16.dp else 14.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(iconSize),
            color = if (selected) {
                tokens.colors.accent.copy(alpha = 0.22f)
            } else {
                tokens.colors.accent.copy(alpha = tokens.opacity.pressed)
            },
            shape = tokens.shapes.compactCard,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    tint = tokens.colors.accent,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = provider.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (highlighted) {
                    EnhancedNewBadge()
                }
            }
            Text(
                text = if (selected) {
                    stringResource(Res.string.settings_advanced_doh_selected)
                } else {
                    stringResource(Res.string.settings_advanced_doh_description)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
        }

        if (selected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = stringResource(Res.string.settings_advanced_doh_selected),
                tint = tokens.colors.accent,
            )
        }
    }
}

private data class EnhancedChoiceOption<T>(
    val value: T,
    val label: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> EnhancedChoiceRow(
    title: String,
    description: String,
    selected: T,
    options: List<EnhancedChoiceOption<T>>,
    isTablet: Boolean,
    highlighted: Boolean,
    onSelected: (T) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val highlightShape = RoundedCornerShape(if (isTablet) NuvioTokens.Radius.lg else NuvioTokens.Radius.md)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier
                        .background(tokens.colors.accent.copy(alpha = 0.08f), highlightShape)
                        .border(tokens.borders.hairline, tokens.colors.accent.copy(alpha = 0.72f), highlightShape)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (highlighted) {
                    EnhancedNewBadge()
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isSelected = option.value == selected
                Surface(
                    modifier = Modifier.clickable { onSelected(option.value) },
                    color = if (isSelected) {
                        tokens.colors.accent
                    } else {
                        tokens.colors.surfaceCard.copy(alpha = 0.72f)
                    },
                    contentColor = if (isSelected) {
                        tokens.colors.onAccent
                    } else {
                        tokens.colors.textPrimary
                    },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(
                        tokens.borders.hairline,
                        if (isSelected) {
                            tokens.colors.accent.copy(alpha = 0.86f)
                        } else {
                            tokens.colors.borderSubtle
                        },
                    ),
                ) {
                    Text(
                        text = option.label,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) tokens.colors.onAccent else tokens.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedNewBadge() {
    val tokens = MaterialTheme.nuvio
    Surface(
        color = tokens.colors.accent.copy(alpha = 0.16f),
        contentColor = tokens.colors.accent,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(tokens.borders.hairline, tokens.colors.accent.copy(alpha = 0.42f)),
    ) {
        Text(
            text = stringResource(Res.string.settings_new_feature_badge),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun EnhancedCommunityFooter(
    isTablet: Boolean,
    onGithubClick: () -> Unit,
    onDiscordClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.colors.surface.copy(alpha = 0.72f),
        shape = if (isTablet) RoundedCornerShape(NuvioTokens.Radius.xl) else tokens.shapes.compactCard,
        border = BorderStroke(tokens.borders.hairline, tokens.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(Res.string.nuvio_enhanced_footer_title),
                style = MaterialTheme.typography.titleMedium,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.nuvio_enhanced_footer_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EnhancedFooterLink(
                    title = stringResource(Res.string.nuvio_enhanced_footer_github),
                    subtitle = stringResource(Res.string.nuvio_enhanced_footer_releases),
                    icon = appIconPainter(AppIconResource.GithubMark),
                    modifier = Modifier.weight(1f),
                    onClick = onGithubClick,
                )
                EnhancedFooterLink(
                    title = stringResource(Res.string.nuvio_enhanced_footer_discord),
                    subtitle = stringResource(Res.string.nuvio_enhanced_footer_community),
                    icon = appIconPainter(AppIconResource.DiscordMark),
                    modifier = Modifier.weight(1f),
                    onClick = onDiscordClick,
                )
            }
        }
    }
}

@Composable
private fun EnhancedFooterLink(
    title: String,
    subtitle: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick),
        color = tokens.colors.accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(NuvioTokens.Radius.lg),
        border = BorderStroke(tokens.borders.hairline, tokens.colors.accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                color = tokens.colors.accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(NuvioTokens.Radius.md),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = title,
                        tint = tokens.colors.accent,
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EnhancedSettingsCategoryBar(
    selected: EnhancedSettingsCategory,
    hasNewFeatures: Boolean,
    onSelected: (EnhancedSettingsCategory) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val categories = buildList {
        if (hasNewFeatures) {
            add(EnhancedSettingsCategory.New to stringResource(Res.string.nuvio_enhanced_category_new))
        }
        add(EnhancedSettingsCategory.All to stringResource(Res.string.nuvio_enhanced_category_all))
        add(EnhancedSettingsCategory.Core to stringResource(Res.string.nuvio_enhanced_category_core))
        add(EnhancedSettingsCategory.Home to stringResource(Res.string.nuvio_enhanced_category_home))
        add(EnhancedSettingsCategory.Player to stringResource(Res.string.nuvio_enhanced_category_player))
        add(EnhancedSettingsCategory.System to stringResource(Res.string.nuvio_enhanced_category_system))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.colors.surface,
        shape = tokens.shapes.compactCard,
        border = BorderStroke(tokens.borders.hairline, tokens.colors.borderSubtle),
    ) {
        FlowRow(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { (category, label) ->
                val isSelected = selected == category
                Surface(
                    modifier = Modifier.clickable { onSelected(category) },
                    color = if (isSelected) {
                        tokens.colors.accent.copy(alpha = 0.16f)
                    } else {
                        Color.Transparent
                    },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(
                        tokens.borders.hairline,
                        if (isSelected) tokens.colors.accent else tokens.colors.borderSubtle,
                    ),
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) tokens.colors.accent else tokens.colors.textMuted,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedIntroCard(
    isTablet: Boolean,
    hasNewFeatures: Boolean,
    onMarkAllSeen: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.colors.surface,
        shape = if (isTablet) RoundedCornerShape(NuvioTokens.Radius.xl) else tokens.shapes.compactCard,
        border = BorderStroke(tokens.borders.hairline, tokens.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = tokens.colors.accent,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.nuvio_enhanced_intro_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.nuvio_enhanced_intro_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                    )
                }
            }
            if (hasNewFeatures) {
                OutlinedButton(onClick = onMarkAllSeen) {
                    Text(stringResource(Res.string.nuvio_enhanced_mark_all_seen))
                }
            }
        }
    }
}

@Composable
private fun BackupPayloadDialog(
    payload: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.nuvio_enhanced_backup_ready_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(Res.string.nuvio_enhanced_backup_ready_desc))
                OutlinedTextField(
                    value = payload,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    maxLines = 8,
                )
            }
        },
        confirmButton = {
            Button(onClick = onCopy) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(stringResource(Res.string.nuvio_enhanced_backup_copy_again))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.nuvio_enhanced_close))
            }
        },
    )
}

@Composable
private fun ImportBackupDialog(
    payload: String,
    error: String?,
    onPayloadChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.nuvio_enhanced_import_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(Res.string.nuvio_enhanced_import_desc))
                OutlinedTextField(
                    value = payload,
                    onValueChange = onPayloadChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    placeholder = { Text(stringResource(Res.string.nuvio_enhanced_import_placeholder)) },
                    maxLines = 10,
                )
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onImport,
                enabled = payload.isNotBlank(),
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(stringResource(Res.string.nuvio_enhanced_import_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.nuvio_enhanced_cancel))
            }
        },
    )
}
