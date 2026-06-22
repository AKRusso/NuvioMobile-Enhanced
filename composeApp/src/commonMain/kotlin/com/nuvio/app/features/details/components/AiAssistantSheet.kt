package com.nuvio.app.features.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.features.ai.AiAssistantService
import com.nuvio.app.features.ai.AiAssistantSettings
import com.nuvio.app.features.ai.AiChatMessage
import com.nuvio.app.features.ai.AiChatRole
import com.nuvio.app.features.ai.AiProvider
import com.nuvio.app.features.details.MetaDetails
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.ai_chat_configure
import nuvio.composeapp.generated.resources.ai_chat_disclaimer
import nuvio.composeapp.generated.resources.ai_chat_error
import nuvio.composeapp.generated.resources.ai_chat_hint
import nuvio.composeapp.generated.resources.ai_chat_prompt_explain
import nuvio.composeapp.generated.resources.ai_chat_prompt_recommend
import nuvio.composeapp.generated.resources.ai_chat_prompt_similar
import nuvio.composeapp.generated.resources.ai_chat_title
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AiAssistantSheet(
    meta: MetaDetails,
    settings: AiAssistantSettings,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val messages = remember(meta.id, settings.provider) { mutableStateListOf<AiChatMessage>() }
    var draft by remember(meta.id) { mutableStateOf("") }
    var isSending by remember(meta.id) { mutableStateOf(false) }
    var error by remember(meta.id) { mutableStateOf<String?>(null) }
    val genericError = stringResource(Res.string.ai_chat_error)

    fun send(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank() || isSending || !settings.isReady) return
        draft = ""
        error = null
        messages += AiChatMessage(AiChatRole.USER, normalized)
        isSending = true
        scope.launch {
            runCatching {
                AiAssistantService.chat(settings, meta, messages.toList())
            }.onSuccess { answer ->
                messages += AiChatMessage(AiChatRole.ASSISTANT, answer)
            }.onFailure { throwable ->
                error = throwable.message ?: genericError
            }
            isSending = false
        }
    }

    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    NuvioModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Column {
                    Text(
                        text = stringResource(Res.string.ai_chat_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${meta.name} • ${if (settings.provider == AiProvider.GEMINI) "Gemini" else "OpenRouter Free"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!settings.isReady) {
                Text(
                    text = stringResource(Res.string.ai_chat_configure),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(18.dp),
                        )
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                if (messages.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.ai_chat_disclaimer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            stringResource(Res.string.ai_chat_prompt_recommend),
                            stringResource(Res.string.ai_chat_prompt_explain),
                            stringResource(Res.string.ai_chat_prompt_similar),
                        ).forEach { prompt ->
                            AssistChip(
                                onClick = { send(prompt) },
                                label = { Text(prompt) },
                            )
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(messages) { message ->
                        ChatBubble(message)
                    }
                    if (isSending) {
                        item {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }

                error?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.ai_chat_hint)) },
                    trailingIcon = {
                        IconButton(
                            onClick = { send(draft) },
                            enabled = draft.isNotBlank() && !isSending,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = null,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(draft) }),
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: AiChatMessage) {
    val isUser = message.role == AiChatRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = message.text,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(
                    color = if (isUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 5.dp,
                        bottomEnd = if (isUser) 5.dp else 18.dp,
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 11.dp),
            color = if (isUser) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
