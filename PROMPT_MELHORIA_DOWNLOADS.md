# Prompt para Melhoria do Sistema de Downloads — Nuvio Enhanced

## Contexto

**Nuvio Enhanced** é um fork do CloudStream para Android TV, escrito em **Kotlin Multiplatform + Jetpack Compose (Compose Multiplatform)**. O sistema de downloads de episódios/filmes foi construído ao longo de 4 rounds iterativos (Rounds 4-7) e o APK funcional mais recente é `fulldebug_r7a.apk`.

**Objetivo:** Uma IA deve fazer uma **revisão completa**, corrigir todos os bugs identificados abaixo, melhorar arquitetura, UI/UX e consistência — **sem remover funcionalidade existente**, sem bump de versão, sem testes, sem release, sem commits.

---

## Arquivos Principais

| Arquivo | Linhas | Função |
|---|---|---|
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/details/components/EpisodeDownloadFlowSheet.kt` | ~1694 | UI completa da sheet: estados, passos, composables, helpers, botões, painel de settings |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadSourceResolver.kt` | ~1142 | Lógica: resolveOptions, autoSelectOptions, pickBestForQuality, EpisodeDownloadCoordinator.enqueue/enqueueAuto, toDownloadOptions, downloadOptionQualityHeight |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsRepository.kt` | ~933 | Queue: queueMode, findPlayableDownload, findPlayableDownloadByVideoId, mutações atômicas |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsModels.kt` | ~511 | Modelos de dados: DownloadQueueMode, DownloadPreferredQuality, PendingSourceSearchStatus, downloadLogicalContentKey(), groupByShow() |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/DownloadsScreen.kt` | ~696 | Tela principal de downloads: lista ativa, lista completa, pending source choice, awaiting source |
| `composeApp/src/commonMain/kotlin/com/nuvio/app/features/downloads/PendingSourceChoiceSheet.kt` | ~416 | Sheet para re-seleção manual de fonte em downloads pendentes |
| `composeApp/src/commonMain/composeResources/values/strings.xml` | (todos os locales) | Strings usadas pela feature |

---

## Funcionalidade Atual (Rounds 4-7)

### Passos do fluxo (enum EpisodeDownloadStep)

1. **Start** — Prompt "Download" / "Pick myself" (série com showStartPrompt).
2. **Episodes** — Seletor de episódios, settings (ícone Tune), `[Automatic]` (auto ON) ou `[Continue]` (auto OFF) — mutuamente exclusivos.
3. **Sources** — Seletor de fontes com filtro de qualidade, opções progressivas, `[Back][Automatic][Download]` (série manual), `[Automatic][Download]` (filme), `[Back][Automatic]` (auto).
4. **AutoSelecting** — Lista de progresso de seleção automática, mensagem de sem resultados, Cancel retorna ao passo anterior (cancela job em execução).
5. **Complete** — Resumo do lote ou mensagem "No sources" + Done fecha a sheet.

### Features implementadas

- **Queue mode**: queueMode (AllAtOnce / OneAtATime).
- **Logical keys**: chaves de deduplicação `parent|season|episode`.
- **Progressive resolveOptions** com callback onFound + toDownloadOptions() (extensão internal).
- **Scaled auto-select timeout** — timeout do lote calculado pelo número de episódios: rounds × 25s + 8s + 10s.
- **Botões mutuamente exclusivos**: Episodes mostra `[Automatic]` (gradiente animado) quando auto ON, `[Continue]` (primary padrão) quando auto OFF.
- **Auto shortcut no Sources** — utilizadores manuais podem disparar seleção automativa one-off.
- **Quality filter chips** no seletor manual: All + alturas distintas.
- **Painel de settings sempre visível** — toggle funciona nos estados ON e OFF.
- **Slide animation** — slideInVertically + fadeIn / slideOutVertically + fadeOut (240ms).
- **Cancel auto-selecting** cancela autoJob, retorna a Episodes (série) ou Sources (filme).
- **Mutações atômicas** via `_uiState.update{}`.
- **findPlayableDownloadByVideoId** escopado por parent.
- **NuvioPrimaryButton** com interactionSource para animação via rememberDownloadPress().
- **StillSearchingFooter** mostrando resolved/total no source picker.
- **Compact AutoSelecting** — fillMaxWidth + LazyColumn(heightIn(max=460.dp)).
- **Mensagem "No sources"** no Complete quando nada encontrado.

---

## Estrutura de Código Atual

### EpisodeDownloadFlowSheet.kt (linha ~155)

```kotlin
EpisodeDownloadFlowSheet(
    meta: MetaDetails,
    defaultEpisode: MetaVideo?,
    initialEpisodes: List<MetaVideo> = emptyList(),
    showStartPrompt: Boolean = true,
    onDismiss: () -> Unit,
)
```

**Variáveis de estado** (~170-205):
- selectedEpisodes, step, options, selectedOption, isLoadingSources, providerStatuses, result, settings, showSettings, episodeAutoStatuses, autoJob: Job?
- `useAutoSelect = !isMovie && settings.autoSelect && selectedEpisodes.isNotEmpty()`

**Funções internas** (~225-330):
- `loadSources()` — busca progressiva via resolveOptions com onFound, quebra no primeiro target com fontes.
- `startAutoDownload()` — atribui autoJob, executa enqueueAuto, transição AutoSelecting -> Complete.
- `cancelAutoSelecting()` — cancela autoJob, limpa statuses, step -> Episodes (série) ou Sources (filme).
- `dismissSheet()` — flag de guarda, sheetVisible=false, delay 280ms, depois onDismiss().

### Composables dos passos (privados)

| Composable | Linha | Params principais |
|---|---|---|
| EpisodeDownloadStart | 681 | episode, onDownloadDefault, onSelectByMe, onDismiss |
| EpisodeDownloadPicker | 726 | episodes, selectedEpisodes, onToggle, onSelectSeason, onClear, onContinue, onDismiss, useAutoSelect, onSettingsToggle |
| QualityFilterChip | 855 | label, selected, onClick |
| EpisodeDownloadSourcePicker | 884 | options, selectedOption, isLoading, providerStatuses, message, emptyMessage, showBack, useAutoSelect, showSettingsButton, onSettingsToggle, onSelect, onDownload, onAutomaticDownload, onBack |
| EpisodeDownloadComplete | 1077 | result, isMovie, onDone |
| EpisodeDownloadCard | 1134 | episode, fallbackArtwork, selected, onClick |
| SourceOptionCard | 1200 | option, selected, onClick |
| DownloadPrimaryButton | 1288 | text, onClick, modifier, enabled |
| SecondaryDownloadButton | 1313 | text, onClick, modifier |
| AutomaticDownloadButton | 1339 | text, onClick, modifier, enabled |
| DownloadSettingsPanel | 1394 | settings, onSettingsChange |
| DownloadToggle | 1499 | enabled, onToggle |
| DownloadSettingsChip | 1536 | text, selected, onClick |
| EpisodeDownloadAutoSelecting | 1575 | statuses, onCancel |
| ProviderSearchPanel | 565 | statuses, isLoading, emptyMessage |
| EpisodeDownloadLoading | 550 | — |

### DownloadSourceResolver.kt

```kotlin
internal data class DownloadSourceOption(
    providerAddonId, providerName, qualityKey, qualityLabel, stream, ...
)
internal data class EpisodeDownloadTarget(
    videoId, parentMetaId, parentMetaType, seasonNumber, episodeNumber, title, thumbnail, overview, embeddedStreams
)
internal data class EpisodeDownloadSettings(
    autoSelect = false, preferredQuality = Best, downloadMode = AllAtOnce
)
```

**Funções-chave:**
- `resolveOptions(contentType, target, onProgress, onFound)` — busca com timeouts, load de manifesto, onFound progressivo.
- `autoSelectOptions(...)` — usa scaled withTimeoutOrNull x rounds de MAX_CONCURRENT_EPISODE_SELECTIONS.
- `pickBestForQuality(options, preferredQuality)` — ordenação por qualidade.
- `downloadOptionQualityHeight(): Int` — extensão internal, extrai altura.
- `toDownloadOptions()` — extensão internal em List, deduplica opções.

**EpisodeDownloadCoordinator** (mesmo ficheiro, linha 636):
- `suspend fun enqueue(...)` — enqueue manual (escrita DB one-shot).
- `suspend fun enqueueAuto(...)` — seleção automática + download com progresso.
- MAX_CONCURRENT_EPISODE_SELECTIONS = 3, MAX_CONCURRENT_PROVIDERS = 4.

**Constantes de timeout:**
- DOWNLOAD_SOURCE_TOTAL_TIMEOUT_MS = 25_000L
- MANIFEST_LOAD_WAIT_MS = 8_000L
- Batch auto timeout (escalado): rounds * 25_000 + 8_000 + 10_000

### DownloadsRepository.kt

- queueMode: DownloadQueueMode (AllAtOnce / OneAtATime).
- findPlayableDownloadByVideoId(videoId, parentMetaId) — escopado por parent.
- findPlayableDownload(videoId) — busca geral.
- Queue com mutações atômicas via _uiState.update{}.

### DownloadsScreen.kt

- `pendingChoiceGroup` e `dismissedPromptKeys` usam `remember` (deviam ser `rememberSaveable`).
- Secção "Awaiting Source" com `PendingShowRow`, retry/cancel.
- `Waiting` icon no status.

### PendingSourceChoiceSheet.kt

- Resolve fontes apenas do PRIMEIRO episódio do grupo.
- Botão "Done" com string hard-coded em inglês.
- Retry sequencial sem paralelismo.

---

## BUGS IDENTIFICADOS (39 total)

### CRITICAL (5)

| # | Ficheiro:linha | Problema | Correção sugerida |
|---|---|---|---|
| 1 | `EpisodeDownloadFlowSheet.kt:493-518` | **Double-tap download** — sem guarda contra duplo toque, lança múltiplos enqueues duplicados. | Adicionar state `isEnqueuing`, desabilitar botão enquanto enqueuing, mostrar loading. |
| 2 | `EpisodeDownloadFlowSheet.kt:493-518` | **Sem loading indicator** durante enqueue manual — UI fica sem feedback. | Mostrar CircularProgressIndicator ou desabilitar botão com texto "Downloading...". |
| 16 | `DownloadsRepository.kt:640` | **`runBlocking` em callback IO** no `onFailure` — potencial deadlock ao resolver string. | Usar string literal hardcoded "Download failed" ou pré-resolver a string. |
| 17 | `DownloadsRepository.kt:23,603,632,653` | **`activeHandles` não é thread-safe** — HashMap simples acessado de múltiplas threads (main + IO callbacks). | Envolver acesso em `synchronized(downloadLock)` com `SynchronizedObject` do atomicfu (já usado no projeto). |
| 36 | `DownloadsRepository.kt` (global) | **Sem sincronização no singleton** — estado mutável (`activeHandles`, `hasLoaded`, `nextDownloadOrdinal`, `_uiState` reads) acessado de UI thread + IO callbacks sem atomicidade. | Criar `private val downloadLock = SynchronizedObject()` e envolver seções críticas. |

### HIGH (8)

| # | Ficheiro:linha | Problema | Correção sugerida |
|---|---|---|---|
| 37 | `EpisodeDownloadFlowSheet.kt:335-343` | **Race no dismiss** — delay de animação pode não chamar `onDismiss` se composable for descartado durante o delay. | Usar `DisposableEffect` para garantir que `onDismiss` é chamado no dispose, ou remover o delay e chamar `onDismiss` diretamente. |
| 4 | `EpisodeDownloadFlowSheet.kt:184-187` | **States não keyados a `meta.id`** — options/providerStatuses/isLoadingSources ficam stale se meta mudar. | Adicionar key: `remember(meta.id) { mutableStateOf(...) }` para options, providerStatuses, isLoadingSources. |
| 11 | `DownloadSourceResolver.kt:325,379` | **MutableList partilhado entre asyncs** — data race no `toList()` concorrente. | Usar `Mutex` do kotlinx.coroutines ou copiar a lista antes de publicar. |
| 18 | `DownloadsRepository.kt:24,27-30` | **`hasLoaded` race condition** — check-then-act sem sincronização. | Envolver em `synchronized(downloadLock)` ou usar `AtomicBoolean` do atomicfu. |
| 25 | `DownloadsModels.kt:295-302` | **`runBlocking` em `toastMessage()`** — pode bloquear main thread. | Usar strings hardcoded já que são mensagens fixas: "Download started", "Replaced", etc. |
| 28 | `DownloadsScreen.kt:81-82` | **`remember` em vez de `rememberSaveable`** — estado perdido em rotação de ecrã. | Mudar `pendingChoiceGroup` e `dismissedPromptKeys` para `rememberSaveable`. |
| 33 | `PendingSourceChoiceSheet.kt:83-87` | **Só resolve fontes do primeiro episódio** — fontes dos restantes podem falhar silenciosamente. | Iterar todos os episódios do grupo ou resolver em paralelo. |
| 12 | `EpisodeDownloadFlowSheet.kt:271-274` | **`break` após primeiro episódio com fontes** — episódios seguintes não são pesquisados no loadSources. | Remover break, ou pesquisar todos os targets e concatenar resultados. |

### MEDIUM (15)

| # | Ficheiro:linha | Problema | Correção sugerida |
|---|---|---|---|
| 6 | `EpisodeDownloadFlowSheet.kt:315-318` | Texto "Download Started" mesmo quando nada começou (falha total). | Mostrar "No sources found" quando started=0 e awaitingSource>0. |
| 7 | `EpisodeDownloadFlowSheet.kt:192,296,411` | `showSettings` não reseta ao trocar de step (via back navigation). | Reset `showSettings = false` no `onBack` do Sources step. |
| 8 | `EpisodeDownloadFlowSheet.kt:900` | Filtro de qualidade invalidado quando novas opções chegam — lista fica vazia. | Usar `LaunchedEffect(options)` para manter filtro se possível, ou resetar para null. |
| 13 | `DownloadSourceResolver.kt:458` | Sem progresso por provider no auto-select — utilizador sem visibilidade. | Passar callback de progresso por provider ao auto-select. |
| 14 | `DownloadSourceResolver.kt:1081-1095` | Fallback de qualidade silencioso — utilizador não é informado que a qualidade preferida não está disponível. | Mostrar toast ou indicator quando qualidade preferida é substituída. |
| 19 | `DownloadsRepository.kt:563-601` | Persistência em disco em cada update de progresso — I/O pesado, pode causar jank. | Debounce: persistir no máximo a cada 2-3 segundos durante download. |
| 20 | `DownloadsRepository.kt:179-191` | Race no `enqueueFromStream` — check-then-act no `_uiState.value`. | Usar `_uiState.update{}` para operação atômica read-modify-write. |
| 21 | `DownloadsRepository.kt:428` | `localFileUri` limpo ao retomer download — URI de ficheiro parcial perdida. | Não limpar localFileUri se o ficheiro parcial ainda existe. |
| 22 | `DownloadsRepository.kt:382-401` | Queue fica parada após pausa em OneAtATime — próximo download não arranca. | Chamar `promoteWaitingDownload()` após pausar (ou adicionar UI para retomar manualmente). |
| 29 | `DownloadsScreen.kt:84-92` | `LaunchedEffect` re-avalia prompts pendentes em cada mudança de estado — pode mostrar prompts inesperados. | Usar key mais estável ou filtrar melhor os grupos elegíveis. |
| 34 | `PendingSourceChoiceSheet.kt:380` | String hard-coded "Done" em vez de resource. | Usar `stringResource(Res.string.action_done)`. |
| 35 | `PendingSourceChoiceSheet.kt:202-208` | Retry de fontes pendentes sequencial (sem paralelismo). | Usar `coroutineScope` + `async` para retry paralelo. |
| 38 | `EpisodeDownloadFlowSheet.kt:414-417` | Settings salvas em disco a cada toggle (sem debounce). | Debounce de 500ms antes de persistir. |
| 39 | `DownloadsModels.kt:233-240` | Computed properties `activeItems`/`completedItems` criam listas novas a cada acesso. | Usar `remember` no caller ou cachear com deriveStateOf. |
| 26/32 | `DownloadsModels.kt:337` + `DownloadsScreen.kt:684` | Duplicação de `formatDownloadBytes`/`formatBytes`. | Unificar numa única função shared. |

### LOW (11)

| # | Ficheiro:linha | Problema |
|---|---|---|
| 3 | `EpisodeDownloadFlowSheet.kt:185` | `selectedOption` não limpa no back (latent state inconsistency). |
| 5 | `EpisodeDownloadFlowSheet.kt:286-321` | Stale options na re-entrada via auto-select. |
| 9 | `EpisodeDownloadFlowSheet.kt:362` | Recomposição desnecessária com `initialEpisodes` como key (referência muda). |
| 10 | `EpisodeDownloadFlowSheet.kt:354-358` | `autoJob` não cancelado no DisposableEffect (mas scope cancela). |
| 15 | `DownloadSourceResolver.kt:1049-1063` | Quality key não determinístico no fallback — deduplicação pode falhar. |
| 23 | `DownloadsRepository.kt:25,769` | `nextDownloadOrdinal` não persistido entre sessões. |
| 24 | `DownloadsRepository.kt:496-551` | Pending searches expirados não limpos no loadFromDisk. |
| 27 | `DownloadsModels.kt:155-160` | Sentinel `-1` para season/episode null — frágil mas não realista. |
| 30 | `DownloadsScreen.kt:94-98` | Null title quando show é deletado — fallback OK mas pode confundir. |
| 31 | `DownloadsScreen.kt:181` | `completedMovies` derived de `uiState` — funciona mas recomputa frequentemente. |
| 14 (parcial) | `DownloadSourceResolver.kt:1049-1063` | Fallback não determinístico no `downloadQualityKey()` — streams idênticos podem ter keys diferentes. |

---

## CÓDIGO DE DEBUG PARA REMOVER

Estes toasts de debug existem no código e **devem ser removidos**:

1. **`MetaDetailsScreen.kt:686`** — `NuvioToastController.show("DEBUG1: download tapped (episodes=true)")`
2. **`MetaDetailsScreen.kt:698`** — `NuvioToastController.show("DEBUG1: download tapped (episodes=false, movie)")`
3. **`StreamsScreen.kt:444`** — `NuvioToastController.show("DEBUG3: stream download tapped")`

---

## REGRAS E RESTRIÇÕES

1. **NÃO** fazer bump de versão, tags ou commits.
2. **NÃO** criar testes unitários ou usar emulador.
3. **NÃO remover funcionalidade existente** — apenas melhorar, reorganizar, corrigir bugs.
4. **Strings PT-BR** devem manter português do Brasil; novas strings adicionadas a TODOS os locales em values/strings.xml.
5. Output é um **único APK** `fulldebug_XXX.apk` em `androidApp/build/outputs/apk/full/debug/`, renomeado in-place via Move-Item (nunca Copy).
6. Build: `.\gradlew.bat :androidApp:assembleFullDebug --rerun-tasks --console=plain` (timeout ~1800000). Compile check: `.\gradlew.bat :composeApp:compileAndroidMain --console=plain`.
7. Convenção de APK: sufixo 3 caracteres. Último: `fulldebug_r7a.apk`. Próximo sugerido: `fulldebug_r8a.apk`.

---

## O QUE A IA DEVE FAZER

### 1. Corrigir todos os bugs CRITICAL (5)

Prioridade máxima. Estes bugs causam crashes, race conditions, dados duplicados ou deadlocks.

### 2. Corrigir todos os bugs HIGH (8)

Importantes para confiabilidade e experiência do utilizador.

### 3. Corrigir bugs MEDIUM (15)

Melhoram consistência, UX e performance.

### 4. Remover código de debug

Remover os 3 toasts DEBUG identificados acima.

### 5. Melhorias de Arquitetura (se possível)

- Decompor `EpisodeDownloadFlowSheet.kt` (1694 linhas) em ficheiros separados.
- Extrair helpers reutilizáveis (botões, chips, cards).
- Melhorar separação de concerns: lógica de negócio vs UI.
- Considerar extrair state logic para ViewModel ou state wrapper.

### 6. Melhorias de UI/UX

- Melhorar hierarquia visual: painel de settings, filtro de qualidade, lista de opções.
- Garantir consistência de animações.
- Verificar acessibilidade (contentDescription, tamanhos mínimos de toque).
- Melhorar feedback durante seleção automática.

### 7. Consistência com Nuvio Enhanced

- Verificar que componentes seguem o design system existente.
- Usar `MaterialTheme.nuvio.colors.surfaceSheet` para backgrounds de sheet.
- Usar `surfaceContainerHigh` para superfícies secundárias.
- Usar `primaryContainer` para estados ativos/selecionados.
- Usar `RoundedCornerShape(18.dp)` para cards/botões.

### 8. Compilar e Gerar APK

- Compilar com `.\gradlew.bat :composeApp:compileAndroidMain --console=plain` para verificar.
- Build APK com `.\gradlew.bat :androidApp:assembleFullDebug --rerun-tasks --console=plain`.
- Renomear in-place para `fulldebug_r8a.apk` (Move-Item).
- Abrir pasta do APK.
- Apresentar resumo em Português (PT-BR).

---

## NOTAS ADICIONAIS

- `downloadOptionQualityHeight()` é internal (accessível da sheet para filtro de qualidade).
- `QualityFilterChip`, estado `qualityFilter`, `qualityHeights`, `filteredOptions` estão definidos dentro de `EpisodeDownloadSourcePicker` — considerar tornar standalone.
- `rememberDownloadPress()` retorna `(MutableInteractionSource, Float)` onde Float é a escala de press.
- `rememberAutomaticActionBrush()` retorna brush com gradiente + glow para o botão Automatic pulsante.
- `PlatformBackHandler` importado de `com.nuvio.app.core.ui.PlatformBackHandler` — trata o back do remote Android TV.
- Design system existente do Nuvio: surfaceSheet para sheets, surfaceContainerHigh para secundários, primaryContainer para estados ativos, RoundedCornerShape(18.dp).
- Git status: repo tem muitos ficheiros modificados (baseline). EpisodeDownloadFlowSheet.kt é untracked.
- Histórico de rounds: Round 4 = fulldebug_cmE.apk, Round 5 = fulldebug_fa6.apk, Round 6 = fulldebug_r6a.apk, Round 7 = fulldebug_r7a.apk.
- Projeto usa `kotlinx.atomicfu` (SynchronizedObject + synchronized) extensivamente — seguir o padrão existente para thread-safety.
- String resource `action_done` existe em strings.xml (linha 8) — usar em vez de string hardcoded "Done".
- String resource `download_failed` existe em strings.xml (linha 1607) — mas `getString()` é suspend, então no `onFailure` callback usar string literal hardcoded.
