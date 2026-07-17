package com.nuvio.app.features.plugins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioIconActionButton
import com.nuvio.app.core.ui.NuvioInfoBadge
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.features.cloudstream.AddCloudStreamRepositoryResult
import com.nuvio.app.features.cloudstream.CloudStreamBulkInstallResult
import com.nuvio.app.features.cloudstream.CloudStreamInstallResult
import com.nuvio.app.features.cloudstream.CloudStreamPlatformSupport
import com.nuvio.app.features.cloudstream.CloudStreamPluginItem
import com.nuvio.app.features.cloudstream.CloudStreamRepository
import com.nuvio.app.features.settings.AppLanguage
import com.nuvio.app.features.settings.ThemeSettingsRepository
import com.nuvio.app.features.streams.StreamSourcePreferencesRepository
import com.nuvio.app.features.streams.cloudStreamAddonId
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CloudStreamSettingsSection() {
    LaunchedEffect(Unit) {
        ThemeSettingsRepository.ensureLoaded()
        CloudStreamRepository.initialize()
    }
    val state by CloudStreamRepository.uiState.collectAsStateWithLifecycle()
    val selectedAppLanguage by ThemeSettingsRepository.selectedAppLanguage.collectAsStateWithLifecycle()
    val sourcePreferences by remember {
        StreamSourcePreferencesRepository.ensureLoaded()
        StreamSourcePreferencesRepository.uiState
    }.collectAsStateWithLifecycle()
    val copy = remember(selectedAppLanguage) { CloudStreamSettingsCopy.forLanguage(selectedAppLanguage) }
    val scope = rememberCoroutineScope()
    var repositoryUrl by rememberSaveable { mutableStateOf("") }
    var editingRepositoryUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var isAddingRepository by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedLanguage by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var bulkInstallMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isBulkInstalling by remember { mutableStateOf(false) }

    val languages = remember(state.plugins) {
        state.plugins.mapNotNull { it.metadata.language }.distinct().sorted()
    }
    val types = remember(state.plugins) {
        state.plugins.flatMap { it.metadata.rawTvTypes }.distinct().sorted()
    }
    val visiblePlugins = remember(state.plugins, query, selectedLanguage, selectedType) {
        val normalizedQuery = query.trim().lowercase()
        state.plugins.filter { item ->
            (normalizedQuery.isBlank() || listOf(
                item.metadata.name,
                item.metadata.internalName,
                item.metadata.description.orEmpty(),
                item.metadata.authors.joinToString(" "),
            ).any { it.lowercase().contains(normalizedQuery) }) &&
                (selectedLanguage == null || item.metadata.language == selectedLanguage) &&
                (selectedType == null || selectedType in item.metadata.rawTvTypes)
        }
    }
    val bulkActionPlugins = remember(visiblePlugins) {
        visiblePlugins.filter { item ->
            !item.isInstalling &&
                item.metadata.status.canInstall &&
                item.compatibility.isRunnable &&
                (!item.isRunnable || item.hasUpdate || !item.isInstalled || !item.verified)
        }
    }
    val pinnedSourceIds = remember(sourcePreferences.pinnedSources) {
        sourcePreferences.pinnedSources.map { it.id }.toSet()
    }

    NuvioSectionLabel(copy.sectionTitle)
    NuvioSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NuvioInfoBadge(text = copy.repositoryCount(state.repositories.size))
            NuvioInfoBadge(text = copy.installedCount(state.plugins.count { it.isInstalled }))
            NuvioInfoBadge(text = copy.enabledProviderCount(state.plugins.count { it.isRunnable }))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = copy.sectionDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (!state.securityWarningAccepted) {
        NuvioSurfaceCard {
            Text(
                text = copy.securityWarningTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = copy.securityWarningBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            NuvioPrimaryButton(
                text = copy.acceptSecurityWarning,
                onClick = CloudStreamRepository::acceptSecurityWarning,
            )
        }
    }

    NuvioSurfaceCard {
        Text(
            text = if (editingRepositoryUrl == null) copy.addRepositoryTitle else copy.editRepositoryTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        NuvioInputField(
            value = repositoryUrl,
            onValueChange = {
                repositoryUrl = it
                message = null
            },
            placeholder = "https://github.com/Kraptor123/cs-kraptor",
        )
        Spacer(Modifier.height(12.dp))
        NuvioPrimaryButton(
            text = when {
                isAddingRepository -> copy.repositoryLoading
                editingRepositoryUrl != null -> copy.applyRepositoryChange
                else -> copy.addRepository
            },
            enabled = repositoryUrl.isNotBlank() && !isAddingRepository,
            onClick = {
                val requested = repositoryUrl.trim()
                val previous = editingRepositoryUrl
                isAddingRepository = true
                message = null
                scope.launch {
                    if (previous != null && requested == previous) {
                        CloudStreamRepository.refreshRepository(previous)
                        message = copy.repositoryRefreshing
                    } else {
                        when (val result = CloudStreamRepository.addRepository(requested)) {
                            is AddCloudStreamRepositoryResult.Success -> {
                                previous?.let(CloudStreamRepository::removeRepository)
                                repositoryUrl = ""
                                editingRepositoryUrl = null
                                message = copy.repositoryAdded(result.repository.name)
                            }
                            is AddCloudStreamRepositoryResult.Error -> message = result.message
                        }
                    }
                    isAddingRepository = false
                }
            },
        )
        message?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (state.repositories.isNotEmpty()) {
        NuvioSectionLabel(copy.repositoriesSectionTitle)
        state.repositories.forEach { repository ->
            NuvioSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(repository.manifest.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            repository.manifest.sourceUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        repository.errorMessage?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(
                        onClick = {
                            repositoryUrl = repository.manifest.sourceUrl
                            editingRepositoryUrl = repository.manifest.sourceUrl
                        },
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = copy.editRepositoryContentDescription)
                    }
                    IconButton(onClick = { CloudStreamRepository.refreshRepository(repository.manifest.sourceUrl) }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = copy.refreshRepositoryContentDescription)
                    }
                    IconButton(onClick = { CloudStreamRepository.removeRepository(repository.manifest.sourceUrl) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = copy.removeRepositoryContentDescription)
                    }
                }
            }
        }
    }

    if (state.plugins.isNotEmpty()) {
        NuvioSectionLabel(copy.providersSectionTitle)
        NuvioSurfaceCard {
            Text(copy.searchAndFilterTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            NuvioInputField(
                value = query,
                onValueChange = { query = it },
                placeholder = copy.searchPlaceholder,
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CloudStreamFilterChip(copy.allLanguages, selectedLanguage == null) { selectedLanguage = null }
                languages.forEach { language ->
                    CloudStreamFilterChip(language.uppercase(), selectedLanguage == language) {
                        selectedLanguage = language.takeUnless { selectedLanguage == language }
                    }
                }
            }
            if (types.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CloudStreamFilterChip(copy.allTypes, selectedType == null) { selectedType = null }
                    types.forEach { type ->
                        CloudStreamFilterChip(type, selectedType == type) {
                            selectedType = type.takeUnless { selectedType == type }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = state.securityWarningAccepted && !isBulkInstalling && bulkActionPlugins.isNotEmpty(),
                onClick = {
                    val targetIds = bulkActionPlugins.map { it.metadata.id.value }
                    isBulkInstalling = true
                    bulkInstallMessage = null
                    message = null
                    scope.launch {
                        bulkInstallMessage = runCatching {
                            CloudStreamRepository.installAndEnablePlugins(targetIds).toUserMessage(copy)
                        }.getOrElse { error ->
                            error.message ?: copy.bulkInstallFailed
                        }
                        isBulkInstalling = false
                    }
                },
            ) {
                Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        isBulkInstalling -> copy.bulkInstalling
                        bulkActionPlugins.isEmpty() -> copy.allInstalledAndActive
                        else -> copy.installAndEnableAll(bulkActionPlugins.size)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            bulkInstallMessage?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    visiblePlugins.forEach { plugin ->
        CloudStreamPluginCard(
            plugin = plugin,
            securityWarningAccepted = state.securityWarningAccepted,
            pinnedSourceIds = pinnedSourceIds,
            copy = copy,
            onMessage = { message = it },
        )
    }
}

private fun CloudStreamBulkInstallResult.toUserMessage(copy: CloudStreamSettingsCopy): String {
    if (requestedCount == 0) return copy.noPluginsToInstall
    val enabledText = copy.enabledPluginCount(enabledCount)
    val installText = if (installedCount > 0) copy.installedOrUpdatedCount(installedCount) else ""
    val skippedText = if (skippedCount > 0) copy.pendingOperationCount(skippedCount) else ""
    val failureText = failures.firstOrNull()?.let { first ->
        copy.skippedFailureCount(failures.size, first.pluginName, first.message.localizedCloudStreamFailure(copy))
    }.orEmpty()
    return when {
        enabledCount > 0 -> "$enabledText$installText$skippedText$failureText."
        failures.isNotEmpty() -> {
            val first = failures.first()
            copy.noPluginEnabled(first.pluginName, first.message.localizedCloudStreamFailure(copy))
        }
        skippedCount > 0 -> copy.onlyPendingOperations(skippedCount)
        else -> copy.noPluginsToInstall
    }
}

@Composable
private fun CloudStreamPluginCard(
    plugin: CloudStreamPluginItem,
    securityWarningAccepted: Boolean,
    pinnedSourceIds: Set<String>,
    copy: CloudStreamSettingsCopy,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sourceId = remember(plugin.metadata.id.value) { cloudStreamAddonId(plugin.metadata.id.value) }
    val isPinnedSource = sourceId in pinnedSourceIds
    NuvioSurfaceCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = plugin.metadata.iconUrl,
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plugin.metadata.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    plugin.metadata.authors.joinToString().ifBlank { copy.unknownAuthor },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                NuvioIconActionButton(
                    icon = Icons.Rounded.PushPin,
                    contentDescription = if (isPinnedSource) copy.unpinSource else copy.pinSource,
                    tint = if (isPinnedSource) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    onClick = {
                        if (isPinnedSource) {
                            StreamSourcePreferencesRepository.unpinSource(sourceId)
                        } else {
                            StreamSourcePreferencesRepository.pinSource(sourceId, plugin.metadata.name)
                        }
                    },
                )
                Switch(
                    checked = plugin.enabled,
                    enabled = plugin.isInstalled && plugin.verified && plugin.compatibility.isRunnable,
                    onCheckedChange = { CloudStreamRepository.setPluginEnabled(plugin.metadata.id.value, it) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            plugin.metadata.description ?: copy.noDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NuvioInfoBadge(text = copy.currentVersion(plugin.metadata.version))
            plugin.installedVersion?.let { NuvioInfoBadge(text = copy.installedVersion(it)) }
            plugin.metadata.language?.let { NuvioInfoBadge(text = it.uppercase()) }
            plugin.metadata.rawTvTypes.forEach { NuvioInfoBadge(text = it) }
            NuvioInfoBadge(
                text = when {
                    plugin.metadata.fileHash != null && plugin.verified -> copy.sha256Verified
                    plugin.metadata.fileHash != null -> copy.hashUnverified
                    plugin.verified -> copy.packageChecked
                    else -> copy.packageUnchecked
                },
            )
            NuvioInfoBadge(
                text = when (plugin.compatibility.platformSupport) {
                    CloudStreamPlatformSupport.AndroidAndIos -> copy.androidAndIosCompatible
                    CloudStreamPlatformSupport.AndroidOnly -> copy.androidCompatible
                    CloudStreamPlatformSupport.Unsupported -> copy.runtimeIncompatible
                },
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = copy.compatibilityReason(plugin.compatibility.platformSupport),
            style = MaterialTheme.typography.bodySmall,
            color = if (plugin.compatibility.isRunnable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        plugin.errorMessage?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = securityWarningAccepted && !plugin.isInstalling,
                onClick = {
                    scope.launch {
                        val result = if (plugin.hasUpdate) {
                            CloudStreamRepository.updatePlugin(plugin.metadata.id.value)
                        } else {
                            CloudStreamRepository.installPlugin(plugin.metadata.id.value)
                        }
                        onMessage(
                            when (result) {
                                is CloudStreamInstallResult.Success -> copy.pluginInstalled(result.plugin.metadata.name)
                                is CloudStreamInstallResult.Error -> result.message.localizedCloudStreamFailure(copy)
                            },
                        )
                    }
                },
            ) {
                Text(
                    when {
                        plugin.isInstalling -> copy.downloading
                        plugin.hasUpdate -> copy.update
                        plugin.isInstalled -> copy.reinstall
                        else -> copy.install
                    },
                )
            }
            if (plugin.isInstalled) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { CloudStreamRepository.removePlugin(plugin.metadata.id.value) },
                ) {
                    Text(copy.remove)
                }
            }
        }
    }
}

private class CloudStreamSettingsCopy private constructor(
    private val turkish: Boolean,
) {
    val sectionTitle: String =
        if (turkish) "CloudStream / CS3" else "CloudStream / CS3"
    val repositoriesSectionTitle: String =
        if (turkish) "CS3 repositoryleri" else "CS3 repositories"
    val providersSectionTitle: String =
        if (turkish) "CS3 providerları" else "CS3 providers"
    val sectionDescription: String =
        if (turkish) {
            "Standart .cs3 paketleri indirilen üçüncü taraf kodudur. Android full sürümü bu paketleri CloudStream çalışma zamanı ile çalıştırabilir; iOS yalnızca uygulamaya derlenmiş uyumluluk adaptörlerini kullanabilir. Yalnızca güvendiğiniz depoları ve eklentileri kurun."
        } else {
            "Standard .cs3 packages are downloaded third-party code. Android full builds can run them with the embedded CloudStream runtime; iOS can only use compatibility adapters compiled into the app. Only install repositories and plugins you trust."
        }
    val securityWarningTitle: String =
        if (turkish) "Üçüncü taraf kod güvenlik uyarısı" else "Third-party code security warning"
    val securityWarningBody: String =
        if (turkish) {
            "Repository ve eklentiler Nuvio tarafından yönetilmez. Yalnızca güvendiğiniz kaynakları ekleyin. Paket denetlenmeden provider etkinleştirilemez."
        } else {
            "Repositories and plugins are not managed by Nuvio. Only add sources you trust. A provider cannot be enabled until its package is checked."
        }
    val acceptSecurityWarning: String =
        if (turkish) "Uyarıyı okudum ve kabul ediyorum" else "I have read and accept the warning"
    val addRepositoryTitle: String =
        if (turkish) "CloudStream repository ekle" else "Add CloudStream repository"
    val editRepositoryTitle: String =
        if (turkish) "Repository bağlantısını düzenle" else "Edit repository link"
    val repositoryLoading: String =
        if (turkish) "Repository okunuyor…" else "Reading repository..."
    val applyRepositoryChange: String =
        if (turkish) "Değişikliği uygula" else "Apply changes"
    val addRepository: String =
        if (turkish) "Repository ekle" else "Add repository"
    val repositoryRefreshing: String =
        if (turkish) "Repository yenileniyor." else "Refreshing repository."
    val editRepositoryContentDescription: String =
        if (turkish) "Repository düzenle" else "Edit repository"
    val refreshRepositoryContentDescription: String =
        if (turkish) "Repository yenile" else "Refresh repository"
    val removeRepositoryContentDescription: String =
        if (turkish) "Repository kaldır" else "Remove repository"
    val searchAndFilterTitle: String =
        if (turkish) "Eklenti ara ve filtrele" else "Search and filter plugins"
    val searchPlaceholder: String =
        if (turkish) "Eklenti, açıklama veya yazar ara" else "Search plugin, description, or author"
    val allLanguages: String =
        if (turkish) "Tüm diller" else "All languages"
    val allTypes: String =
        if (turkish) "Tüm türler" else "All types"
    val bulkInstallFailed: String =
        if (turkish) "Toplu kurulum tamamlanamadı." else "Bulk install could not be completed."
    val bulkInstalling: String =
        if (turkish) "Toplu kurulum yapılıyor…" else "Bulk install in progress..."
    val allInstalledAndActive: String =
        if (turkish) "Tümü kurulu ve aktif" else "Everything is installed and active"
    val noPluginsToInstall: String =
        if (turkish) "Kurulacak eklenti bulunamadı." else "No plugins to install."
    val unknownAuthor: String =
        if (turkish) "Yazar belirtilmemiş" else "Author not specified"
    val noDescription: String =
        if (turkish) "Açıklama yok." else "No description."
    val sha256Verified: String =
        if (turkish) "SHA-256 doğrulandı" else "SHA-256 verified"
    val hashUnverified: String =
        if (turkish) "Hash doğrulanmadı" else "Hash unverified"
    val packageChecked: String =
        if (turkish) "Paket denetlendi" else "Package checked"
    val packageUnchecked: String =
        if (turkish) "Paket denetlenmedi" else "Package unchecked"
    val androidAndIosCompatible: String =
        if (turkish) "Android + iOS uyumlu" else "Android + iOS compatible"
    val androidCompatible: String =
        if (turkish) "Android uyumlu" else "Android compatible"
    val runtimeIncompatible: String =
        if (turkish) "Runtime uyumsuz" else "Runtime incompatible"
    val downloading: String =
        if (turkish) "İndiriliyor…" else "Downloading..."
    val update: String =
        if (turkish) "Güncelle" else "Update"
    val reinstall: String =
        if (turkish) "Yeniden kur" else "Reinstall"
    val install: String =
        if (turkish) "Kur" else "Install"
    val remove: String =
        if (turkish) "Kaldır" else "Remove"
    val pinSource: String =
        if (turkish) "Kaynağı sabitle" else "Pin source"
    val unpinSource: String =
        if (turkish) "Kaynak sabitlemesini kaldır" else "Unpin source"
    val acceptSecurityWarningFailure: String =
        if (turkish) "Üçüncü taraf eklenti güvenlik uyarısını kabul edin." else "Accept the third-party plugin security warning."
    val pluginNotFoundFailure: String =
        if (turkish) "Eklenti bulunamadı." else "Plugin was not found."
    val pluginDownFailure: String =
        if (turkish) "Bu eklenti şu anda kapalı görünüyor." else "This plugin appears to be down."
    val packageEnableFailure: String =
        if (turkish) {
            "Paket kuruldu ama doğrulama/uyumluluk şartları nedeniyle aktif edilemedi."
        } else {
            "The package was installed, but could not be enabled because verification or compatibility checks failed."
        }

    fun repositoryCount(count: Int): String =
        if (turkish) "$count repository" else "$count repositories"

    fun installedCount(count: Int): String =
        if (turkish) "$count kurulu" else "$count installed"

    fun enabledProviderCount(count: Int): String =
        if (turkish) "$count etkin provider" else "$count enabled providers"

    fun repositoryAdded(name: String): String =
        if (turkish) "$name eklendi." else "$name added."

    fun installAndEnableAll(count: Int): String =
        if (turkish) "Tümünü kur ve aktif et ($count)" else "Install and enable all ($count)"

    fun enabledPluginCount(count: Int): String =
        if (turkish) "$count eklenti aktif edildi" else "$count plugins enabled"

    fun installedOrUpdatedCount(count: Int): String =
        if (turkish) ", $count paket kuruldu/güncellendi" else ", $count packages installed/updated"

    fun pendingOperationCount(count: Int): String =
        if (turkish) ", $count işlem zaten sürüyor" else ", $count operations already in progress"

    fun skippedFailureCount(count: Int, pluginName: String, message: String): String =
        if (turkish) {
            ". $count eklenti atlandı: $pluginName - $message"
        } else {
            ". $count plugins skipped: $pluginName - $message"
        }

    fun noPluginEnabled(pluginName: String, message: String): String =
        if (turkish) {
            "Hiçbir eklenti aktif edilemedi: $pluginName - $message"
        } else {
            "No plugins could be enabled: $pluginName - $message"
        }

    fun onlyPendingOperations(count: Int): String =
        if (turkish) "$count işlem zaten sürüyor." else "$count operations are already in progress."

    fun currentVersion(version: Int): String =
        if (turkish) "Güncel v$version" else "Current v$version"

    fun installedVersion(version: Int): String =
        if (turkish) "Kurulu v$version" else "Installed v$version"

    fun compatibilityReason(platformSupport: CloudStreamPlatformSupport): String =
        when (platformSupport) {
            CloudStreamPlatformSupport.AndroidAndIos -> {
                if (turkish) {
                    "Bu provider, Nuvio Enhanced içine derlenmiş incelenmiş çapraz platform adaptörü kullanır."
                } else {
                    "This provider has a reviewed cross-platform adapter compiled into Nuvio Enhanced."
                }
            }
            CloudStreamPlatformSupport.AndroidOnly -> {
                if (turkish) {
                    "Android full sürümü bu standart CloudStream .cs3 paketini gömülü CloudStream çalışma zamanı ile çalıştırır."
                } else {
                    "Android full builds execute this standard CloudStream .cs3 package with the embedded CloudStream runtime."
                }
            }
            CloudStreamPlatformSupport.Unsupported -> {
                if (turkish) {
                    "Bu standart .cs3 paketi Android DEX kodu içerir ve iOS üzerinde çalışamaz."
                } else {
                    "This standard .cs3 package contains Android DEX code, which cannot run on iOS."
                }
            }
        }

    fun pluginInstalled(name: String): String =
        if (turkish) "$name paketi denetlendi ve kuruldu." else "$name package was checked and installed."

    companion object {
        fun forLanguage(language: AppLanguage): CloudStreamSettingsCopy =
            CloudStreamSettingsCopy(turkish = language == AppLanguage.TURKISH)
    }
}

private fun String.localizedCloudStreamFailure(copy: CloudStreamSettingsCopy): String =
    when (this) {
        "Accept the third-party plugin security warning before installation",
        "Accept the third-party plugin security warning.",
        "Üçüncü taraf eklenti güvenlik uyarısını kabul edin." -> copy.acceptSecurityWarningFailure
        "CloudStream plugin was not found",
        "Plugin was not found.",
        "Eklenti bulunamadı." -> copy.pluginNotFoundFailure
        "This CloudStream plugin is marked as down",
        "This plugin appears to be down.",
        "Bu eklenti şu anda kapalı görünüyor." -> copy.pluginDownFailure
        "The package was installed, but could not be enabled because verification or compatibility checks failed.",
        "Paket kuruldu ama doğrulama/uyumluluk şartları nedeniyle aktif edilemedi." -> copy.packageEnableFailure
        else -> this
    }

@Composable
private fun CloudStreamFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
