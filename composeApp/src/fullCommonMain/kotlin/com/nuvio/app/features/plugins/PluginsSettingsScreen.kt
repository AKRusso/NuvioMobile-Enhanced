package com.nuvio.app.features.plugins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioIconActionButton
import com.nuvio.app.core.ui.NuvioInfoBadge
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.plugins.runtime.PluginRuntime
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.plugins_badge_disabled
import nuvio.composeapp.generated.resources.plugins_badge_enabled
import nuvio.composeapp.generated.resources.plugins_badge_providers
import nuvio.composeapp.generated.resources.plugins_badge_refreshing
import nuvio.composeapp.generated.resources.plugins_badge_repos
import nuvio.composeapp.generated.resources.plugins_badge_tmdb_key_missing
import nuvio.composeapp.generated.resources.plugins_badge_tmdb_key_set
import nuvio.composeapp.generated.resources.plugins_button_install_repo
import nuvio.composeapp.generated.resources.plugins_button_installing
import nuvio.composeapp.generated.resources.plugins_button_test_provider
import nuvio.composeapp.generated.resources.plugins_button_testing
import nuvio.composeapp.generated.resources.plugins_cd_delete_repo
import nuvio.composeapp.generated.resources.plugins_cd_refresh_repo
import nuvio.composeapp.generated.resources.plugins_quality_filtering_desc
import nuvio.composeapp.generated.resources.plugins_quality_filtering_empty
import nuvio.composeapp.generated.resources.plugins_empty_providers
import nuvio.composeapp.generated.resources.plugins_empty_repos_subtitle
import nuvio.composeapp.generated.resources.plugins_empty_repos_title
import nuvio.composeapp.generated.resources.plugins_enable_globally_desc
import nuvio.composeapp.generated.resources.plugins_enable_globally_title
import nuvio.composeapp.generated.resources.plugins_error_enter_repo_url
import nuvio.composeapp.generated.resources.plugins_group_by_repo_desc
import nuvio.composeapp.generated.resources.plugins_group_by_repo_title
import nuvio.composeapp.generated.resources.plugins_input_manifest_placeholder
import nuvio.composeapp.generated.resources.plugins_message_installed
import nuvio.composeapp.generated.resources.plugins_provider_disabled_by_repo
import nuvio.composeapp.generated.resources.plugins_provider_no_description
import nuvio.composeapp.generated.resources.plugins_provider_version
import nuvio.composeapp.generated.resources.plugins_repo_fallback_label
import nuvio.composeapp.generated.resources.plugins_repo_version
import nuvio.composeapp.generated.resources.plugins_repository_filter_all
import nuvio.composeapp.generated.resources.plugins_repository_filter_count
import nuvio.composeapp.generated.resources.plugins_repository_filter_desc
import nuvio.composeapp.generated.resources.plugins_repository_filter_disable_visible
import nuvio.composeapp.generated.resources.plugins_repository_filter_enable_visible
import nuvio.composeapp.generated.resources.plugins_section_quality_filtering
import nuvio.composeapp.generated.resources.plugins_section_add_repo
import nuvio.composeapp.generated.resources.plugins_section_installed_repos
import nuvio.composeapp.generated.resources.plugins_section_overview
import nuvio.composeapp.generated.resources.plugins_section_providers
import nuvio.composeapp.generated.resources.plugins_section_repository_control
import nuvio.composeapp.generated.resources.plugins_test_error_title
import nuvio.composeapp.generated.resources.plugins_test_failed
import nuvio.composeapp.generated.resources.plugins_test_results_count
import nuvio.composeapp.generated.resources.plugins_tmdb_required_message
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PluginsSettingsPageContent(
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        PluginRepository.initialize()
    }

    val uiState by PluginRepository.uiState.collectAsStateWithLifecycle()
    val tmdbSettings by remember {
        TmdbSettingsRepository.ensureLoaded()
        TmdbSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var repositoryUrl by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    var testingScraperId by remember { mutableStateOf<String?>(null) }
    val testResults = remember { mutableStateMapOf<String, List<PluginRuntimeResult>>() }
    var selectedRepositoryUrl by rememberSaveable { mutableStateOf<String?>(null) }

    var configuringScraper by remember { mutableStateOf<PluginScraper?>(null) }
    var configuringLayout by remember { mutableStateOf<String?>(null) }

    val sortedRepos = remember(uiState.repositories) {
        uiState.repositories.sortedBy { it.name.lowercase() }
    }
    LaunchedEffect(sortedRepos, selectedRepositoryUrl) {
        if (selectedRepositoryUrl != null && sortedRepos.none { it.manifestUrl == selectedRepositoryUrl }) {
            selectedRepositoryUrl = null
        }
    }
    val hasTmdbApiKey = tmdbSettings.hasApiKey
    val repositoryNameByUrl = remember(sortedRepos) {
        sortedRepos.associate { it.manifestUrl to it.name }
    }
    val sortedScrapers = remember(uiState.scrapers, repositoryNameByUrl) {
        uiState.scrapers.sortedWith(
            compareBy<PluginScraper>(
                { repositoryNameByUrl[it.repositoryUrl]?.lowercase() ?: it.repositoryUrl.lowercase() },
                { it.name.lowercase() },
            ),
        )
    }
    val visibleScrapers = remember(sortedScrapers, selectedRepositoryUrl) {
        selectedRepositoryUrl
            ?.let { selectedUrl -> sortedScrapers.filter { it.repositoryUrl == selectedUrl } }
            ?: sortedScrapers
    }

    val repoFallbackLabel = stringResource(Res.string.plugins_repo_fallback_label)
    val testFailedDefault = stringResource(Res.string.plugins_test_failed)
    val testErrorTitle = stringResource(Res.string.plugins_test_error_title)
    val installedTemplate = stringResource(Res.string.plugins_message_installed)
    val enterRepoUrlError = stringResource(Res.string.plugins_error_enter_repo_url)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NuvioSectionLabel(stringResource(Res.string.plugins_section_overview))
        NuvioSurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_repos, sortedRepos.size))
                NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_providers, sortedScrapers.size))
                NuvioInfoBadge(
                    text = if (uiState.pluginsEnabled) {
                        stringResource(Res.string.plugins_badge_enabled)
                    } else {
                        stringResource(Res.string.plugins_badge_disabled)
                    },
                )
                NuvioInfoBadge(
                    text = if (hasTmdbApiKey) {
                        stringResource(Res.string.plugins_badge_tmdb_key_set)
                    } else {
                        stringResource(Res.string.plugins_badge_tmdb_key_missing)
                    },
                )
            }
            if (!hasTmdbApiKey) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.plugins_tmdb_required_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.plugins_enable_globally_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.plugins_enable_globally_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = uiState.pluginsEnabled,
                    onCheckedChange = { PluginRepository.setPluginsEnabled(it) },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.plugins_group_by_repo_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.plugins_group_by_repo_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = uiState.groupStreamsByRepository,
                    onCheckedChange = { PluginRepository.setGroupStreamsByRepository(it) },
                )
            }
        }

        NuvioSectionLabel(stringResource(Res.string.plugins_section_repository_control))
        NuvioSurfaceCard {
            Text(
                text = stringResource(Res.string.plugins_repository_filter_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PluginRepositoryFilterChip(
                    label = stringResource(Res.string.plugins_repository_filter_all),
                    countLabel = stringResource(
                        Res.string.plugins_repository_filter_count,
                        sortedScrapers.count { it.enabled },
                        sortedScrapers.size,
                    ),
                    selected = selectedRepositoryUrl == null,
                    onClick = { selectedRepositoryUrl = null },
                )
                sortedRepos.forEach { repo ->
                    val repoScrapers = sortedScrapers.filter { it.repositoryUrl == repo.manifestUrl }
                    PluginRepositoryFilterChip(
                        label = repo.name,
                        countLabel = stringResource(
                            Res.string.plugins_repository_filter_count,
                            repoScrapers.count { it.enabled },
                            repoScrapers.size,
                        ),
                        selected = selectedRepositoryUrl == repo.manifestUrl,
                        onClick = { selectedRepositoryUrl = repo.manifestUrl },
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = visibleScrapers.any { it.manifestEnabled && !it.enabled },
                    onClick = {
                        selectedRepositoryUrl
                            ?.let { PluginRepository.toggleRepositoryScrapers(it, true) }
                            ?: PluginRepository.toggleAllScrapers(true)
                    },
                ) {
                    Text(stringResource(Res.string.plugins_repository_filter_enable_visible))
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = visibleScrapers.any { it.manifestEnabled && it.enabled },
                    onClick = {
                        selectedRepositoryUrl
                            ?.let { PluginRepository.toggleRepositoryScrapers(it, false) }
                            ?: PluginRepository.toggleAllScrapers(false)
                    },
                ) {
                    Text(stringResource(Res.string.plugins_repository_filter_disable_visible))
                }
            }
        }

        NuvioSectionLabel(stringResource(Res.string.plugins_section_quality_filtering))
        NuvioSurfaceCard {
            Text(
                text = stringResource(Res.string.plugins_quality_filtering_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PluginQualityFilterOptions.forEach { option ->
                    val excluded = option.id in uiState.excludedQualities
                    PluginQualityFilterChip(
                        option = option,
                        excluded = excluded,
                        onClick = { PluginRepository.setQualityExcluded(option.id, !excluded) },
                    )
                }
            }
            if (uiState.excludedQualities.isEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(Res.string.plugins_quality_filtering_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        NuvioSectionLabel(stringResource(Res.string.plugins_section_add_repo))
        NuvioSurfaceCard {
            NuvioInputField(
                value = repositoryUrl,
                onValueChange = {
                    repositoryUrl = it
                    message = null
                },
                placeholder = stringResource(Res.string.plugins_input_manifest_placeholder),
            )
            Spacer(modifier = Modifier.height(16.dp))
            NuvioPrimaryButton(
                text = if (isAdding) {
                    stringResource(Res.string.plugins_button_installing)
                } else {
                    stringResource(Res.string.plugins_button_install_repo)
                },
                enabled = repositoryUrl.isNotBlank() && !isAdding,
                onClick = {
                    val requested = repositoryUrl.trim()
                    if (requested.isBlank()) {
                        message = enterRepoUrlError
                        return@NuvioPrimaryButton
                    }
                    isAdding = true
                    message = null
                    coroutineScope.launch {
                        when (val result = PluginRepository.addRepository(requested)) {
                            is AddPluginRepositoryResult.Success -> {
                                repositoryUrl = ""
                                message = installedTemplate.replace("%1\$s", result.repository.name)
                            }
                            is AddPluginRepositoryResult.Error -> {
                                message = result.message
                            }
                        }
                        isAdding = false
                    }
                },
            )
            message?.let { text ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        NuvioSectionLabel(stringResource(Res.string.plugins_section_installed_repos))
        if (sortedRepos.isEmpty()) {
            NuvioSurfaceCard {
                Text(
                    text = stringResource(Res.string.plugins_empty_repos_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.plugins_empty_repos_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            sortedRepos.forEach { repo ->
                NuvioSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = repo.name,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            repo.version?.let { version ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(Res.string.plugins_repo_version, version),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = repo.manifestUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NuvioIconActionButton(
                                icon = Icons.Rounded.Refresh,
                                contentDescription = stringResource(Res.string.plugins_cd_refresh_repo),
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = { PluginRepository.refreshRepository(repo.manifestUrl, pushAfterRefresh = true) },
                            )
                            NuvioIconActionButton(
                                icon = Icons.Rounded.Delete,
                                contentDescription = stringResource(Res.string.plugins_cd_delete_repo),
                                tint = MaterialTheme.colorScheme.error,
                                onClick = { PluginRepository.removeRepository(repo.manifestUrl) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_providers, repo.scraperCount))
                        if (repo.isRefreshing) {
                            NuvioInfoBadge(text = stringResource(Res.string.plugins_badge_refreshing))
                        }
                    }
                    repo.errorMessage?.let { errorMessage ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        NuvioSectionLabel(stringResource(Res.string.plugins_section_providers))
        if (visibleScrapers.isEmpty()) {
            NuvioSurfaceCard {
                Text(
                    text = stringResource(Res.string.plugins_empty_providers),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            visibleScrapers.forEach { scraper ->
                val scraperResults = testResults[scraper.id]
                val isTestingThisScraper = testingScraperId == scraper.id
                val repositoryName = repositoryNameByUrl[scraper.repositoryUrl]
                    ?: scraper.repositoryUrl.fallbackRepositoryLabel(repoFallbackLabel)

                NuvioSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Extension,
                                contentDescription = null,
                                tint = if (scraper.enabled) Color(0xFF68B76A) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = repositoryName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = scraper.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = scraper.description.ifBlank {
                                        stringResource(Res.string.plugins_provider_no_description)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (scraper.hasSettings) {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        val layout = PluginRuntime.getPluginSettingsLayout(scraper.code, scraper.id)
                                        if (layout != null) {
                                            configuringScraper = scraper
                                            configuringLayout = layout
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Provider settings",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Switch(
                                checked = scraper.enabled,
                                onCheckedChange = { PluginRepository.toggleScraper(scraper.id, it) },
                                enabled = scraper.manifestEnabled,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        NuvioInfoBadge(text = scraper.supportedTypes.joinToString(" | "))
                        NuvioInfoBadge(text = stringResource(Res.string.plugins_provider_version, scraper.version))
                        if (!scraper.manifestEnabled) {
                            NuvioInfoBadge(text = stringResource(Res.string.plugins_provider_disabled_by_repo))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    NuvioPrimaryButton(
                        text = if (isTestingThisScraper) {
                            stringResource(Res.string.plugins_button_testing)
                        } else {
                            stringResource(Res.string.plugins_button_test_provider)
                        },
                        enabled = hasTmdbApiKey && !isTestingThisScraper,
                        onClick = {
                            testingScraperId = scraper.id
                            coroutineScope.launch {
                                PluginRepository.testScraper(scraper.id)
                                    .onSuccess { results ->
                                        testResults[scraper.id] = results
                                    }
                                    .onFailure { error ->
                                        testResults[scraper.id] = listOf(
                                            PluginRuntimeResult(
                                                title = testErrorTitle,
                                                name = error.message ?: testFailedDefault,
                                                url = "about:error",
                                            ),
                                        )
                                    }
                                testingScraperId = null
                            }
                        },
                    )

                    if (!scraperResults.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(Res.string.plugins_test_results_count, scraperResults.size),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        scraperResults.take(8).forEach { result ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = result.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = result.url,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    if (configuringScraper != null && configuringLayout != null) {
        PluginSettingsDialog(
            scraperId = configuringScraper!!.id,
            scraperName = configuringScraper!!.name,
            layoutJson = configuringLayout!!,
            onDismiss = {
                configuringScraper = null
                configuringLayout = null
            }
        )
    }
}

@Composable
private fun PluginRepositoryFilterChip(
    label: String,
    countLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PluginQualityFilterChip(
    option: PluginQualityFilterOption,
    excluded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (excluded) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        },
        contentColor = if (excluded) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            1.dp,
            if (excluded) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.66f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Text(
            text = if (excluded) "X ${option.label}" else option.label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

private fun String.fallbackRepositoryLabel(fallback: String): String {
    val withoutQuery = substringBefore("?")
    val withoutManifest = withoutQuery.removeSuffix("/manifest.json")
    val host = withoutManifest.substringAfter("://", withoutManifest).substringBefore('/')
    return host.ifBlank {
        withoutManifest.substringAfterLast('/').ifBlank { fallback }
    }
}
