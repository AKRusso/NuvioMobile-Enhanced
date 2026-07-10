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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
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
import com.nuvio.app.core.ui.NuvioInfoBadge
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.features.cloudstream.AddCloudStreamRepositoryResult
import com.nuvio.app.features.cloudstream.CloudStreamInstallResult
import com.nuvio.app.features.cloudstream.CloudStreamPluginItem
import com.nuvio.app.features.cloudstream.CloudStreamRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CloudStreamSettingsSection() {
    LaunchedEffect(Unit) { CloudStreamRepository.initialize() }
    val state by CloudStreamRepository.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var repositoryUrl by rememberSaveable { mutableStateOf("") }
    var editingRepositoryUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var isAddingRepository by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedLanguage by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

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

    NuvioSectionLabel("CloudStream repository ve eklentileri")
    NuvioSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NuvioInfoBadge(text = "${state.repositories.size} repository")
            NuvioInfoBadge(text = "${state.plugins.count { it.isInstalled }} kurulu")
            NuvioInfoBadge(text = "${state.plugins.count { it.isRunnable }} etkin provider")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Standart .cs3 paketleri Android DEX kodu içerir. Nuvio yalnızca iki platform için uygulamaya derlenmiş, incelenmiş uyumluluk adaptörlerini çalıştırır; diğer eklentiler açıkça uyumsuz görünür.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (!state.securityWarningAccepted) {
        NuvioSurfaceCard {
            Text(
                text = "Üçüncü taraf kod güvenlik uyarısı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Repository ve eklentiler Nuvio tarafından yönetilmez. Yalnızca güvendiğiniz kaynakları ekleyin. Paket hash'i doğrulanmadan provider etkinleştirilemez.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            NuvioPrimaryButton(
                text = "Uyarıyı okudum ve kabul ediyorum",
                onClick = CloudStreamRepository::acceptSecurityWarning,
            )
        }
    }

    NuvioSurfaceCard {
        Text(
            text = if (editingRepositoryUrl == null) "CloudStream repository ekle" else "Repository bağlantısını düzenle",
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
                isAddingRepository -> "Repository okunuyor…"
                editingRepositoryUrl != null -> "Değişikliği uygula"
                else -> "Repository ekle"
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
                        message = "Repository yenileniyor."
                    } else {
                        when (val result = CloudStreamRepository.addRepository(requested)) {
                            is AddCloudStreamRepositoryResult.Success -> {
                                previous?.let(CloudStreamRepository::removeRepository)
                                repositoryUrl = ""
                                editingRepositoryUrl = null
                                message = "${result.repository.name} eklendi."
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
                    Icon(Icons.Rounded.Edit, contentDescription = "Repository düzenle")
                }
                IconButton(onClick = { CloudStreamRepository.refreshRepository(repository.manifest.sourceUrl) }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Repository yenile")
                }
                IconButton(onClick = { CloudStreamRepository.removeRepository(repository.manifest.sourceUrl) }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Repository kaldır")
                }
            }
        }
    }

    if (state.plugins.isNotEmpty()) {
        NuvioSurfaceCard {
            Text("Eklenti ara ve filtrele", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            NuvioInputField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Eklenti, açıklama veya yazar ara",
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CloudStreamFilterChip("Tüm diller", selectedLanguage == null) { selectedLanguage = null }
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
                    CloudStreamFilterChip("Tüm türler", selectedType == null) { selectedType = null }
                    types.forEach { type ->
                        CloudStreamFilterChip(type, selectedType == type) {
                            selectedType = type.takeUnless { selectedType == type }
                        }
                    }
                }
            }
        }
    }

    visiblePlugins.forEach { plugin ->
        CloudStreamPluginCard(
            plugin = plugin,
            securityWarningAccepted = state.securityWarningAccepted,
            onMessage = { message = it },
        )
    }
}

@Composable
private fun CloudStreamPluginCard(
    plugin: CloudStreamPluginItem,
    securityWarningAccepted: Boolean,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
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
                    plugin.metadata.authors.joinToString().ifBlank { "Yazar belirtilmemiş" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = plugin.enabled,
                enabled = plugin.isInstalled && plugin.verified && plugin.compatibility.isRunnable,
                onCheckedChange = { CloudStreamRepository.setPluginEnabled(plugin.metadata.id.value, it) },
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            plugin.metadata.description ?: "Açıklama yok.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NuvioInfoBadge(text = "Güncel v${plugin.metadata.version}")
            plugin.installedVersion?.let { NuvioInfoBadge(text = "Kurulu v$it") }
            plugin.metadata.language?.let { NuvioInfoBadge(text = it.uppercase()) }
            plugin.metadata.rawTvTypes.forEach { NuvioInfoBadge(text = it) }
            NuvioInfoBadge(text = if (plugin.verified) "SHA-256 doğrulandı" else "Hash doğrulanmadı")
            NuvioInfoBadge(text = if (plugin.compatibility.isRunnable) "Android + iOS uyumlu" else "Runtime uyumsuz")
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = plugin.compatibility.reason,
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
                                is CloudStreamInstallResult.Success -> "${plugin.metadata.name} paketi doğrulandı ve kuruldu."
                                is CloudStreamInstallResult.Error -> result.message
                            },
                        )
                    }
                },
            ) {
                Text(
                    when {
                        plugin.isInstalling -> "İndiriliyor…"
                        plugin.hasUpdate -> "Güncelle"
                        plugin.isInstalled -> "Yeniden kur"
                        else -> "Kur"
                    },
                )
            }
            if (plugin.isInstalled) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { CloudStreamRepository.removePlugin(plugin.metadata.id.value) },
                ) {
                    Text("Kaldır")
                }
            }
        }
    }
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
