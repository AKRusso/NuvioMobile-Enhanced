package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.ai.AiAssistantSettings
import com.nuvio.app.features.ai.AiAssistantSettingsRepository
import com.nuvio.app.features.ai.AiProvider
import com.nuvio.app.features.ai.displayName
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_save
import nuvio.composeapp.generated.resources.ai_settings_api_key
import nuvio.composeapp.generated.resources.ai_settings_api_key_help
import nuvio.composeapp.generated.resources.ai_settings_enable
import nuvio.composeapp.generated.resources.ai_settings_enable_description
import nuvio.composeapp.generated.resources.ai_settings_get_key
import nuvio.composeapp.generated.resources.ai_settings_fallback_help
import nuvio.composeapp.generated.resources.ai_settings_model
import nuvio.composeapp.generated.resources.ai_settings_model_help
import nuvio.composeapp.generated.resources.ai_settings_provider
import nuvio.composeapp.generated.resources.ai_settings_section_access
import nuvio.composeapp.generated.resources.ai_settings_section_general
import nuvio.composeapp.generated.resources.ai_settings_section_web_search
import nuvio.composeapp.generated.resources.ai_settings_tavily_description
import nuvio.composeapp.generated.resources.ai_settings_tavily_key
import nuvio.composeapp.generated.resources.ai_settings_web_search
import nuvio.composeapp.generated.resources.ai_settings_web_search_description
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.aiAssistantSettingsContent(
    isTablet: Boolean,
    settings: AiAssistantSettings,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.ai_settings_section_general),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.ai_settings_enable),
                    description = stringResource(Res.string.ai_settings_enable_description),
                    checked = settings.enabled,
                    isTablet = isTablet,
                    onCheckedChange = AiAssistantSettingsRepository::setEnabled,
                )
                SettingsGroupDivider(isTablet = isTablet)
                ProviderSelector(isTablet = isTablet, selected = settings.provider)
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.ai_settings_section_access),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                ProviderCredentials(
                    isTablet = isTablet,
                    provider = settings.provider,
                    apiKey = settings.activeApiKey,
                    model = settings.activeModel,
                )
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.ai_settings_section_web_search),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.ai_settings_web_search),
                    description = stringResource(Res.string.ai_settings_web_search_description),
                    checked = settings.webSearchEnabled,
                    enabled = settings.tavilyApiKey.isNotBlank(),
                    isTablet = isTablet,
                    onCheckedChange = AiAssistantSettingsRepository::setWebSearchEnabled,
                )
                SettingsGroupDivider(isTablet = isTablet)
                TavilyCredentials(
                    isTablet = isTablet,
                    apiKey = settings.tavilyApiKey,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ProviderSelector(
    isTablet: Boolean,
    selected: AiProvider,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTablet) 20.dp else 16.dp,
                vertical = if (isTablet) 16.dp else 14.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.ai_settings_provider),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AiProvider.entries.forEach { provider ->
                FilterChip(
                    selected = provider == selected,
                    onClick = { AiAssistantSettingsRepository.setProvider(provider) },
                    label = { Text(provider.displayName) },
                )
            }
        }
        Text(
            text = stringResource(Res.string.ai_settings_fallback_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProviderCredentials(
    isTablet: Boolean,
    provider: AiProvider,
    apiKey: String,
    model: String,
) {
    val uriHandler = LocalUriHandler.current
    var apiKeyDraft by rememberSaveable(provider, apiKey) { mutableStateOf(apiKey) }
    var modelDraft by rememberSaveable(provider, model) { mutableStateOf(model) }
    val keyUrl = when (provider) {
        AiProvider.CEREBRAS -> "https://cloud.cerebras.ai/"
        AiProvider.GROQ -> "https://console.groq.com/keys"
        AiProvider.GEMINI -> "https://aistudio.google.com/app/apikey"
        AiProvider.OPENROUTER -> "https://openrouter.ai/settings/keys"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTablet) 20.dp else 16.dp,
                vertical = if (isTablet) 16.dp else 14.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = provider.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.ai_settings_api_key_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsSecretTextField(
            value = apiKeyDraft,
            onValueChange = { apiKeyDraft = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.ai_settings_api_key),
        )
        OutlinedTextField(
            value = modelDraft,
            onValueChange = { modelDraft = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(Res.string.ai_settings_model)) },
        )
        Text(
            text = stringResource(Res.string.ai_settings_model_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                enabled = apiKeyDraft.trim() != apiKey || modelDraft.trim() != model,
                onClick = {
                    when (provider) {
                        AiProvider.CEREBRAS -> {
                            AiAssistantSettingsRepository.setCerebrasApiKey(apiKeyDraft)
                            AiAssistantSettingsRepository.setCerebrasModel(modelDraft)
                        }
                        AiProvider.GROQ -> {
                            AiAssistantSettingsRepository.setGroqApiKey(apiKeyDraft)
                            AiAssistantSettingsRepository.setGroqModel(modelDraft)
                        }
                        AiProvider.GEMINI -> {
                            AiAssistantSettingsRepository.setGeminiApiKey(apiKeyDraft)
                            AiAssistantSettingsRepository.setGeminiModel(modelDraft)
                        }
                        AiProvider.OPENROUTER -> {
                            AiAssistantSettingsRepository.setOpenRouterApiKey(apiKeyDraft)
                            AiAssistantSettingsRepository.setOpenRouterModel(modelDraft)
                        }
                    }
                },
            ) {
                Text(stringResource(Res.string.action_save))
            }
            OutlinedButton(onClick = { uriHandler.openUri(keyUrl) }) {
                Text(stringResource(Res.string.ai_settings_get_key))
            }
        }
    }
}

@Composable
private fun TavilyCredentials(
    isTablet: Boolean,
    apiKey: String,
) {
    val uriHandler = LocalUriHandler.current
    var draft by rememberSaveable(apiKey) { mutableStateOf(apiKey) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTablet) 20.dp else 16.dp,
                vertical = if (isTablet) 16.dp else 14.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Tavily",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.ai_settings_tavily_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsSecretTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.ai_settings_tavily_key),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                enabled = draft.trim() != apiKey,
                onClick = {
                    AiAssistantSettingsRepository.setTavilyApiKey(draft)
                    if (draft.isNotBlank()) {
                        AiAssistantSettingsRepository.setWebSearchEnabled(true)
                    }
                },
            ) {
                Text(stringResource(Res.string.action_save))
            }
            OutlinedButton(
                onClick = { uriHandler.openUri("https://app.tavily.com/home") },
            ) {
                Text(stringResource(Res.string.ai_settings_get_key))
            }
        }
    }
}
