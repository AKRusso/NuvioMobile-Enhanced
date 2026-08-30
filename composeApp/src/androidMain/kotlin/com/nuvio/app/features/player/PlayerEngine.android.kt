package com.nuvio.app.features.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.media.audiofx.LoudnessEnhancer
import android.text.SpannableString
import android.net.Uri
import android.util.Log
import android.util.TypedValue
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.view.SurfaceHolder
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.util.AttributeSet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ForwardingRenderer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.media3.ui.CaptionStyleCompat
import com.nuvio.app.R
import com.nuvio.app.features.streams.normalizeStreamType
import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.MPVNode
import `is`.xyz.mpv.Utils
import io.github.peerless2012.ass.media.widget.AssSubtitleView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "NuvioPlayer"
private const val PLAYER_DIAGNOSTIC_TAG = "NuvioPlayerDiag"
private const val AUDIO_ROUTE_ERROR_PREFIX = "audio-route:"

private fun DecoderCounters.videoDiagnosticSummary(): String {
    ensureUpdated()
    val averageProcessingOffsetUs = if (videoFrameProcessingOffsetCount > 0) {
        totalVideoFrameProcessingOffsetUs / videoFrameProcessingOffsetCount
    } else {
        0L
    }
    return "decoderCounters rendered=$renderedOutputBufferCount skipped=$skippedOutputBufferCount " +
        "dropped=$droppedBufferCount maxConsecutiveDropped=$maxConsecutiveDroppedBufferCount " +
        "averageProcessingOffsetUs=$averageProcessingOffsetUs"
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
actual fun PlatformPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String?,
    sourceHeaders: Map<String, String>,
    sourceResponseHeaders: Map<String, String>,
    externalSubtitles: List<com.nuvio.app.features.streams.StreamSubtitle>,
    streamType: String?,
    useYoutubeChunkedPlayback: Boolean,
    modifier: Modifier,
    playWhenReady: Boolean,
    initialPositionMs: Long?,
    initialPositionRequestKey: String?,
    resizeMode: PlayerResizeMode,
    useNativeController: Boolean,
    onInitialPositionHandled: (key: String, handled: Boolean) -> Unit,
    onControllerReady: (PlayerEngineController) -> Unit,
    onSnapshot: (PlayerPlaybackSnapshot) -> Unit,
    onError: (String?) -> Unit,
) {
    val playerSettings by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val normalizedStreamType = normalizeStreamType(streamType)
    val isDirectMatroska = normalizedStreamType.isMatroskaStreamType()
    val playerSourceKey = listOf(
        sourceUrl,
        sourceAudioUrl.orEmpty(),
        sanitizePlaybackHeaders(sourceHeaders),
        sanitizePlaybackResponseHeaders(sourceResponseHeaders),
        normalizedStreamType.orEmpty(),
        useYoutubeChunkedPlayback,
        initialPositionRequestKey.orEmpty(),
        externalSubtitles,
    )
    var activeEngine by remember(playerSourceKey, playerSettings.androidPlaybackEngine) {
        mutableStateOf(playerSettings.androidPlaybackEngine.initialAndroidEngine(normalizedStreamType))
    }
    when (activeEngine) {
        ResolvedAndroidPlaybackEngine.ExoPlayer -> ExoPlayerSurface(
            sourceUrl = sourceUrl,
            sourceAudioUrl = sourceAudioUrl,
            sourceHeaders = sourceHeaders,
            sourceResponseHeaders = sourceResponseHeaders,
            externalSubtitles = externalSubtitles,
            streamType = streamType,
            useYoutubeChunkedPlayback = useYoutubeChunkedPlayback,
            modifier = modifier,
            playWhenReady = playWhenReady,
            initialPositionMs = initialPositionMs,
            initialPositionRequestKey = initialPositionRequestKey,
            resizeMode = resizeMode,
            useNativeController = useNativeController,
            onInitialPositionHandled = onInitialPositionHandled,
            onControllerReady = onControllerReady,
            onSnapshot = onSnapshot,
            onError = { message ->
                if (message?.startsWith(AUDIO_ROUTE_ERROR_PREFIX) == true) {
                    // Audio route failures do not prove that the stream needs another
                    // engine. Keep the existing player/source and show the actual error.
                    onError(message.removePrefix(AUDIO_ROUTE_ERROR_PREFIX))
                } else if (message != null && playerSettings.androidPlaybackEngine == AndroidPlaybackEngine.Auto) {
                    Log.w(TAG, "ExoPlayer failed; falling back to libmpv: $message")
                    initialPositionRequestKey?.let { key ->
                        onInitialPositionHandled(key, false)
                    }
                    activeEngine = ResolvedAndroidPlaybackEngine.Libmpv
                    onError(null)
                } else {
                    onError(message)
                }
            },
        )
        ResolvedAndroidPlaybackEngine.Libmpv -> {
            LaunchedEffect(initialPositionRequestKey) {
                initialPositionRequestKey?.let { key ->
                    onInitialPositionHandled(key, false)
                }
            }
            LibmpvPlayerSurface(
                sourceUrl = sourceUrl,
                sourceAudioUrl = sourceAudioUrl,
                sourceHeaders = sourceHeaders,
                externalSubtitles = externalSubtitles,
                modifier = modifier,
                playWhenReady = playWhenReady,
                resizeMode = resizeMode,
                videoOutput = playerSettings.androidLibmpvVideoOutput,
                hardwareDecodingEnabled = playerSettings.androidLibmpvHardwareDecodingEnabled && !isDirectMatroska,
                yuv420pEnabled = playerSettings.androidLibmpvYuv420pEnabled,
                onControllerReady = onControllerReady,
                onSnapshot = onSnapshot,
                onError = onError,
            )
        }
    }
}

private enum class ResolvedAndroidPlaybackEngine {
    ExoPlayer,
    Libmpv,
}

private fun AndroidPlaybackEngine.initialAndroidEngine(streamType: String?): ResolvedAndroidPlaybackEngine =
    when {
        streamType.isMatroskaStreamType() -> ResolvedAndroidPlaybackEngine.Libmpv
        this == AndroidPlaybackEngine.ExoPlayer -> ResolvedAndroidPlaybackEngine.ExoPlayer
        this == AndroidPlaybackEngine.Libmpv -> ResolvedAndroidPlaybackEngine.Libmpv
        else -> ResolvedAndroidPlaybackEngine.ExoPlayer
    }

private fun String?.isMatroskaStreamType(): Boolean =
    this == "matroska" || this == "mkv"

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ExoPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String?,
    sourceHeaders: Map<String, String>,
    sourceResponseHeaders: Map<String, String>,
    externalSubtitles: List<com.nuvio.app.features.streams.StreamSubtitle>,
    streamType: String?,
    useYoutubeChunkedPlayback: Boolean,
    modifier: Modifier,
    playWhenReady: Boolean,
    initialPositionMs: Long?,
    initialPositionRequestKey: String?,
    resizeMode: PlayerResizeMode,
    useNativeController: Boolean,
    onInitialPositionHandled: (key: String, handled: Boolean) -> Unit,
    onControllerReady: (PlayerEngineController) -> Unit,
    onSnapshot: (PlayerPlaybackSnapshot) -> Unit,
    onError: (String?) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnSnapshot = rememberUpdatedState(onSnapshot)
    val latestOnError = rememberUpdatedState(onError)
    val latestOnInitialPositionHandled = rememberUpdatedState(onInitialPositionHandled)
    val latestPlayWhenReady = rememberUpdatedState(playWhenReady)
    val coroutineScope = rememberCoroutineScope()
    val playbackDiagnosticsEnabled = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    val playerSettings by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()

    val sanitizedSourceHeaders = remember(sourceHeaders) {
        sanitizePlaybackHeaders(sourceHeaders)
    }
    val sanitizedSourceResponseHeaders = remember(sourceResponseHeaders) {
        sanitizePlaybackResponseHeaders(sourceResponseHeaders)
    }
    val normalizedStreamType = remember(streamType) {
        normalizeStreamType(streamType)
    }
    val useLibass = playerSettings.subtitleStyle.shouldUseLibass(playerSettings.useLibass)
    val libassRenderType = runCatching {
        LibassRenderType.valueOf(playerSettings.libassRenderType)
    }.getOrDefault(LibassRenderType.CUES)
    val playerSourceKey = listOf(
        sourceUrl,
        sourceAudioUrl.orEmpty(),
        sanitizedSourceHeaders,
        sanitizedSourceResponseHeaders,
        normalizedStreamType.orEmpty(),
        useYoutubeChunkedPlayback,
        initialPositionRequestKey.orEmpty(),
    )
    val subtitleDelayUs = remember(playerSourceKey) { AtomicLong(0L) }
    var selectedExternalSubtitleMimeType by remember(playerSourceKey) { mutableStateOf<String?>(null) }
    val latestExternalSubtitleMimeType = rememberUpdatedState(selectedExternalSubtitleMimeType)
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var videoAspectRatio by remember(playerSourceKey) { mutableStateOf(0f) }
    val latestVideoAspectRatio = rememberUpdatedState(videoAspectRatio)
    var decoderPriorityOverride by remember(playerSourceKey) { mutableStateOf<Int?>(null) }
    var fallbackStartPositionMs by remember(playerSourceKey) { mutableStateOf<Long?>(null) }
    var audioRouteRecoveryAttempted by remember(playerSourceKey) { mutableStateOf(false) }
    val effectiveDecoderPriority = decoderPriorityOverride ?: playerSettings.decoderPriority

    val initialMediaItem = remember(playerSourceKey, externalSubtitles) {
        val subtitleConfigs = externalSubtitles.mapNotNull { subtitle ->
            val mimeType = resolveSubtitleMimeType(subtitle.url, subtitle.headers)
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.url))
                .setMimeType(mimeType)
                .setLanguage(subtitle.language)
                .setLabel(subtitle.name ?: subtitle.language)
                .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                .build()
        }
        playbackMediaItemFromUrl(
            url = sourceUrl,
            responseHeaders = sanitizedSourceResponseHeaders,
            streamType = normalizedStreamType,
        ).buildUpon()
            .setMediaId(sourceUrl)
            .apply {
                if (subtitleConfigs.isNotEmpty()) {
                    setSubtitleConfigurations(subtitleConfigs)
                }
            }
            .build()
    }

    var resolvedMediaItem by remember(playerSourceKey) { mutableStateOf(initialMediaItem) }
    var probeAttempted by remember(playerSourceKey) { mutableStateOf(false) }

    val extractorsFactory = remember {
        DefaultExtractorsFactory()
            .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
            .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)
    }
    val dataSourceFactory = remember(
        context,
        sourceUrl,
        sanitizedSourceHeaders,
        sanitizedSourceResponseHeaders,
        useYoutubeChunkedPlayback,
        externalSubtitles,
    ) {
        PlatformPlaybackDataSourceFactory.create(
            context = context,
            defaultRequestHeaders = sanitizedSourceHeaders,
            defaultResponseHeaders = sanitizedSourceResponseHeaders,
            useYoutubeChunkedPlayback = useYoutubeChunkedPlayback,
            useLongReadTimeout = isLoopbackPlaybackSource(sourceUrl),
            externalSubtitles = externalSubtitles,
        )
    }

    fun ExoPlayer.setPlaybackMediaItem(videoMediaItem: MediaItem, startPositionMs: Long? = null) {
        if (!sourceAudioUrl.isNullOrBlank()) {
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)
            val videoSource = mediaSourceFactory.createMediaSource(videoMediaItem)
            val audioSource = mediaSourceFactory.createMediaSource(playbackMediaItemFromUrl(sourceAudioUrl))
            val mergedSource = MergingMediaSource(videoSource, audioSource)
            if (startPositionMs != null) {
                setMediaSource(mergedSource, startPositionMs.coerceAtLeast(0L))
            } else {
                setMediaSource(mergedSource)
            }
        } else if (startPositionMs != null) {
            setMediaItem(videoMediaItem, startPositionMs.coerceAtLeast(0L))
        } else {
            setMediaItem(videoMediaItem)
        }
    }

    val exoPlayer = remember(
        sourceUrl,
        sourceAudioUrl,
        sanitizedSourceHeaders,
        sanitizedSourceResponseHeaders,
        normalizedStreamType,
        useYoutubeChunkedPlayback,
        effectiveDecoderPriority,
        useLibass,
        libassRenderType,
        playerSettings.androidMemorySafeBufferEnabled,
        playerSettings.mapDV7ToHevc,
        playerSettings.tunnelingEnabled,
        initialPositionRequestKey,
    ) {
        val renderersFactory = SubtitleOffsetRenderersFactory(
            context = context,
            subtitleDelayUsProvider = subtitleDelayUs::get,
            shouldNormalizeCuePositionProvider = {
                latestExternalSubtitleMimeType.value == MimeTypes.TEXT_VTT
            },
            videoBoundsFractionProvider = {
                playerViewRef?.videoBoundsFraction(latestVideoAspectRatio.value)
            },
        )
            .setExtensionRendererMode(effectiveDecoderPriority)
            .setEnableDecoderFallback(true)
            .setMapDV7ToHevc(playerSettings.mapDV7ToHevc)

        val trackSelector = DefaultTrackSelector(context).apply {
            var parameters = buildUponParameters()
                .setAllowInvalidateSelectionsOnRendererCapabilitiesChange(true)
            if (playerSettings.tunnelingEnabled) {
                parameters = parameters.setTunnelingEnabled(true)
            }
            val captioningManager = context.getSystemService(Context.CAPTIONING_SERVICE)
                as? android.view.accessibility.CaptioningManager
            if (captioningManager != null) {
                if (!captioningManager.isEnabled) {
                    parameters = parameters.setIgnoredTextSelectionFlags(
                        parameters.build().ignoredTextSelectionFlags or C.SELECTION_FLAG_DEFAULT,
                    )
                }
                captioningManager.locale?.let { locale ->
                    parameters = parameters.setPreferredTextLanguage(locale.isO3Language)
                }
            }
            if (playerSettings.subtitleStyle.useForcedSubtitles) {
                parameters = parameters.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else {
                parameters = parameters.setIgnoredTextSelectionFlags(
                    parameters.build().ignoredTextSelectionFlags or C.SELECTION_FLAG_FORCED,
                )
            }
            setParameters(parameters)
        }

        val loadControl = buildAndroidLoadControl(playerSettings.androidMemorySafeBufferEnabled)

        val player = if (useLibass) {
            ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .buildWithAssSupportCompat(
                    context = context,
                    renderType = libassRenderType.toAssRenderType(),
                    dataSourceFactory = dataSourceFactory,
                    extractorsFactory = extractorsFactory,
                    renderersFactory = renderersFactory
                )
        } else {
            val mediaSourceFactory = DefaultMediaSourceFactory(
                dataSourceFactory,
                extractorsFactory,
            )

            ExoPlayer.Builder(context)
                .setRenderersFactory(renderersFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
        }

        player.applySubtitleTrackPreferences(
            preferredLanguage = playerSettings.preferredSubtitleLanguage,
            useForcedSubtitles = playerSettings.subtitleStyle.useForcedSubtitles,
            autoSelectionApplied = false,
            hasActiveSubtitle = false,
            useCustomSubtitles = false,
        )
        player
    }

    val nowPlayingController = remember(context, exoPlayer) {
        AndroidPlayerNowPlayingController(
            context = context,
            controls = AndroidPlayerNowPlayingController.PlaybackControls(
                play = {
                    exoPlayer.playWhenReady = true
                    exoPlayer.play()
                },
                pause = exoPlayer::pause,
                seekTo = { positionMs -> exoPlayer.seekTo(positionMs.coerceAtLeast(0L)) },
                seekBy = { offsetMs ->
                    exoPlayer.seekTo((exoPlayer.currentPosition + offsetMs).coerceAtLeast(0L))
                },
            ),
        )
    }

    fun dispatchExoPlayerSnapshot() {
        val snapshot = exoPlayer.snapshot()
        latestOnSnapshot.value(snapshot)
        nowPlayingController.syncPlayback(snapshot)
    }

    DisposableEffect(nowPlayingController) {
        onDispose { nowPlayingController.release() }
    }

    LaunchedEffect(exoPlayer, resolvedMediaItem, initialPositionRequestKey) {
        val mediaItem = resolvedMediaItem
        val requestedStartPositionMs = fallbackStartPositionMs
            ?: initialPositionMs?.takeIf { it > 0L }
        exoPlayer.setPlaybackMediaItem(mediaItem, requestedStartPositionMs)
        if (fallbackStartPositionMs == null) {
            initialPositionRequestKey?.let { key ->
                latestOnInitialPositionHandled.value(
                    key,
                    requestedStartPositionMs != null,
                )
            }
        }
        exoPlayer.prepare()
    }

    val pendingSubtitleTrackIndex = remember { mutableListOf<Int>() }
    val pendingAudioTrackSelection = remember { mutableListOf<TrackSelectionSnapshot>() }
    var currentSubtitleStyle by remember { mutableStateOf(SubtitleStyleState.DEFAULT) }
    var subtitleSelectionJob by remember { mutableStateOf<Job?>(null) }
    var loudnessEnhancer by remember(exoPlayer) { mutableStateOf<LoudnessEnhancer?>(null) }
    var loudnessEnhancerSessionId by remember(exoPlayer) { mutableStateOf(C.AUDIO_SESSION_ID_UNSET) }
    var requestedVolumeBoost by remember(exoPlayer) {
        mutableStateOf(playerSettings.volumeBoostPercent / 100f)
    }
    val isInPip = rememberIsInPictureInPicture()
    val pipSubtitleScale by rememberUpdatedState(if (isInPip) 0.4f else 1.0f)
    val sidecarController = remember(exoPlayer, coroutineScope) {
        SidecarSubtitleController(
            scope = coroutineScope,
            getPlayer = { exoPlayer },
            getSubtitleDelayMs = { (subtitleDelayUs.get() / 1_000L).toInt() },
        )
    }

    fun syncPlayerViewKeepScreenOn() {
        playerViewRef?.keepScreenOn = exoPlayer.shouldKeepPlayerScreenOn()
    }

    fun applyExoVolumeBoost(
        multiplier: Float,
        sessionId: Int = exoPlayer.audioSessionId,
    ) {
        val boost = multiplier.coerceIn(1f, 2f)
        requestedVolumeBoost = boost
        val targetGainMb = ((boost - 1f) * 1200f).toInt().coerceIn(0, 1200)
        if (loudnessEnhancerSessionId != sessionId) {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            loudnessEnhancerSessionId = C.AUDIO_SESSION_ID_UNSET
        }
        if (targetGainMb <= 0 || sessionId == C.AUDIO_SESSION_ID_UNSET) {
            loudnessEnhancer?.enabled = false
            exoPlayer.volume = 1f
            return
        }
        val enhancer = loudnessEnhancer ?: runCatching {
            LoudnessEnhancer(sessionId).also {
                loudnessEnhancer = it
                loudnessEnhancerSessionId = sessionId
            }
        }.getOrNull()
        enhancer?.let {
            runCatching {
                it.setTargetGain(targetGainMb)
                it.enabled = true
            }.onFailure { error ->
                Log.w(TAG, "Failed to apply LoudnessEnhancer volume boost", error)
            }
        }
        exoPlayer.volume = 1f
    }

    fun preserveAudioSelectionForReload(reason: String) {
        pendingAudioTrackSelection.clear()
        val selection = exoPlayer.captureSelectedTrack(C.TRACK_TYPE_AUDIO) ?: return
        pendingAudioTrackSelection.add(selection)
        Log.d(TAG, "$reason: preserving audio track index=${selection.index} id=${selection.id}")
    }

    DisposableEffect(exoPlayer) {
        PlayerPictureInPictureManager.registerPausePlaybackCallback {
            exoPlayer.pause()
        }
        PlayerPictureInPictureManager.registerTogglePlaybackCallback {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    exoPlayer.seekTo(0L)
                }
                exoPlayer.play()
            }
        }

        fun reportPlayerError(error: PlaybackException) {
            if (
                playerSettings.decoderPriority == DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON &&
                effectiveDecoderPriority != DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER &&
                error.isDecoderFailure()
            ) {
                Log.w(
                    TAG,
                    "Decoder failure (${error.errorCodeName}); retrying with app decoders",
                    error,
                )
                fallbackStartPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                decoderPriorityOverride = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                latestOnError.value(null)
                return
            }
            val message = error.localizedMessage ?: runBlocking { getString(Res.string.player_unable_to_play_stream) }
            latestOnError.value(
                if (error.isAudioRouteFailure()) "$AUDIO_ROUTE_ERROR_PREFIX$message" else message,
            )
        }

        val listener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                applyExoVolumeBoost(requestedVolumeBoost, audioSessionId)
            }

            override fun onPlayerError(error: PlaybackException) {
                syncPlayerViewKeepScreenOn()
                if (error.isAudioRouteFailure() && !audioRouteRecoveryAttempted) {
                    audioRouteRecoveryAttempted = true
                    val positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val shouldResume = exoPlayer.playWhenReady
                    val playbackSpeed = exoPlayer.playbackParameters.speed
                    preserveAudioSelectionForReload("audio route recovery")
                    Log.w(
                        TAG,
                        "Audio route changed; retrying the existing ExoPlayer source at $positionMs ms",
                        error,
                    )
                    coroutineScope.launch {
                        // AudioTrack can still be releasing the old route immediately after
                        // a Bluetooth transition. Reuse the same player and media item once
                        // the route has settled instead of changing engines/reloading URLs.
                        delay(350L)
                        exoPlayer.prepare()
                        exoPlayer.seekTo(positionMs)
                        exoPlayer.setPlaybackSpeed(playbackSpeed)
                        exoPlayer.playWhenReady = shouldResume
                    }
                    return
                }

                val isSourceError = error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                        error.cause?.toString()?.contains("UnrecognizedInputFormatException") == true

                if (isSourceError && !probeAttempted) {
                    probeAttempted = true
                    coroutineScope.launch {
                        val probedMime = withContext(Dispatchers.IO) {
                            probeMimeType(sourceUrl, sanitizedSourceHeaders)
                        }
                        if (probedMime != null) {
                            Log.d(TAG, "Playback failed with source error. Probed MIME type: $probedMime. Retrying...")
                            resolvedMediaItem = MediaItem.Builder()
                                .setUri(sourceUrl)
                                .setMimeType(probedMime)
                                .setMediaId(sourceUrl)
                                .apply {
                                    val subtitleConfigs = externalSubtitles.mapNotNull { subtitle ->
                                        val mimeType = resolveSubtitleMimeType(subtitle.url, subtitle.headers)
                                        MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.url))
                                            .setMimeType(mimeType)
                                            .setLanguage(subtitle.language)
                                            .setLabel(subtitle.name ?: subtitle.language)
                                            .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                                            .build()
                                    }
                                    if (subtitleConfigs.isNotEmpty()) {
                                        setSubtitleConfigurations(subtitleConfigs)
                                    }
                                }
                                .build()
                            latestOnError.value(null)
                            return@launch
                        }
                        reportPlayerError(error)
                    }
                    return
                }

                reportPlayerError(error)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateName = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN($playbackState)"
                }
                Log.d(TAG, "onPlaybackStateChanged: $stateName")
                if (playbackDiagnosticsEnabled) {
                    Log.i(
                        PLAYER_DIAGNOSTIC_TAG,
                        "state=$stateName positionMs=${exoPlayer.currentPosition.coerceAtLeast(0L)} " +
                            "bufferedDurationMs=${exoPlayer.totalBufferedDuration.coerceAtLeast(0L)} " +
                            "durationMs=${exoPlayer.duration.coerceAtLeast(0L)} " +
                            "playWhenReady=${exoPlayer.playWhenReady} isPlaying=${exoPlayer.isPlaying}",
                    )
                }
                if (playbackState == Player.STATE_READY) {
                    fallbackStartPositionMs = null
                    latestOnError.value(null)
                    exoPlayer.logCurrentTracks("STATE_READY")
                }
                syncPlayerViewKeepScreenOn()
                dispatchExoPlayerSnapshot()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                syncPlayerViewKeepScreenOn()
                dispatchExoPlayerSnapshot()
            }

            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                dispatchExoPlayerSnapshot()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                latestOnSnapshot.value(exoPlayer.snapshot())
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                }
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                Log.d(TAG, "onTracksChanged: ${tracks.groups.size} groups total")
                exoPlayer.logCurrentTracks("onTracksChanged")
                pendingAudioTrackSelection.firstOrNull()?.let { selection ->
                    if (tracks.groups.any { it.type == C.TRACK_TYPE_AUDIO }) {
                        pendingAudioTrackSelection.clear()
                        val restored = exoPlayer.restoreTrackSelection(selection)
                        Log.d(TAG, "onTracksChanged: restored pending audio selection=$restored")
                    }
                }
                if (pendingSubtitleTrackIndex.isNotEmpty() && tracks.groups.isNotEmpty()) {
                    val idx = pendingSubtitleTrackIndex.removeAt(0)
                    Log.d(TAG, "onTracksChanged: applying pending subtitle selection index=$idx")
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, idx < 0)
                        .build()
                    if (idx >= 0) {
                        exoPlayer.selectTrackByIndex(C.TRACK_TYPE_TEXT, idx)
                    }
                }
                dispatchExoPlayerSnapshot()
            }

        }
        val analyticsListener = if (playbackDiagnosticsEnabled) object : AnalyticsListener {
            override fun onPositionDiscontinuity(
                eventTime: AnalyticsListener.EventTime,
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                val reasonName = when (reason) {
                    Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> "AUTO_TRANSITION"
                    Player.DISCONTINUITY_REASON_SEEK -> "SEEK"
                    Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> "SEEK_ADJUSTMENT"
                    Player.DISCONTINUITY_REASON_SKIP -> "SKIP"
                    Player.DISCONTINUITY_REASON_REMOVE -> "REMOVE"
                    Player.DISCONTINUITY_REASON_INTERNAL -> "INTERNAL"
                    Player.DISCONTINUITY_REASON_SILENCE_SKIP -> "SILENCE_SKIP"
                    else -> "UNKNOWN($reason)"
                }
                Log.i(
                    PLAYER_DIAGNOSTIC_TAG,
                    "discontinuity reason=$reasonName oldPositionMs=${oldPosition.positionMs} " +
                        "newPositionMs=${newPosition.positionMs} eventPositionMs=${eventTime.eventPlaybackPositionMs}",
                )
            }

            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long,
            ) {
                Log.i(
                    PLAYER_DIAGNOSTIC_TAG,
                    "videoDecoder name=$decoderName initializationDurationMs=$initializationDurationMs " +
                        "positionMs=${eventTime.eventPlaybackPositionMs}",
                )
            }

            override fun onVideoInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?,
            ) {
                Log.i(
                    PLAYER_DIAGNOSTIC_TAG,
                    "videoFormat mime=${format.sampleMimeType ?: "unknown"} codecs=${format.codecs ?: "unknown"} " +
                        "size=${format.width}x${format.height} fps=${format.frameRate} " +
                        "reuse=${decoderReuseEvaluation?.result ?: "none"} positionMs=${eventTime.eventPlaybackPositionMs}",
                )
            }

            override fun onRenderedFirstFrame(
                eventTime: AnalyticsListener.EventTime,
                output: Any,
                renderTimeMs: Long,
            ) {
                Log.i(
                    PLAYER_DIAGNOSTIC_TAG,
                    "firstFrame renderTimeMs=$renderTimeMs positionMs=${eventTime.eventPlaybackPositionMs}",
                )
            }

            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFrames: Int,
                elapsedMs: Long,
            ) {
                Log.w(
                    PLAYER_DIAGNOSTIC_TAG,
                    "droppedFrames count=$droppedFrames elapsedMs=$elapsedMs " +
                        "positionMs=${eventTime.eventPlaybackPositionMs}",
                )
            }

            override fun onVideoFrameProcessingOffset(
                eventTime: AnalyticsListener.EventTime,
                totalProcessingOffsetUs: Long,
                frameCount: Int,
            ) {
                val averageOffsetUs = if (frameCount > 0) totalProcessingOffsetUs / frameCount else 0L
                Log.i(
                    PLAYER_DIAGNOSTIC_TAG,
                    "processingOffset frames=$frameCount averageUs=$averageOffsetUs " +
                        "positionMs=${eventTime.eventPlaybackPositionMs}",
                )
            }

            override fun onVideoDisabled(
                eventTime: AnalyticsListener.EventTime,
                decoderCounters: DecoderCounters,
            ) {
                decoderCounters.ensureUpdated()
                Log.i(
                    PLAYER_DIAGNOSTIC_TAG,
                    "videoDisabled ${decoderCounters.videoDiagnosticSummary()} " +
                        "positionMs=${eventTime.eventPlaybackPositionMs}",
                )
            }

            override fun onAudioUnderrun(
                eventTime: AnalyticsListener.EventTime,
                bufferSize: Int,
                bufferSizeMs: Long,
                elapsedSinceLastFeedMs: Long,
            ) {
                Log.w(
                    PLAYER_DIAGNOSTIC_TAG,
                    "audioUnderrun bufferSize=$bufferSize bufferSizeMs=$bufferSizeMs " +
                        "elapsedSinceLastFeedMs=$elapsedSinceLastFeedMs " +
                        "positionMs=${eventTime.eventPlaybackPositionMs}",
                )
            }
        } else null
        exoPlayer.addListener(listener)
        if (analyticsListener != null) {
            exoPlayer.addAnalyticsListener(analyticsListener)
        }
        onDispose {
            PlayerPictureInPictureManager.registerPausePlaybackCallback(null)
            PlayerPictureInPictureManager.registerTogglePlaybackCallback(null)
            exoPlayer.removeListener(listener)
            if (analyticsListener != null) {
                exoPlayer.removeAnalyticsListener(analyticsListener)
            }
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            loudnessEnhancerSessionId = C.AUDIO_SESSION_ID_UNSET
            playerViewRef?.keepScreenOn = false
            subtitleSelectionJob?.cancel()
            sidecarController.stopSidecarAddonSubtitle(clearView = true)
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val activity = context.findActivity()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> exoPlayer.playWhenReady = latestPlayWhenReady.value
                Lifecycle.Event.ON_STOP -> {
                    val isInPictureInPicture =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity?.isInPictureInPictureMode == true
                    val isFinishing = activity?.isFinishing == true
                    val hasActiveNowPlayingSession = nowPlayingController.isActive
                    if ((!isInPictureInPicture && !hasActiveNowPlayingSession) || isFinishing) {
                        exoPlayer.pause()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer, playWhenReady) {
        exoPlayer.playWhenReady = latestPlayWhenReady.value
        syncPlayerViewKeepScreenOn()
        dispatchExoPlayerSnapshot()
    }

    LaunchedEffect(exoPlayer) {
        onControllerReady(
            object : PlayerEngineController {
                override fun play() {
                    exoPlayer.playWhenReady = true
                    exoPlayer.play()
                }

                override fun pause() {
                    exoPlayer.pause()
                }

                override fun seekTo(positionMs: Long) {
                    exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
                }

                override fun seekBy(offsetMs: Long) {
                    exoPlayer.seekTo((exoPlayer.currentPosition + offsetMs).coerceAtLeast(0L))
                }

                override fun retry() {
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }

                override fun setPlaybackSpeed(speed: Float) {
                    exoPlayer.setPlaybackSpeed(speed)
                }

                override fun setVolumeBoost(multiplier: Float) {
                    applyExoVolumeBoost(multiplier)
                }

                override fun updateNowPlayingMetadata(info: PlayerNowPlayingInfo) {
                    nowPlayingController.updateMetadata(info)
                }

                override fun clearNowPlayingInfo() {
                    nowPlayingController.clear()
                }

                override fun getAudioTracks(): List<AudioTrack> =
                    exoPlayer.extractAudioTracks(context)

                override fun getSubtitleTracks(): List<SubtitleTrack> {
                    val tracks = exoPlayer.extractSubtitleTracks(context)
                    Log.d(TAG, "getSubtitleTracks: found ${tracks.size} tracks")
                    tracks.forEach { t ->
                        Log.d(TAG, "  track idx=${t.index} id=${t.id} label='${t.label}' lang=${t.language} selected=${t.isSelected}")
                    }
                    return tracks
                }

                override fun selectAudioTrack(index: Int) {
                    exoPlayer.selectTrackByIndex(C.TRACK_TYPE_AUDIO, index)
                }

                override fun selectSubtitleTrack(index: Int) {
                    Log.d(TAG, "selectSubtitleTrack: index=$index")
                    sidecarController.stopSidecarAddonSubtitle(clearView = true)
                    if (index < 0) {
                        Log.d(TAG, "selectSubtitleTrack: disabling text tracks")
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                        return
                    }
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .build()
                    exoPlayer.selectTrackByIndex(C.TRACK_TYPE_TEXT, index)
                    Log.d(TAG, "selectSubtitleTrack: after selection, textDisabled=${exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)}")
                    exoPlayer.logCurrentTracks("after selectSubtitleTrack")
                }

                override fun setSubtitleUri(url: String) {
                    Log.d(TAG, "setSubtitleUri: url=$url")
                    subtitleSelectionJob?.cancel()
                    if (sidecarController.canAttachAddonSubtitleViaSidecar(url, useLibass)) {
                        Log.d(TAG, "setSubtitleUri: using buffer-preserving sidecar for url=$url")
                        val headers = externalSubtitles.firstOrNull { it.url == url }?.headers.orEmpty()
                        if (
                            sidecarController.startSidecarAddonSubtitle(
                                url = url,
                                headers = headers,
                                useLibass = useLibass,
                            )
                        ) {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .build()
                            return
                        }
                    }
                    subtitleSelectionJob = coroutineScope.launch {
                        val currentPosition = exoPlayer.currentPosition
                        val wasPlaying = exoPlayer.isPlaying
                        val currentMediaItem = exoPlayer.currentMediaItem ?: run {
                            Log.e(TAG, "setSubtitleUri: currentMediaItem is null, aborting")
                            return@launch
                        }
                        preserveAudioSelectionForReload("setSubtitleUri")
                        val resolvedMime = withContext(Dispatchers.IO) {
                            resolveSubtitleMimeType(url)
                        }
                        selectedExternalSubtitleMimeType = resolvedMime
                        Log.d(TAG, "setSubtitleUri: currentPosition=$currentPosition, wasPlaying=$wasPlaying")
                        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(url))
                            .setMimeType(resolvedMime)
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                            .build()
                        Log.d(
                            TAG,
                            "setSubtitleUri: subtitleConfig built, uri=${subtitleConfig.uri}, mime=${subtitleConfig.mimeType}, selectionFlags=${subtitleConfig.selectionFlags}"
                        )
                        val newMediaItem = currentMediaItem.buildUpon()
                            .setSubtitleConfigurations(listOf(subtitleConfig))
                            .build()
                        Log.d(TAG, "setSubtitleUri: newMediaItem subtitleConfigs count=${newMediaItem.localConfiguration?.subtitleConfigurations?.size}")
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .setPreferredTextRoleFlags(C.ROLE_FLAG_SUBTITLE)
                            .build()
                        Log.d(TAG, "setSubtitleUri: track params set before prepare, textDisabled=${exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)}")
                        exoPlayer.setPlaybackMediaItem(newMediaItem, currentPosition)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = wasPlaying
                        Log.d(TAG, "setSubtitleUri: prepare() called, waiting for STATE_READY")
                    }
                }

                override fun clearExternalSubtitle() {
                    Log.d(TAG, "clearExternalSubtitle called")
                    subtitleSelectionJob?.cancel()
                    sidecarController.stopSidecarAddonSubtitle(clearView = true)
                    selectedExternalSubtitleMimeType = null
                    val currentPosition = exoPlayer.currentPosition
                    val wasPlaying = exoPlayer.isPlaying
                    val currentMediaItem = exoPlayer.currentMediaItem ?: return
                    if (currentMediaItem.localConfiguration?.subtitleConfigurations?.isNotEmpty() == true) {
                        preserveAudioSelectionForReload("clearExternalSubtitle")
                        val newMediaItem = currentMediaItem.buildUpon()
                            .setSubtitleConfigurations(emptyList())
                            .build()
                        exoPlayer.setPlaybackMediaItem(newMediaItem, currentPosition)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = wasPlaying
                    } else {
                        selectSubtitleTrack(-1)
                    }
                    Log.d(TAG, "clearExternalSubtitle: done, position=$currentPosition")
                }

                override fun clearExternalSubtitleAndSelect(trackIndex: Int) {
                    Log.d(TAG, "clearExternalSubtitleAndSelect: trackIndex=$trackIndex")
                    subtitleSelectionJob?.cancel()
                    sidecarController.stopSidecarAddonSubtitle(clearView = true)
                    selectedExternalSubtitleMimeType = null
                    val currentPosition = exoPlayer.currentPosition
                    val wasPlaying = exoPlayer.isPlaying
                    val currentMediaItem = exoPlayer.currentMediaItem ?: return
                    if (currentMediaItem.localConfiguration?.subtitleConfigurations?.isNotEmpty() == true) {
                        pendingSubtitleTrackIndex.clear()
                        pendingSubtitleTrackIndex.add(trackIndex)
                        preserveAudioSelectionForReload("clearExternalSubtitleAndSelect")
                        val newMediaItem = currentMediaItem.buildUpon()
                            .setSubtitleConfigurations(emptyList())
                            .build()
                        exoPlayer.setPlaybackMediaItem(newMediaItem, currentPosition)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = wasPlaying
                    } else {
                        pendingSubtitleTrackIndex.clear()
                        selectSubtitleTrack(trackIndex)
                    }
                    Log.d(TAG, "clearExternalSubtitleAndSelect: done, pending=$trackIndex position=$currentPosition")
                }

                override fun applySubtitleStyle(style: SubtitleStyleState) {
                    currentSubtitleStyle = style
                    playerViewRef?.applySubtitleStyle(style, pipSubtitleScale)
                }

                override fun applySubtitlePreferences(
                    preferredLanguage: String,
                    secondaryPreferredLanguage: String?,
                    useForcedSubtitles: Boolean,
                    autoSelectionApplied: Boolean,
                    hasActiveSubtitle: Boolean,
                    useCustomSubtitles: Boolean,
                ) {
                    exoPlayer.applySubtitleTrackPreferences(
                        preferredLanguage = preferredLanguage,
                        useForcedSubtitles = useForcedSubtitles,
                        autoSelectionApplied = autoSelectionApplied,
                        hasActiveSubtitle = hasActiveSubtitle,
                        useCustomSubtitles = useCustomSubtitles,
                    )
                }

                override fun setSubtitleDelayMs(delayMs: Int) {
                    subtitleDelayUs.set(
                        delayMs
                            .coerceIn(SUBTITLE_DELAY_MIN_MS, SUBTITLE_DELAY_MAX_MS)
                            .toLong() * 1_000L,
                    )
                }

                override fun refreshSubtitlePosition(positionMs: Long) {
                    exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
                }
            }
        )
    }

    LaunchedEffect(exoPlayer) {
        while (isActive) {
            dispatchExoPlayerSnapshot()
            delay(250L)
        }
    }

    LaunchedEffect(exoPlayer) {
        if (!playbackDiagnosticsEnabled) return@LaunchedEffect
        var previousPositionMs = -1L
        var previousRenderedFrames = 0
        var previousDroppedFrames = 0
        while (isActive) {
            delay(5_000L)
            val positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            val counters = exoPlayer.videoDecoderCounters
            counters?.ensureUpdated()
            val renderedFrames = counters?.renderedOutputBufferCount ?: 0
            val droppedFrames = counters?.droppedBufferCount ?: 0
            val positionDeltaMs = if (previousPositionMs >= 0L) positionMs - previousPositionMs else 0L
            val renderedDelta = (renderedFrames - previousRenderedFrames).coerceAtLeast(0)
            val droppedDelta = (droppedFrames - previousDroppedFrames).coerceAtLeast(0)
            Log.i(
                PLAYER_DIAGNOSTIC_TAG,
                "sample positionMs=$positionMs positionDeltaMs=$positionDeltaMs " +
                    "bufferedDurationMs=${exoPlayer.totalBufferedDuration.coerceAtLeast(0L)} " +
                    "isPlaying=${exoPlayer.isPlaying} displayHz=${playerViewRef?.display?.refreshRate ?: -1f} " +
                    "sourceFps=${exoPlayer.videoFormat?.frameRate ?: -1f} rendered=$renderedFrames " +
                    "renderedDelta=$renderedDelta dropped=$droppedFrames droppedDelta=$droppedDelta " +
                    (counters?.videoDiagnosticSummary() ?: "decoderCounters=unavailable"),
            )
            previousPositionMs = positionMs
            previousRenderedFrames = renderedFrames
            previousDroppedFrames = droppedFrames
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = useNativeController
                layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                player = exoPlayer
                keepScreenOn = exoPlayer.shouldKeepPlayerScreenOn()
                this.resizeMode = resizeMode.toExoResizeMode()
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                playerViewRef = this
                sidecarController.bindSubtitleView(subtitleView)
                syncLibassOverlay(
                    player = exoPlayer,
                    enabled = useLibass,
                    renderType = libassRenderType,
                )
                applySubtitleStyle(currentSubtitleStyle, pipSubtitleScale)
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
            playerView.useController = useNativeController
            playerView.resizeMode = resizeMode.toExoResizeMode()
            playerViewRef = playerView
            sidecarController.bindSubtitleView(playerView.subtitleView)
            syncPlayerViewKeepScreenOn()
            playerView.syncLibassOverlay(
                player = exoPlayer,
                enabled = useLibass,
                renderType = libassRenderType,
            )
            playerView.applySubtitleStyle(currentSubtitleStyle, pipSubtitleScale)
        },
    )
}

@Composable
private fun LibmpvPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String?,
    sourceHeaders: Map<String, String>,
    externalSubtitles: List<com.nuvio.app.features.streams.StreamSubtitle>,
    modifier: Modifier,
    playWhenReady: Boolean,
    resizeMode: PlayerResizeMode,
    videoOutput: AndroidLibmpvVideoOutput,
    hardwareDecodingEnabled: Boolean,
    yuv420pEnabled: Boolean,
    onControllerReady: (PlayerEngineController) -> Unit,
    onSnapshot: (PlayerPlaybackSnapshot) -> Unit,
    onError: (String?) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnSnapshot = rememberUpdatedState(onSnapshot)
    val latestOnError = rememberUpdatedState(onError)
    val latestPlayWhenReady = rememberUpdatedState(playWhenReady)
    val coroutineScope = rememberCoroutineScope()
    val sanitizedSourceHeaders = remember(sourceHeaders) {
        sanitizePlaybackHeaders(sourceHeaders)
    }
    var playerViewRef by remember { mutableStateOf<NuvioLibmpvView?>(null) }
    val nowPlayingController = remember(context, playerViewRef) {
        playerViewRef?.let { view ->
            AndroidPlayerNowPlayingController(
                context = context,
                controls = AndroidPlayerNowPlayingController.PlaybackControls(
                    play = { view.setPaused(false) },
                    pause = { view.setPaused(true) },
                    seekTo = { positionMs -> view.seekToMs(positionMs) },
                    seekBy = { offsetMs -> view.seekByMs(offsetMs) },
                ),
            )
        }
    }

    DisposableEffect(nowPlayingController) {
        onDispose { nowPlayingController?.release() }
    }

    DisposableEffect(lifecycleOwner, nowPlayingController) {
        val activity = context.findActivity()
        val observer = LifecycleEventObserver { _, event ->
            val view = playerViewRef ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> view.setPaused(!latestPlayWhenReady.value)
                Lifecycle.Event.ON_STOP -> {
                    val isInPictureInPicture =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity?.isInPictureInPictureMode == true
                    val isFinishing = activity?.isFinishing == true
                    val hasActiveNowPlayingSession = nowPlayingController?.isActive == true
                    if ((!isInPictureInPicture && !hasActiveNowPlayingSession) || isFinishing) {
                        view.setPaused(true)
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(playerViewRef, nowPlayingController) {
        val view = playerViewRef ?: return@DisposableEffect onDispose {}
        fun dispatchSnapshot(
            updateKeepScreenOn: Boolean = false,
            clearError: Boolean = false,
        ) {
            coroutineScope.launch(Dispatchers.Main.immediate) {
                val snapshot = view.snapshot()
                if (!view.canDispatchCallbacks()) return@launch
                if (clearError) latestOnError.value(null)
                latestOnSnapshot.value(snapshot)
                nowPlayingController?.syncPlayback(snapshot)
                if (updateKeepScreenOn) {
                    view.keepScreenOn = snapshot.shouldKeepPlayerScreenOn()
                }
            }
        }
        val observer = object : MPV.EventObserver {
            override fun eventProperty(property: String) = Unit
            override fun eventProperty(property: String, value: Long) {
                if (property == "cache-buffering-state") {
                    dispatchSnapshot(updateKeepScreenOn = true)
                }
            }
            override fun eventProperty(property: String, value: Boolean) {
                if (property == "eof-reached" || property == "pause" || property == "paused-for-cache" || property == "seeking") {
                    dispatchSnapshot(updateKeepScreenOn = true)
                }
            }
            override fun eventProperty(property: String, value: String) = Unit
            override fun eventProperty(property: String, value: Double) {
                if (property == "duration" || property == "time-pos" || property == "speed") {
                    dispatchSnapshot()
                }
            }
            override fun eventProperty(property: String, value: MPVNode) {
                if (property == "track-list") {
                    view.onTrackListChanged()
                    dispatchSnapshot()
                }
            }
            override fun event(eventId: Int, data: MPVNode) {
                when (eventId) {
                    MPV.mpvEvent.MPV_EVENT_START_FILE -> {
                        coroutineScope.launch(Dispatchers.Main.immediate) {
                            if (!view.canDispatchCallbacks()) return@launch
                            latestOnError.value(null)
                            val snapshot = PlayerPlaybackSnapshot()
                            latestOnSnapshot.value(snapshot)
                            nowPlayingController?.syncPlayback(snapshot)
                        }
                    }
                    MPV.mpvEvent.MPV_EVENT_FILE_LOADED,
                    MPV.mpvEvent.MPV_EVENT_PLAYBACK_RESTART -> {
                        dispatchSnapshot(clearError = true)
                    }
                    MPV.mpvEvent.MPV_EVENT_END_FILE -> {
                        dispatchSnapshot(updateKeepScreenOn = true)
                    }
                }
            }
        }
        view.mpv.addObserver(observer)
        onDispose {
            view.mpv.removeObserver(observer)
        }
    }

    DisposableEffect(playerViewRef) {
        val view = playerViewRef ?: return@DisposableEffect onDispose {}
        PlayerPictureInPictureManager.registerPausePlaybackCallback {
            view.setPaused(true)
        }
        PlayerPictureInPictureManager.registerTogglePlaybackCallback {
            coroutineScope.launch(Dispatchers.Main.immediate) {
                val snapshot = view.snapshot()
                if (!view.canDispatchCallbacks()) return@launch
                if (snapshot.isPlaying) {
                    view.setPaused(true)
                } else {
                    if (snapshot.isEnded) {
                        view.seekToMs(0L)
                    }
                    view.setPaused(false)
                }
            }
        }
        onDispose {
            PlayerPictureInPictureManager.registerPausePlaybackCallback(null)
            PlayerPictureInPictureManager.registerTogglePlaybackCallback(null)
            view.keepScreenOn = false
        }
    }

    LaunchedEffect(playerViewRef, sourceUrl, sourceAudioUrl, sanitizedSourceHeaders, externalSubtitles) {
        val view = playerViewRef ?: return@LaunchedEffect
        val snapshot = PlayerPlaybackSnapshot()
        latestOnSnapshot.value(snapshot)
        nowPlayingController?.syncPlayback(snapshot)
        view.loadSource(
            sourceUrl = sourceUrl,
            sourceAudioUrl = sourceAudioUrl,
            requestHeaders = sanitizedSourceHeaders,
            externalSubtitles = externalSubtitles,
            playWhenReady = latestPlayWhenReady.value,
        )
    }

    LaunchedEffect(playerViewRef, playWhenReady) {
        val view = playerViewRef ?: return@LaunchedEffect
        view.setPaused(!latestPlayWhenReady.value)
        val snapshot = view.snapshot()
        if (!view.canDispatchCallbacks()) return@LaunchedEffect
        view.keepScreenOn = snapshot.shouldKeepPlayerScreenOn()
        latestOnSnapshot.value(snapshot)
        nowPlayingController?.syncPlayback(snapshot)
    }

    LaunchedEffect(playerViewRef, resizeMode) {
        playerViewRef?.applyResizeMode(resizeMode)
    }

    LaunchedEffect(playerViewRef, sourceUrl, sourceAudioUrl, sanitizedSourceHeaders, externalSubtitles) {
        val view = playerViewRef ?: return@LaunchedEffect
        onControllerReady(view.controller(context, nowPlayingController))
    }

    LaunchedEffect(playerViewRef) {
        val view = playerViewRef ?: return@LaunchedEffect
        while (isActive) {
            val snapshot = view.snapshot()
            if (!view.canDispatchCallbacks()) return@LaunchedEffect
            latestOnSnapshot.value(snapshot)
            nowPlayingController?.syncPlayback(snapshot)
            view.keepScreenOn = snapshot.shouldKeepPlayerScreenOn()
            delay(250L)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            NuvioLibmpvView(
                context = viewContext,
                videoOutput = videoOutput,
                hardwareDecodingEnabled = hardwareDecodingEnabled,
                yuv420pEnabled = yuv420pEnabled,
            ).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                keepScreenOn = false
                runCatching {
                    Utils.copyAssets(viewContext)
                    initialize(viewContext.filesDir.path, viewContext.cacheDir.path)
                }.onFailure { error ->
                    Log.e(TAG, "Failed to initialize libmpv", error)
                    latestOnError.value(error.localizedMessage ?: "libmpv unavailable")
                }
                playerViewRef = this
            }
        },
        update = { view ->
            playerViewRef = view
            view.applyResizeMode(resizeMode)
        },
        onRelease = { view ->
            if (playerViewRef === view) playerViewRef = null
            view.releaseMpv()
        },
    )
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private class NuvioLibmpvView(
    context: Context,
    private val videoOutput: AndroidLibmpvVideoOutput,
    private val hardwareDecodingEnabled: Boolean,
    private val yuv420pEnabled: Boolean,
    attrs: AttributeSet? = null,
) : BaseMPVView(context, attrs) {
    private val mpvDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "NuvioLibmpv").apply { isDaemon = true }
    }.asCoroutineDispatcher()
    private val mpvScope = CoroutineScope(SupervisorJob() + mpvDispatcher)
    private val released = AtomicBoolean(false)
    private val operationGate = MpvOperationGate(released)
    private val snapshots = LastGoodSnapshotCoordinator(PlayerPlaybackSnapshot())
    private var currentSourceUrl: String? = null
    private var currentSourceAudioUrl: String? = null
    private var currentRequestHeaders: Map<String, String> = emptyMap()
    private var currentExternalSubtitles: List<com.nuvio.app.features.streams.StreamSubtitle> = emptyList()
    private var lastKnownDurationMs: Long = 0L
    private var lastKnownPositionMs: Long = 0L
    private var surfaceReady: Boolean = false
    private var lastSurfaceWidth: Int = 0
    private var lastSurfaceHeight: Int = 0
    private var pendingLoadPlayWhenReady: Boolean? = null
    private var desiredPlayWhenReady: Boolean = false
    private val surfaceGeneration = AtomicLong(0L)
    private val sourceGeneration = AtomicLong(0L)
    private val surfaceAvailable = AtomicBoolean(false)
    private var currentSubtitleStyle: SubtitleStyleState = SubtitleStyleState.DEFAULT
    @Volatile
    private var cachedAudioTracks: List<AudioTrack> = emptyList()
    @Volatile
    private var cachedSubtitleTracks: List<SubtitleTrack> = emptyList()

    private fun executeMpv(operation: String, block: () -> Unit) {
        if (!operationGate.acceptSubmission()) return
        val operationLabel = operation.toSafeMpvOperationLabel()
        mpvScope.launch {
            if (!operationGate.allowExecution()) return@launch
            runCatching(block).onFailure { error ->
                Log.w(TAG, "libmpv operation failed: $operationLabel", error)
            }
        }
    }

    fun canDispatchCallbacks(): Boolean = operationGate.allowExecution()

    fun releaseMpv() {
        if (!operationGate.beginRelease()) return

        surfaceGeneration.incrementAndGet()
        sourceGeneration.incrementAndGet()
        surfaceAvailable.set(false)
        pendingLoadPlayWhenReady = null
        currentSourceUrl = null
        currentSourceAudioUrl = null
        currentRequestHeaders = emptyMap()
        currentExternalSubtitles = emptyList()
        cachedAudioTracks = emptyList()
        cachedSubtitleTracks = emptyList()
        desiredPlayWhenReady = false
        surfaceReady = false
        lastSurfaceWidth = 0
        lastSurfaceHeight = 0
        keepScreenOn = false
        holder.removeCallback(this)

        mpvScope.launch {
            try {
                runCatching { mpv.destroy() }
                    .onFailure { error -> Log.w(TAG, "libmpv destroy failed", error) }
            } finally {
                mpvDispatcher.close()
            }
        }
    }

    override fun initOptions() {
        setVo(videoOutput.mpvValue)
        mpv.setOptionString("profile", "fast")
        mpv.setOptionString("hwdec", if (hardwareDecodingEnabled) "auto" else "no")
        if (yuv420pEnabled) {
            mpv.setOptionString("vf", "format=yuv420p")
        }
        mpv.setOptionString("msg-level", "all=warn")
        mpv.setOptionString("tls-verify", "yes")
        mpv.setOptionString("tls-ca-file", "${context.filesDir.path}/cacert.pem")
        mpv.setOptionString("demuxer-max-bytes", "${libmpvCacheBytes()}").logIfMpvError("demuxer-max-bytes")
        mpv.setOptionString("demuxer-max-back-bytes", "${libmpvCacheBytes()}").logIfMpvError("demuxer-max-back-bytes")
        mpv.setOptionString("vd-lavc-film-grain", "cpu")
        mpv.setPropertyBoolean("keep-open", true)
        mpv.setPropertyBoolean("input-default-bindings", true)
        mpv.setPropertyBoolean("audio-fallback-to-null", true)
        mpv.setOptionString("resume-playback", "no").logIfMpvError("resume-playback")
        mpv.setOptionString("save-position-on-quit", "no").logIfMpvError("save-position-on-quit")
    }

    override fun postInitOptions() = Unit

    override fun observeProperties() {
        val props = mapOf(
            "pause" to MPV.mpvFormat.MPV_FORMAT_FLAG,
            "paused-for-cache" to MPV.mpvFormat.MPV_FORMAT_FLAG,
            "core-idle" to MPV.mpvFormat.MPV_FORMAT_FLAG,
            "eof-reached" to MPV.mpvFormat.MPV_FORMAT_FLAG,
            "seeking" to MPV.mpvFormat.MPV_FORMAT_FLAG,
            "cache-buffering-state" to MPV.mpvFormat.MPV_FORMAT_INT64,
            "duration" to MPV.mpvFormat.MPV_FORMAT_DOUBLE,
            "duration/full" to MPV.mpvFormat.MPV_FORMAT_DOUBLE,
            "time-pos" to MPV.mpvFormat.MPV_FORMAT_DOUBLE,
            "playback-time" to MPV.mpvFormat.MPV_FORMAT_DOUBLE,
            "percent-pos" to MPV.mpvFormat.MPV_FORMAT_DOUBLE,
            "demuxer-cache-time" to MPV.mpvFormat.MPV_FORMAT_DOUBLE,
            "speed" to MPV.mpvFormat.MPV_FORMAT_DOUBLE,
            "video-params/w" to MPV.mpvFormat.MPV_FORMAT_INT64,
            "video-params/h" to MPV.mpvFormat.MPV_FORMAT_INT64,
            "video-out-params/w" to MPV.mpvFormat.MPV_FORMAT_INT64,
            "video-out-params/h" to MPV.mpvFormat.MPV_FORMAT_INT64,
            "track-list" to MPV.mpvFormat.MPV_FORMAT_NODE,
        )
        props.forEach { (name, format) -> mpv.observeProperty(name, format) }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        super.surfaceChanged(holder, format, width, height)
        val generation = surfaceGeneration.get()
        if (!holder.surface.isValid) {
            surfaceAvailable.set(false)
            executeMpv("surfaceChangedInvalid") {
                if (generation == surfaceGeneration.get()) surfaceReady = false
            }
            return
        }
        surfaceAvailable.set(true)
        updateSurfaceSize(width, height, generation)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)
        val generation = surfaceGeneration.incrementAndGet()
        if (!holder.surface.isValid) {
            surfaceAvailable.set(false)
            executeMpv("surfaceCreatedInvalid") {
                if (generation == surfaceGeneration.get()) surfaceReady = false
            }
            return
        }
        surfaceAvailable.set(true)
        if (width > 0 && height > 0) {
            updateSurfaceSize(width, height, generation)
        }
        // BaseMPVView attaches the native surface above. Reconcile the existing renderer
        // after the Android target changes without issuing loadfile or replacing the stream.
        post {
            executeMpv("surfaceReconcile") {
                reconcileSurfaceAfterAttach(generation)
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        val generation = surfaceGeneration.incrementAndGet()
        surfaceAvailable.set(false)
        executeMpv("surfaceDestroyed") {
            if (generation != surfaceGeneration.get()) return@executeMpv
            if (desiredPlayWhenReady) {
                // BaseMPVView owns detachment; only serialize Enhanced's playback state.
                runCatching { mpv.setPropertyBoolean("pause", true) }
            }
            surfaceReady = false
            lastSurfaceWidth = 0
            lastSurfaceHeight = 0
        }
        super.surfaceDestroyed(holder)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        updateSurfaceSize(width, height, surfaceGeneration.get())
    }

    fun loadSource(
        sourceUrl: String,
        sourceAudioUrl: String?,
        requestHeaders: Map<String, String>,
        externalSubtitles: List<com.nuvio.app.features.streams.StreamSubtitle>,
        playWhenReady: Boolean,
    ) {
        if (!operationGate.acceptSubmission()) return
        val generation = sourceGeneration.incrementAndGet()
        executeMpv("loadSource") {
            if (generation != sourceGeneration.get()) return@executeMpv
            val sameSource =
                currentSourceUrl == sourceUrl &&
                    currentSourceAudioUrl == sourceAudioUrl &&
                    currentRequestHeaders == requestHeaders &&
                    currentExternalSubtitles == externalSubtitles
            currentSourceUrl = sourceUrl
            currentSourceAudioUrl = sourceAudioUrl
            currentRequestHeaders = requestHeaders
            currentExternalSubtitles = externalSubtitles
            desiredPlayWhenReady = playWhenReady
            if (!sameSource) {
                lastKnownDurationMs = 0L
                lastKnownPositionMs = 0L
                loadCurrentSource(playWhenReady, generation)
            } else {
                applyRequestHeaders(requestHeaders)
                setPausedNow(!playWhenReady)
                if (pendingLoadPlayWhenReady != null) {
                    pendingLoadPlayWhenReady = playWhenReady
                }
            }
        }
    }

    private fun loadCurrentSource(
        playWhenReady: Boolean,
        expectedSourceGeneration: Long = sourceGeneration.get(),
    ) {
        if (!operationGate.allowExecution() || expectedSourceGeneration != sourceGeneration.get()) return
        val sourceUrl = currentSourceUrl ?: return
        if (!surfaceAvailable.get() || !surfaceReady || !holder.surface.isValid) {
            surfaceReady = false
            pendingLoadPlayWhenReady = playWhenReady
            applyRequestHeaders(currentRequestHeaders)
            setPausedNow(!playWhenReady)
            return
        }
        val libmpvSourceUrl = sourceUrl.toLibmpvLoadPath()
        val surface = surfaceGeneration.get()
        if (expectedSourceGeneration != sourceGeneration.get()) return
        runMpvLoadHeaderTransaction(
            sourceHeaders = currentRequestHeaders,
            subtitleHeaders = currentExternalSubtitles.map { sanitizePlaybackHeaders(it.headers) },
            applyHeaders = ::applyRequestHeaders,
            loadSource = loadSource@{
                if (
                    expectedSourceGeneration != sourceGeneration.get() ||
                    surface != surfaceGeneration.get() ||
                    !surfaceAvailable.get() ||
                    !holder.surface.isValid
                ) return@loadSource false
                syncMpvSurfaceSize()
                setPausedNow(!playWhenReady)
                mpv.setOptionString("start", "0").logIfMpvError("start")
                mpv.command("loadfile", libmpvSourceUrl, "replace")
                pendingLoadPlayWhenReady = null
                currentSourceAudioUrl?.takeIf { it.isNotBlank() }?.let { sourceAudioUrl ->
                    mpv.command("audio-add", sourceAudioUrl.toLibmpvLoadPath(), "auto")
                }
                true
            },
            addSubtitle = addSubtitle@{ index ->
                if (
                    expectedSourceGeneration != sourceGeneration.get() ||
                    surface != surfaceGeneration.get() ||
                    !surfaceAvailable.get() ||
                    !holder.surface.isValid
                ) return@addSubtitle false
                val flag = if (index == 0) "auto" else "cached"
                mpv.command(
                    "sub-add",
                    currentExternalSubtitles[index].url.toLibmpvLoadPath(),
                    flag,
                )
                true
            },
        )
        if (expectedSourceGeneration == sourceGeneration.get()) {
            setPausedNow(!playWhenReady)
        }
    }

    private fun updateSurfaceSize(width: Int, height: Int, generation: Long) {
        executeMpv("surfaceSize") {
            if (generation != surfaceGeneration.get()) return@executeMpv
            if (width <= 0 || height <= 0 || !surfaceAvailable.get() || !holder.surface.isValid) {
                surfaceReady = false
                return@executeMpv
            }
            surfaceReady = true
            val changed = width != lastSurfaceWidth || height != lastSurfaceHeight
            lastSurfaceWidth = width
            lastSurfaceHeight = height
            syncMpvSurfaceSize()
            if (changed) {
                postInvalidate()
            }
            pendingLoadPlayWhenReady?.let { loadCurrentSource(it) }
        }
    }

    private fun reconcileSurfaceAfterAttach(generation: Long) {
        if (
            !operationGate.allowExecution() ||
            generation != surfaceGeneration.get() ||
            !surfaceAvailable.get() ||
            !surfaceReady ||
            !holder.surface.isValid
        ) return
        syncMpvSurfaceSize()
        runCatching { mpv.setPropertyBoolean("pause", !desiredPlayWhenReady) }
        postInvalidate()
    }

    private fun syncMpvSurfaceSize() {
        if (!surfaceAvailable.get() || !surfaceReady || !holder.surface.isValid) return
        val width = lastSurfaceWidth.takeIf { it > 0 } ?: width.takeIf { it > 0 } ?: return
        val height = lastSurfaceHeight.takeIf { it > 0 } ?: height.takeIf { it > 0 } ?: return
        mpv.setPropertyString("android-surface-size", "${width}x$height")
    }

    private fun String.toLibmpvLoadPath(): String {
        val value = trim()
        if (value.isBlank()) return value
        return value.toExistingLocalFileOrNull()?.absolutePath ?: value
    }

    private fun String.toExistingLocalFileOrNull(): File? =
        runCatching {
            when {
                startsWith("file:", ignoreCase = true) -> File(URI(this))
                startsWith("/") -> File(this)
                else -> null
            }
        }.getOrNull()?.takeIf { it.exists() && it.isFile }

    fun setPaused(paused: Boolean) {
        executeMpv("setPaused") {
            setPausedNow(paused)
        }
    }

    private fun setPausedNow(paused: Boolean) {
        desiredPlayWhenReady = !paused
        runCatching { mpv.setPropertyBoolean("pause", paused) }
    }

    fun seekToMs(positionMs: Long) {
        executeMpv("seekExact") {
            seekToMsNow(positionMs)
        }
    }

    private fun seekToMsNow(positionMs: Long) {
        val targetMs = positionMs.coerceAtLeast(0L)
            .let { ms -> if (lastKnownDurationMs > 0L) ms.coerceAtMost(lastKnownDurationMs) else ms }
        lastKnownPositionMs = targetMs
        val seconds = targetMs / 1000.0
        mpv.command("seek", String.format(Locale.US, "%.3f", seconds), "absolute+exact")
    }

    suspend fun snapshot(): PlayerPlaybackSnapshot {
        if (!operationGate.acceptSubmission()) return snapshots.lastGood()
        return runCatching {
            withContext(mpvDispatcher) {
                if (!operationGate.allowExecution()) {
                    snapshots.lastGood()
                } else {
                    snapshots.read(::readSnapshotNow)
                }
            }
        }.getOrElse { snapshots.lastGood() }
    }

    private fun readSnapshotNow(): PlayerPlaybackSnapshot {
        val paused = mpv.getPropertyBoolean("pause") ?: true
        val pausedForCache = mpv.getPropertyBoolean("paused-for-cache") ?: false
        val idle = mpv.getPropertyBoolean("core-idle") ?: false
        val ended = mpv.getPropertyBoolean("eof-reached") ?: false
        val seeking = mpv.getPropertyBoolean("seeking") ?: false
        val cacheBufferingState = mpv.getPropertyInt("cache-buffering-state")
        val rawDurationMs = maxOf(
            mpv.getPropertyDouble("duration/full").toMillis(),
            mpv.getPropertyDouble("duration").toMillis(),
        )
        val percentPositionMs = mpv.getPropertyDouble("percent-pos")
            ?.takeIf { it.isFinite() && it > 0.0 && rawDurationMs > 0L }
            ?.let { percent -> (rawDurationMs * (percent / 100.0)).toLong() }
            ?: 0L
        val rawPositionMs = maxOf(
            mpv.getPropertyDouble("time-pos").toMillis(),
            mpv.getPropertyDouble("playback-time").toMillis(),
            percentPositionMs,
        )
        val durationMs = stableDurationMs(rawDurationMs, rawPositionMs)
        val positionMs = stablePositionMs(rawPositionMs, durationMs)
        val cachePositionMs = mpv.getPropertyDouble("demuxer-cache-time").toMillis()
        val isCacheBuffering = cacheBufferingState != null && cacheBufferingState in 0 until 100
        val isLoading = pausedForCache ||
            (!paused && !ended && (seeking || isCacheBuffering || (idle && durationMs <= 0L)))
        val videoWidth = mpv.getPropertyInt("video-out-params/dw")
            ?: mpv.getPropertyInt("video-params/dw")
            ?: mpvVideoDimension("w")
        val videoHeight = mpv.getPropertyInt("video-out-params/dh")
            ?: mpv.getPropertyInt("video-params/dh")
            ?: mpvVideoDimension("h")
        return PlayerPlaybackSnapshot(
            isLoading = isLoading,
            isPlaying = !paused && !isLoading && !idle && !ended,
            isEnded = ended,
            durationMs = durationMs,
            positionMs = positionMs,
            bufferedPositionMs = maxOf(positionMs, cachePositionMs),
            playbackSpeed = (mpv.getPropertyDouble("speed") ?: 1.0).toFloat(),
            videoWidth = videoWidth,
            videoHeight = videoHeight,
        )
    }

    private fun stableDurationMs(candidateDurationMs: Long, candidatePositionMs: Long): Long {
        val duration = candidateDurationMs.coerceAtLeast(0L)
        val position = candidatePositionMs.coerceAtLeast(0L)
        val stable = when {
            duration <= 0L -> lastKnownDurationMs
            lastKnownDurationMs > 0L &&
                duration < (lastKnownDurationMs * 0.75).toLong() &&
                position <= lastKnownDurationMs + 1_000L -> lastKnownDurationMs
            duration + 1_000L < position -> maxOf(lastKnownDurationMs, position)
            else -> duration
        }
        if (stable > 0L) {
            lastKnownDurationMs = stable
        }
        return stable
    }

    private fun stablePositionMs(candidatePositionMs: Long, durationMs: Long): Long {
        val position = candidatePositionMs.coerceAtLeast(0L)
        val stable = when {
            durationMs > 0L -> position.coerceIn(0L, durationMs)
            position > 0L -> position
            else -> lastKnownPositionMs
        }
        if (stable > 0L || durationMs > 0L) {
            lastKnownPositionMs = stable
        }
        return stable
    }

    private fun mpvVideoDimension(axis: String): Int? =
        (mpv.getPropertyInt("video-out-params/$axis") ?: mpv.getPropertyInt("video-params/$axis"))
            ?.takeIf { it > 0 }

    fun applyResizeMode(resizeMode: PlayerResizeMode) {
        executeMpv("resizeMode") {
            when (resizeMode) {
                PlayerResizeMode.Fit -> {
                    mpv.setPropertyDouble("panscan", 0.0)
                    mpv.setPropertyString("video-aspect-override", "no")
                }
                PlayerResizeMode.Fill -> {
                    mpv.setPropertyDouble("panscan", 1.0)
                    mpv.setPropertyString("video-aspect-override", "no")
                }
                PlayerResizeMode.Zoom -> {
                    mpv.setPropertyDouble("panscan", 0.5)
                    mpv.setPropertyString("video-aspect-override", "no")
                }
            }
        }
    }

    fun seekByMs(offsetMs: Long) {
        executeMpv("seekRelative") {
            val snapshot = snapshots.read(::readSnapshotNow)
            seekToMsNow(snapshot.positionMs + offsetMs)
        }
    }

    fun controller(
        context: Context,
        nowPlayingController: AndroidPlayerNowPlayingController?,
    ): PlayerEngineController =
        object : PlayerEngineController {
            override fun play() = setPaused(false)

            override fun pause() = setPaused(true)

            override fun seekTo(positionMs: Long) = this@NuvioLibmpvView.seekToMs(positionMs)

            override fun seekBy(offsetMs: Long) = this@NuvioLibmpvView.seekByMs(offsetMs)

            override fun retry() {
                executeMpv("retry") {
                    loadCurrentSource(playWhenReady = true)
                }
            }

            override fun setPlaybackSpeed(speed: Float) {
                executeMpv("playbackSpeed") {
                    mpv.setPropertyDouble("speed", speed.coerceIn(0.25f, 4f).toDouble())
                }
            }

            override fun updateNowPlayingMetadata(info: PlayerNowPlayingInfo) {
                nowPlayingController?.updateMetadata(info)
            }

            override fun clearNowPlayingInfo() {
                nowPlayingController?.clear()
            }

            override fun setMuted(muted: Boolean) {
                executeMpv("mute") {
                    mpv.setPropertyBoolean("mute", muted)
                }
            }

            override fun setVolumeBoost(multiplier: Float) {
                executeMpv("volumeBoost") {
                    val boost = multiplier.coerceIn(1f, 2f)
                    mpv.setPropertyDouble("volume", 100.0)
                    if (boost <= 1.001f) {
                        mpv.setPropertyString("af", "")
                    } else {
                        val gain = (boost * 100f).toInt().toFloat() / 100f
                        mpv.setPropertyString("af", "lavfi=[volume=$gain]")
                    }
                }
            }

            override fun getAudioTracks(): List<AudioTrack> = cachedAudioTracks

            override fun getSubtitleTracks(): List<SubtitleTrack> = cachedSubtitleTracks

            override fun selectAudioTrack(index: Int) {
                executeMpv("selectAudioTrack") {
                    if (index < 0) {
                        mpv.setPropertyString("aid", "no")
                    } else {
                        extractLibmpvTracks(context, type = "audio").getOrNull(index)?.let { track ->
                            mpv.setPropertyInt("aid", track.id)
                        }
                    }
                }
            }

            override fun selectSubtitleTrack(index: Int) {
                executeMpv("selectSubtitleTrack") {
                    if (index < 0) {
                        mpv.setPropertyString("sid", "no")
                    } else {
                        extractLibmpvTracks(context, type = "sub").getOrNull(index)?.let { track ->
                            mpv.setPropertyInt("sid", track.id)
                        }
                    }
                }
            }

            override fun setSubtitleUri(url: String) {
                executeMpv("addExternalSubtitle") {
                    mpv.command("sub-add", url.toLibmpvLoadPath(), "select")
                }
            }

            override fun clearExternalSubtitle() {
                executeMpv("clearExternalSubtitle") {
                    mpv.setPropertyString("sid", "no")
                }
            }

            override fun clearExternalSubtitleAndSelect(trackIndex: Int) {
                selectSubtitleTrack(trackIndex)
            }

            override fun applySubtitleStyle(style: SubtitleStyleState) {
                executeMpv("subtitleStyle") {
                    currentSubtitleStyle = style
                    mpv.setPropertyString("sub-color", style.textColor.toMpvColor())
                    mpv.setPropertyString("sub-back-color", style.backgroundColor.toMpvColor())
                    mpv.setPropertyString("sub-outline-color", style.outlineColor.toMpvColor())
                    mpv.setPropertyString("sub-border-color", style.outlineColor.toMpvColor())
                    mpv.setPropertyString("sub-border-style", style.toMpvSubtitleBorderStyle())
                    mpv.setPropertyInt("sub-shadow-offset", 0)
                    mpv.setPropertyString("sub-bold", if (style.bold) "yes" else "no")
                    style.customFontDirectory()?.let { mpv.setPropertyString("sub-fonts-dir", it) }
                    style.toMpvSubtitleFont()?.let { mpv.setPropertyString("sub-font", it) }
                    mpv.setPropertyInt("sub-font-size", style.toMpvSubtitleFontSize())
                    mpv.setPropertyInt("sub-outline-size", style.toMpvSubtitleOutlineSize())
                    mpv.setPropertyInt("sub-border-size", style.toMpvSubtitleOutlineSize())
                    syncSubtitleAssOverrideNow()
                }
            }

            override fun setSubtitleDelayMs(delayMs: Int) {
                executeMpv("subtitleDelay") {
                    mpv.setPropertyDouble(
                        "sub-delay",
                        delayMs.coerceIn(SUBTITLE_DELAY_MIN_MS, SUBTITLE_DELAY_MAX_MS) / 1000.0,
                    )
                }
            }

            override fun refreshSubtitlePosition(positionMs: Long) {
                seekToMs(positionMs)
            }
        }

    private fun applyRequestHeaders(headers: Map<String, String>) {
        val userAgent = headers.entries.firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }?.value
        if (!userAgent.isNullOrBlank()) {
            mpv.setPropertyString("user-agent", userAgent)
        }
        val serialized = headers
            .filterKeys { !it.equals("User-Agent", ignoreCase = true) }
            .map { (key, value) -> "${key}: ${value.replace(",", "\\,")}" }
            .joinToString(",")
        mpv.setPropertyString("http-header-fields", serialized)
    }

    fun onTrackListChanged() {
        executeMpv("trackListChanged") {
            refreshTrackCacheNow()
            syncSubtitleAssOverrideNow()
        }
    }

    private fun refreshTrackCacheNow() {
        cachedAudioTracks = extractLibmpvTracks(context, type = "audio").mapIndexed { index, track ->
            AudioTrack(
                index = index,
                id = track.id.toString(),
                label = track.label,
                language = track.language,
                isSelected = track.isSelected,
            )
        }
        cachedSubtitleTracks = extractLibmpvTracks(context, type = "sub").mapIndexed { index, track ->
            SubtitleTrack(
                index = index,
                id = track.id.toString(),
                label = track.label,
                language = track.language,
                isSelected = track.isSelected,
                isForced = track.isForced,
            )
        }
    }

    private fun syncSubtitleAssOverrideNow() {
        val forceNuvioStyle = selectedSubtitleIsAss()
        mpv.setPropertyString("sub-ass-override", if (forceNuvioStyle) "strip" else "no")
        mpv.setPropertyInt(
            "sub-pos",
            if (forceNuvioStyle) {
                100
            } else {
                (100 - currentSubtitleStyle.bottomOffset / 10).coerceIn(0, 100)
            },
        )
    }

    private fun selectedSubtitleIsAss(): Boolean =
        mpv.getPropertyNode("track-list")
            ?.asArray()
            ?.any { node ->
                node.nodeString("type") == "sub" &&
                    node.nodeBoolean("selected") == true &&
                    isAssSubtitleCodec(node.nodeString("codec"))
            } == true

    private fun extractLibmpvTracks(context: Context, type: String): List<LibmpvTrack> {
        val nodes = mpv.getPropertyNode("track-list")?.asArray()?.toList().orEmpty()
        return nodes
            .filter { node -> node.nodeString("type") == type }
            .mapIndexedNotNull { index, node ->
                val id = node.nodeInt("id") ?: return@mapIndexedNotNull null
                val rawLabel = node.nodeString("title")
                    ?: node.nodeString("external-filename")?.substringAfterLast('/')
                val codec = node.nodeString("codec")
                val language = node.nodeString("lang") ?: normalizeLanguageCode(rawLabel)
                val label = if (type == "audio") {
                    val formatBuilder = Format.Builder()
                        .setLanguage(language)
                        .setLabel(rawLabel)
                        .setSampleMimeType(codec.toAudioMimeType())
                    node.nodeInt("demux-channel-count")
                        ?.takeIf { it > 0 }
                        ?.let(formatBuilder::setChannelCount)
                    node.nodeInt("demux-samplerate")
                        ?.takeIf { it > 0 }
                        ?.let(formatBuilder::setSampleRate)
                    node.nodeInt("demux-bitrate")
                        ?.takeIf { it > 0 }
                        ?.let(formatBuilder::setAverageBitrate)
                    CustomDefaultTrackNameProvider(context.resources)
                        .getTrackName(formatBuilder.build())
                        .takeIf { it.isNotBlank() }
                } else {
                    val formatBuilder = Format.Builder()
                        .setLanguage(language)
                        .setSampleMimeType(codec.toSubtitleMimeType())
                    rawLabel
                        ?.takeIf { it.isNotBlank() && !it.equals(codec, ignoreCase = true) }
                        ?.let(formatBuilder::setLabel)
                    CustomDefaultTrackNameProvider(context.resources)
                        .getTrackName(formatBuilder.build())
                        .takeIf { it.isNotBlank() }
                        ?: rawLabel?.takeIf { it.isNotBlank() }
                        ?: codec
                } ?: runBlocking { getString(Res.string.compose_player_track_number, index + 1) }
                LibmpvTrack(
                    id = id,
                    label = label,
                    language = language,
                    isSelected = node.nodeBoolean("selected") ?: false,
                    isForced = inferForcedSubtitleTrack(
                        label = label,
                        language = language,
                        trackId = id.toString(),
                        hasForcedSelectionFlag = node.nodeBoolean("forced") ?: false,
                    ),
                )
            }
    }
}

internal class MpvOperationGate(
    private val released: AtomicBoolean = AtomicBoolean(false),
) {
    fun acceptSubmission(): Boolean = !released.get()

    fun allowExecution(): Boolean = !released.get()

    fun beginRelease(): Boolean = released.compareAndSet(false, true)
}

internal class LastGoodSnapshotCoordinator<T>(initial: T) {
    @Volatile
    private var snapshot: T = initial

    fun lastGood(): T = snapshot

    fun read(readNow: () -> T): T = runCatching(readNow)
        .onSuccess { snapshot = it }
        .getOrElse { snapshot }
}

internal fun runMpvLoadHeaderTransaction(
    sourceHeaders: Map<String, String>,
    subtitleHeaders: List<Map<String, String>>,
    applyHeaders: (Map<String, String>) -> Unit,
    loadSource: () -> Boolean,
    addSubtitle: (Int) -> Boolean,
) {
    applyHeaders(sourceHeaders)
    if (!loadSource()) return

    subtitleHeaders.forEachIndexed { index, headers ->
        if (headers.isEmpty()) {
            if (!addSubtitle(index)) return
        } else {
            applyHeaders(sourceHeaders + headers)
            try {
                if (!addSubtitle(index)) return
            } finally {
                applyHeaders(sourceHeaders)
            }
        }
    }
}

private fun PlayerPlaybackSnapshot.shouldKeepPlayerScreenOn(): Boolean =
    isPlaying || isLoading

private fun String.toSafeMpvOperationLabel(): String =
    trim()
        .takeIf {
            it.matches(Regex("[A-Za-z][A-Za-z0-9_.-]{0,63}")) &&
                !it.contains("token", ignoreCase = true) &&
                !it.contains("header", ignoreCase = true) &&
                !it.contains("url", ignoreCase = true)
        }
        ?: "unknown"

private data class LibmpvTrack(
    val id: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean,
    val isForced: Boolean,
)

internal fun isAssSubtitleCodec(codec: String?): Boolean = when (codec?.trim()?.lowercase()) {
    "ass", "ssa" -> true
    else -> false
}

private fun libmpvCacheBytes(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) 64 * 1024 * 1024 else 32 * 1024 * 1024

private fun Int.logIfMpvError(option: String) {
    if (this < 0) Log.w(TAG, "libmpv option failed: $option status=$this")
}

private fun Double?.toMillis(): Long =
    this?.takeIf { it.isFinite() && it > 0.0 }?.let { (it * 1000.0).toLong() } ?: 0L

private fun MPVNode.nodeString(key: String): String? =
    runCatching { this[key]?.asString() }.getOrNull()?.takeIf { it.isNotBlank() }

private fun MPVNode.nodeInt(key: String): Int? =
    runCatching { this[key]?.asInt()?.toInt() }.getOrNull()

private fun MPVNode.nodeBoolean(key: String): Boolean? =
    runCatching { this[key]?.asBoolean() }.getOrNull()

private fun String?.toAudioMimeType(): String? = when (this?.trim()?.lowercase()) {
    "aac", "aac_latm" -> MimeTypes.AUDIO_AAC
    "ac3", "ac-3" -> MimeTypes.AUDIO_AC3
    "eac3", "e-ac-3" -> MimeTypes.AUDIO_E_AC3
    "truehd", "mlp" -> MimeTypes.AUDIO_TRUEHD
    "dts", "dca" -> MimeTypes.AUDIO_DTS
    "dtshd", "dts-hd" -> MimeTypes.AUDIO_DTS_HD
    "flac" -> MimeTypes.AUDIO_FLAC
    "alac" -> MimeTypes.AUDIO_ALAC
    "opus" -> MimeTypes.AUDIO_OPUS
    "vorbis" -> MimeTypes.AUDIO_VORBIS
    "mp3", "mp3float" -> MimeTypes.AUDIO_MPEG
    "mp2" -> MimeTypes.AUDIO_MPEG_L2
    else -> null
}

private fun String?.toSubtitleMimeType(): String? = when (this?.trim()?.lowercase()) {
    "subrip", "srt" -> MimeTypes.APPLICATION_SUBRIP
    "ass", "ssa" -> MimeTypes.TEXT_SSA
    "webvtt", "vtt" -> MimeTypes.TEXT_VTT
    "hdmv_pgs_subtitle", "pgs" -> "application/pgs"
    else -> null
}

private fun androidx.compose.ui.graphics.Color.toMpvColor(): String {
    val argb = toArgb()
    val alpha = (argb ushr 24) and 0xff
    val red = (argb shr 16) and 0xff
    val green = (argb shr 8) and 0xff
    val blue = argb and 0xff
    return "#%02X%02X%02X%02X".format(alpha, red, green, blue)
}

private fun androidx.compose.ui.graphics.Color.alphaByte(): Int =
    (toArgb() ushr 24) and 0xff

private fun SubtitleStyleState.toMpvSubtitleFontSize(): Int =
    (fontSizeSp * MPV_SUBTITLE_FONT_SIZE_SCALE).toInt().coerceIn(
        MPV_SUBTITLE_FONT_SIZE_MIN,
        MPV_SUBTITLE_FONT_SIZE_MAX,
    )

private fun SubtitleStyleState.toMpvSubtitleOutlineSize(): Int =
    if (!outlineEnabled) 0 else outlineWidth.coerceIn(1, 5)

private fun SubtitleStyleState.toMpvSubtitleBorderStyle(): String =
    if (outlineEnabled) {
        "outline-and-shadow"
    } else if (backgroundColor.alphaByte() > 0) {
        "opaque-box"
    } else {
        "outline-and-shadow"
    }

private const val MPV_SUBTITLE_FONT_SIZE_SCALE = 55.0 / 18.0
private const val MPV_SUBTITLE_FONT_SIZE_MIN = 36
private const val MPV_SUBTITLE_FONT_SIZE_MAX = 122

private fun buildAndroidLoadControl(memorySafeBufferEnabled: Boolean): DefaultLoadControl =
    DefaultLoadControl.Builder().apply {
        if (memorySafeBufferEnabled) {
            setTargetBufferBytes(48 * 1024 * 1024)
            setBufferDurationsMs(
                10_000,
                40_000,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                4_000,
            )
            setBackBuffer(0, false)
            setPrioritizeTimeOverSizeThresholds(false)
        } else {
            setTargetBufferBytes(100 * 1024 * 1024)
            setBufferDurationsMs(
                15_000,
                70_000,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                5_000,
            )
        }
    }.build()

private fun ExoPlayer.snapshot(): PlayerPlaybackSnapshot {
    val (videoWidth, videoHeight) = videoDimensions()
    return PlayerPlaybackSnapshot(
        isLoading = playbackState == Player.STATE_IDLE || playbackState == Player.STATE_BUFFERING,
        isPlaying = isPlaying,
        isEnded = playbackState == Player.STATE_ENDED,
        durationMs = duration.coerceAtLeast(0L),
        positionMs = currentPosition.coerceAtLeast(0L),
        bufferedPositionMs = bufferedPosition.coerceAtLeast(0L),
        playbackSpeed = playbackParameters.speed,
        videoWidth = videoWidth,
        videoHeight = videoHeight,
    )
}

private fun ExoPlayer.videoDimensions(): Pair<Int, Int> {
    val format = videoFormat ?: return videoSize.width to videoSize.height
    val hasCrop = format.decodedWidth != Format.NO_VALUE &&
        format.decodedHeight != Format.NO_VALUE &&
        (format.decodedWidth > format.width || format.decodedHeight > format.height)
    val baseWidth = if (hasCrop) format.width else (format.width.takeIf { it > 0 } ?: videoSize.width)
    val baseHeight = if (hasCrop) format.height else (format.height.takeIf { it > 0 } ?: videoSize.height)
    val ratio = format.pixelWidthHeightRatio
    return if (ratio != 1f) (baseWidth * ratio).roundToInt() to baseHeight else baseWidth to baseHeight
}

private fun ExoPlayer.shouldKeepPlayerScreenOn(): Boolean =
    playerError == null &&
        playWhenReady &&
        playbackState in setOf(Player.STATE_BUFFERING, Player.STATE_READY)

private data class TrackSelectionSnapshot(
    val trackType: Int,
    val index: Int,
    val id: String?,
    val language: String?,
    val label: String?,
    val sampleMimeType: String?,
    val codecs: String?,
    val channelCount: Int,
    val roleFlags: Int,
)

private fun ExoPlayer.captureSelectedTrack(trackType: Int): TrackSelectionSnapshot? {
    var idx = 0
    for (group in currentTracks.groups) {
        if (group.type != trackType) continue
        for (trackIndex in 0 until group.length) {
            if (group.isTrackSelected(trackIndex)) {
                val format = group.mediaTrackGroup.getFormat(trackIndex)
                return TrackSelectionSnapshot(
                    trackType = trackType,
                    index = idx,
                    id = format.id,
                    language = format.language,
                    label = format.label,
                    sampleMimeType = format.sampleMimeType,
                    codecs = format.codecs,
                    channelCount = format.channelCount,
                    roleFlags = format.roleFlags,
                )
            }
            idx++
        }
    }
    return null
}

private fun ExoPlayer.restoreTrackSelection(selection: TrackSelectionSnapshot): Boolean {
    selection.id?.takeIf { it.isNotBlank() }?.let { id ->
        val restored = selectTrackByPredicate(selection.trackType, "id=$id") { _, format ->
            format.id == id
        }
        if (restored) {
            return true
        }
    }

    selection.label?.takeIf { it.isNotBlank() }?.let { label ->
        val restored = selectTrackByPredicate(selection.trackType, "label=$label") { _, format ->
            format.label.equals(label, ignoreCase = true) &&
                (selection.language.isNullOrBlank() ||
                    format.language.equals(selection.language, ignoreCase = true))
        }
        if (restored) {
            return true
        }
    }

    val technicalMatchIndexes = mutableListOf<Int>()
    var idx = 0
    for (group in currentTracks.groups) {
        if (group.type != selection.trackType) continue
        for (trackIndex in 0 until group.length) {
            val format = group.mediaTrackGroup.getFormat(trackIndex)
            if (
                !selection.language.isNullOrBlank() &&
                format.language.equals(selection.language, ignoreCase = true) &&
                format.sampleMimeType == selection.sampleMimeType &&
                format.codecs == selection.codecs &&
                format.channelCount == selection.channelCount &&
                format.roleFlags == selection.roleFlags
            ) {
                technicalMatchIndexes.add(idx)
            }
            idx++
        }
    }
    if (technicalMatchIndexes.size == 1) {
        return selectTrackByIndex(selection.trackType, technicalMatchIndexes.first())
    }

    return selectTrackByIndex(selection.trackType, selection.index)
}

private fun PlaybackException.isDecoderFailure(): Boolean =
    errorCode in setOf(
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED,
    )

private fun PlaybackException.isAudioRouteFailure(): Boolean {
    val diagnostic = buildString {
        append(errorCodeName)
        append(' ')
        append(message.orEmpty())
        append(' ')
        append(cause?.javaClass?.name.orEmpty())
        append(' ')
        append(cause?.message.orEmpty())
    }.lowercase()
    return diagnostic.contains("audio_track") ||
        diagnostic.contains("audiotrack") ||
        diagnostic.contains("audio sink") ||
        diagnostic.contains("audio renderer") ||
        diagnostic.contains("audio output")
}

private fun PlayerResizeMode.toExoResizeMode(): Int =
    when (this) {
        PlayerResizeMode.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        PlayerResizeMode.Fill -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        PlayerResizeMode.Zoom -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

private fun PlayerView.syncLibassOverlay(
    player: ExoPlayer,
    enabled: Boolean,
    renderType: LibassRenderType,
) {
    val containerId = if (renderType == LibassRenderType.OVERLAY_OPEN_GL) {
        R.id.libass_overlay_container_gl
    } else {
        R.id.libass_overlay_container
    }
    val overlayContainer = findViewById<android.widget.FrameLayout>(containerId) ?: return
    val needsOverlay = enabled && renderType.usesOverlaySubtitleView()
    val boundPlayer = getTag(R.id.libass_overlay_bound_player) as? ExoPlayer
    val hasOverlayChild = overlayContainer.hasAssOverlayChild()

    if (!needsOverlay) {
        if (hasOverlayChild) {
            overlayContainer.removeAssOverlayChildren()
        }
        if (boundPlayer != null) {
            setTag(R.id.libass_overlay_bound_player, null)
        }
        return
    }

    val assHandler = player.getAssHandlerCompat() ?: return
    if (boundPlayer === player && hasOverlayChild) {
        return
    }

    overlayContainer.removeAssOverlayChildren()
    val assSubtitleView = AssSubtitleView(overlayContainer.context, assHandler)
    overlayContainer.addView(
        assSubtitleView,
        android.widget.FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    )
    setTag(R.id.libass_overlay_bound_player, player)
}

private fun LibassRenderType.usesOverlaySubtitleView(): Boolean =
    this == LibassRenderType.OVERLAY_CANVAS || this == LibassRenderType.OVERLAY_OPEN_GL

private fun android.widget.FrameLayout.hasAssOverlayChild(): Boolean {
    for (index in 0 until childCount) {
        if (getChildAt(index) is AssSubtitleView) {
            return true
        }
    }
    return false
}

private fun android.widget.FrameLayout.removeAssOverlayChildren() {
    for (index in childCount - 1 downTo 0) {
        if (getChildAt(index) is AssSubtitleView) {
            removeViewAt(index)
        }
    }
}

private fun PlayerView.applySubtitleStyle(style: SubtitleStyleState, pipScale: Float = 1.0f) {
    subtitleView?.apply {
        val baseBottomPaddingFraction = SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION * 2f / 3f
        val offsetFraction = (style.bottomOffset / 1000f).coerceIn(0f, 0.2f)
        val bottomPaddingFraction = (baseBottomPaddingFraction + offsetFraction).coerceIn(0f, 0.4f)

        setApplyEmbeddedStyles(false)
        setApplyEmbeddedFontSizes(false)
        setBottomPaddingFraction(bottomPaddingFraction)
        setStyle(
            CaptionStyleCompat(
                style.textColor.toArgb(),
                style.backgroundColor.toArgb(),
                android.graphics.Color.TRANSPARENT,
                if (style.outlineEnabled) CaptionStyleCompat.EDGE_TYPE_OUTLINE else CaptionStyleCompat.EDGE_TYPE_NONE,
                style.outlineColor.toArgb(),
                style.toAndroidSubtitleTypeface(),
            )
        )
        setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSizeSp.toFloat() * pipScale)
    }
}

private fun SubtitleStyleState.toAndroidSubtitleTypeface(): Typeface {
    val typefaceStyle = if (bold) Typeface.BOLD else Typeface.NORMAL
    if (fontFamily == SubtitleFontFamily.Custom && !customFontPath.isNullOrBlank()) {
        runCatching { Typeface.createFromFile(customFontPath) }
            .getOrNull()
            ?.let { return Typeface.create(it, typefaceStyle) }
    }
    return when (fontFamily) {
        SubtitleFontFamily.System -> if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        SubtitleFontFamily.SansSerif -> Typeface.create(Typeface.SANS_SERIF, typefaceStyle)
        SubtitleFontFamily.Serif -> Typeface.create(Typeface.SERIF, typefaceStyle)
        SubtitleFontFamily.Monospace -> Typeface.create(Typeface.MONOSPACE, typefaceStyle)
        SubtitleFontFamily.Rounded -> Typeface.create("sans-serif-rounded", typefaceStyle)
        SubtitleFontFamily.Custom -> if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
}

private fun SubtitleStyleState.toMpvSubtitleFont(): String? =
    when (fontFamily) {
        SubtitleFontFamily.System -> null
        SubtitleFontFamily.SansSerif -> "sans-serif"
        SubtitleFontFamily.Serif -> "serif"
        SubtitleFontFamily.Monospace -> "monospace"
        SubtitleFontFamily.Rounded -> "sans-serif-rounded"
        SubtitleFontFamily.Custom -> customFontPath
            ?.takeIf(String::isNotBlank)
            ?.let(::subtitleFontFamilyName)
            ?: customFontName?.takeIf { it.isNotBlank() }
    }

private fun SubtitleStyleState.customFontDirectory(): String? =
    customFontPath
        ?.takeIf { fontFamily == SubtitleFontFamily.Custom && it.isNotBlank() }
        ?.let { File(it).parentFile?.absolutePath }

private fun ExoPlayer.extractAudioTracks(context: Context): List<AudioTrack> {
    val tracks = mutableListOf<AudioTrack>()
    val trackNameProvider = CustomDefaultTrackNameProvider(context.resources)
    var idx = 0
    for (group in currentTracks.groups) {
        if (group.type != C.TRACK_TYPE_AUDIO) continue
        for (trackIndex in 0 until group.length) {
            val format = group.mediaTrackGroup.getFormat(trackIndex)
            val label = trackNameProvider.getTrackName(format).takeIf { it.isNotBlank() }
                ?: runBlocking { getString(Res.string.compose_player_track_number, idx + 1) }
            tracks.add(
                AudioTrack(
                    index = idx,
                    id = format.id ?: idx.toString(),
                    label = label,
                    language = format.language,
                    isSelected = group.isTrackSelected(trackIndex),
                )
            )
            idx++
        }
    }
    return tracks
}

private fun ExoPlayer.applySubtitleTrackPreferences(
    preferredLanguage: String,
    useForcedSubtitles: Boolean,
    autoSelectionApplied: Boolean,
    hasActiveSubtitle: Boolean,
    useCustomSubtitles: Boolean,
) {
    if (hasActiveSubtitle || useCustomSubtitles) return

    val builder = trackSelectionParameters.buildUpon()
    val resolvedPreferred = exoPreferredTextLanguage(preferredLanguage)
    if (resolvedPreferred == null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        builder.setPreferredTextLanguage(null)
        builder.setPreferredTextRoleFlags(0)
    } else {
        val userDisabledSubtitles = autoSelectionApplied && !hasActiveSubtitle
        val shouldSuppressExoAutoSelect = useForcedSubtitles && !autoSelectionApplied
        if (!userDisabledSubtitles && !shouldSuppressExoAutoSelect) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        }
        if (!shouldSuppressExoAutoSelect) {
            builder.setPreferredTextLanguage(resolvedPreferred)
            builder.setPreferredTextRoleFlags(C.ROLE_FLAG_SUBTITLE)
        }
    }

    val currentFlags = trackSelectionParameters.ignoredTextSelectionFlags
    builder.setIgnoredTextSelectionFlags(
        if (useForcedSubtitles) {
            currentFlags and C.SELECTION_FLAG_FORCED.inv()
        } else {
            currentFlags or C.SELECTION_FLAG_FORCED
        },
    )
    trackSelectionParameters = builder.build()
}

private fun exoPreferredTextLanguage(preferredLanguage: String): String? {
    val normalized = normalizeLanguageCode(preferredLanguage) ?: return null
    return when (normalized) {
        SubtitleLanguageOption.NONE,
        SubtitleLanguageOption.FORCED,
        -> null
        SubtitleLanguageOption.DEVICE -> DeviceLanguagePreferences.preferredLanguageCodes().firstOrNull()
        else -> normalized
    }
}

private fun ExoPlayer.extractSubtitleTracks(context: Context): List<SubtitleTrack> {
    val tracks = mutableListOf<SubtitleTrack>()
    val trackNameProvider = CustomDefaultTrackNameProvider(context.resources)
    var idx = 0
    for (group in currentTracks.groups) {
        if (group.type != C.TRACK_TYPE_TEXT) continue
        for (trackIndex in 0 until group.length) {
            val format = group.mediaTrackGroup.getFormat(trackIndex)
            val hasForcedSelectionFlag = (format.selectionFlags and C.SELECTION_FLAG_FORCED) != 0
            val label = trackNameProvider.getTrackName(format)
                .takeIf { it.isNotBlank() }
                ?: format.label?.trim()?.takeIf(String::isNotBlank)
                ?: runBlocking { getString(Res.string.compose_player_track_number, idx + 1) }
            tracks.add(
                SubtitleTrack(
                    index = idx,
                    id = format.id ?: idx.toString(),
                    label = label,
                    language = format.language,
                    isSelected = group.isTrackSelected(trackIndex),
                    isForced = inferForcedSubtitleTrack(
                        label = format.label,
                        language = format.language,
                        trackId = format.id,
                        hasForcedSelectionFlag = hasForcedSelectionFlag,
                    ),
                )
            )
            idx++
        }
    }
    return tracks
}

private fun ExoPlayer.selectTrackByIndex(trackType: Int, targetIndex: Int): Boolean {
    return selectTrackByPredicate(trackType, "index=$targetIndex") { idx, _ ->
        idx == targetIndex
    }
}

private fun ExoPlayer.selectTrackByPredicate(
    trackType: Int,
    targetDescription: String,
    predicate: (index: Int, format: Format) -> Boolean,
): Boolean {
    val typeName = if (trackType == C.TRACK_TYPE_AUDIO) "AUDIO" else "TEXT"
    Log.d(TAG, "selectTrack: type=$typeName target=$targetDescription")
    var idx = 0
    for (group in currentTracks.groups) {
        if (group.type != trackType) continue
        for (trackIndex in 0 until group.length) {
            val format = group.mediaTrackGroup.getFormat(trackIndex)
            if (!predicate(idx, format)) {
                idx++
                continue
            }
            Log.d(TAG, "selectTrack: found track at idx=$idx, format.id=${format.id}, lang=${format.language}, label=${format.label}")
            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .setOverrideForType(
                    TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIndex))
                )
                .build()
            Log.d(TAG, "selectTrack: override applied")
            return true
        }
    }
    Log.w(TAG, "selectTrack: no group found for type=$typeName target=$targetDescription (total groups scanned=$idx)")
    return false
}

private fun ExoPlayer.logCurrentTracks(context: String) {
    Log.d(TAG, "--- logCurrentTracks ($context) ---")
    Log.d(TAG, "  textDisabled=${trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)}")
    for (group in currentTracks.groups) {
        val typeName = when (group.type) {
            C.TRACK_TYPE_AUDIO -> "AUDIO"
            C.TRACK_TYPE_TEXT -> "TEXT"
            C.TRACK_TYPE_VIDEO -> "VIDEO"
            else -> "OTHER(${group.type})"
        }
        if (group.type != C.TRACK_TYPE_TEXT && group.type != C.TRACK_TYPE_AUDIO) continue
        for (trackIndex in 0 until group.length) {
            val format = group.mediaTrackGroup.getFormat(trackIndex)
            Log.d(
                TAG,
                "  track type=$typeName index=$trackIndex id=${format.id} lang=${format.language} " +
                    "label=${format.label} selected=${group.isTrackSelected(trackIndex)} " +
                    "supported=${group.isTrackSupported(trackIndex)}",
            )
        }
    }
    Log.d(TAG, "--- end logCurrentTracks ---")
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun PlayerView.videoBoundsFraction(aspectRatio: Float): RectF? {
    val subtitleView = this.subtitleView ?: return null
    val viewWidth = subtitleView.width.toFloat()
    val viewHeight = subtitleView.height.toFloat()
    if (viewWidth <= 0f || viewHeight <= 0f) return null

    if (aspectRatio > 0f) {
        val parentRatio = viewWidth / viewHeight
        return if (parentRatio > aspectRatio) {
            val fitW = viewHeight * aspectRatio
            val leftPx = (viewWidth - fitW) / 2f
            RectF(leftPx / viewWidth, 0f, (leftPx + fitW) / viewWidth, 1f)
        } else {
            val fitH = viewWidth / aspectRatio
            val topPx = (viewHeight - fitH) / 2f
            RectF(0f, topPx / viewHeight, 1f, (topPx + fitH) / viewHeight)
        }
    }

    val contentFrame = getTag(androidx.media3.ui.R.id.exo_content_frame) as? AspectRatioFrameLayout
        ?: findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame)
            ?.also { setTag(androidx.media3.ui.R.id.exo_content_frame, it) }
        ?: return null
    val frameWidth = contentFrame.width.toFloat()
    val frameHeight = contentFrame.height.toFloat()
    if (frameWidth <= 0f || frameHeight <= 0f) return null
    if (frameWidth > viewWidth || frameHeight > viewHeight) return null
    val left = contentFrame.x / viewWidth
    val top = contentFrame.y / viewHeight
    return RectF(
        left,
        top,
        left + frameWidth / viewWidth,
        top + frameHeight / viewHeight,
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
private class SubtitleOffsetRenderersFactory(
    context: Context,
    private val subtitleDelayUsProvider: () -> Long,
    private val shouldNormalizeCuePositionProvider: () -> Boolean,
    private val videoBoundsFractionProvider: () -> RectF?,
) : DefaultRenderersFactory(context) {
    override fun buildTextRenderers(
        context: Context,
        output: TextOutput,
        outputLooper: android.os.Looper,
        extensionRendererMode: Int,
        out: ArrayList<Renderer>,
    ) {
        val normalizingOutput = CueNormalizingTextOutput(
            delegate = output,
            shouldNormalizeCuePositionProvider = shouldNormalizeCuePositionProvider,
            videoBoundsFractionProvider = videoBoundsFractionProvider,
        )
        val startIndex = out.size
        super.buildTextRenderers(context, normalizingOutput, outputLooper, extensionRendererMode, out)
        for (index in startIndex until out.size) {
            out[index] = SubtitleOffsetRenderer(
                baseRenderer = out[index],
                subtitleDelayUsProvider = subtitleDelayUsProvider,
            )
        }
    }
}

private class CueNormalizingTextOutput(
    private val delegate: TextOutput,
    private val shouldNormalizeCuePositionProvider: () -> Boolean,
    private val videoBoundsFractionProvider: () -> RectF?,
) : TextOutput {
    @Deprecated("Required by the current Media3 TextOutput interface.")
    override fun onCues(cueGroup: CueGroup) {
        val processed = cueGroup.cues.map(::processCue)
        delegate.onCues(CueGroup(processed, cueGroup.presentationTimeUs))
    }

    @Deprecated("Required by the current Media3 TextOutput interface.")
    @Suppress("DEPRECATION")
    override fun onCues(cues: List<Cue>) {
        delegate.onCues(cues.map(::processCue))
    }

    private fun processCue(cue: Cue): Cue {
        var processed = fixRtlCueText(cue)
        if (shouldNormalizeCuePositionProvider()) {
            processed = normalizeCuePosition(processed)
        }
        if (processed.bitmap != null) {
            val bounds = videoBoundsFractionProvider()
            if (bounds != null && bounds.width() > 0f && bounds.height() > 0f) {
                val isIdentity = bounds.left == 0f && bounds.top == 0f
                    && bounds.width() == 1f && bounds.height() == 1f
                if (!isIdentity) {
                    processed = remapBitmapCueToVideoBounds(processed, bounds)
                }
            }
        }
        return processed
    }

    private fun remapBitmapCueToVideoBounds(cue: Cue, bounds: RectF): Cue {
        val builder = cue.buildUpon()
        if (cue.position != Cue.DIMEN_UNSET) {
            builder.setPosition(bounds.left + cue.position * bounds.width())
        }
        if (cue.size != Cue.DIMEN_UNSET) {
            builder.setSize(cue.size * bounds.width())
        }
        if (cue.lineType == Cue.LINE_TYPE_FRACTION && cue.line != Cue.DIMEN_UNSET) {
            builder.setLine(bounds.top + cue.line * bounds.height(), Cue.LINE_TYPE_FRACTION)
        }
        if (cue.bitmapHeight != Cue.DIMEN_UNSET) {
            builder.setBitmapHeight(cue.bitmapHeight * bounds.height())
        }
        return builder.build()
    }

    private fun normalizeCuePosition(cue: Cue): Cue {
        if (cue.bitmap != null || cue.verticalType != Cue.TYPE_UNSET || cue.line == Cue.DIMEN_UNSET) {
            return cue
        }
        return cue.buildUpon()
            .setLine(Cue.DIMEN_UNSET, Cue.TYPE_UNSET)
            .setLineAnchor(Cue.TYPE_UNSET)
            .build()
    }

    private fun fixRtlCueText(cue: Cue): Cue {
        val text = cue.text ?: return cue
        if (!containsRtlChars(text)) return cue
        val original = text.toString()
        val fixed = original.split('\n').joinToString("\n") { line ->
            moveLeadingRtlPunctuationToEnd(line)
        }
        if (fixed == original) return cue
        return cue.buildUpon().setText(SpannableString(fixed)).build()
    }

    private fun moveLeadingRtlPunctuationToEnd(line: String): String {
        if (line.isEmpty()) return line
        var end = 0
        while (end < line.length && line[end] in RTL_PUNCTUATION) end++
        if (end == 0) return line
        return line.substring(end) + line.substring(0, end)
    }

    private fun containsRtlChars(text: CharSequence): Boolean {
        for (char in text) {
            val directionality = Character.getDirectionality(char)
            if (
                directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
            ) {
                return true
            }
        }
        return false
    }

    companion object {
        private val RTL_PUNCTUATION = setOf('.', ',', '?', '!', '-', ':', ';', '…', ')', '(')
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
private class SubtitleOffsetRenderer(
    baseRenderer: Renderer,
    private val subtitleDelayUsProvider: () -> Long,
) : ForwardingRenderer(baseRenderer) {
    override fun render(positionUs: Long, elapsedRealtimeUs: Long) {
        val adjustedPositionUs = (positionUs - subtitleDelayUsProvider()).coerceAtLeast(0L)
        super.render(adjustedPositionUs, elapsedRealtimeUs)
    }
}

private fun resolveSubtitleMimeType(url: String, headers: Map<String, String>? = null): String {
    probeSubtitleHeaders(url, headers)?.let { (contentType, contentDisposition) ->
        mapSubtitleMime(contentType)?.let { return it }
        filenameFromContentDisposition(contentDisposition)?.let(::guessSubtitleMime)?.let { return it }
    }
    return guessSubtitleMime(url)
}

private fun probeSubtitleHeaders(url: String, headers: Map<String, String>? = null): Pair<String?, String?>? {
    val methods = listOf("HEAD", "GET")
    methods.forEach { method ->
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 5_000
                readTimeout = 5_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "*/*")
                headers?.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }
            }
            try {
                connection.responseCode
                connection.contentType to connection.getHeaderField("Content-Disposition")
            } finally {
                connection.disconnect()
            }
        }.getOrNull()?.let { return it }
    }
    return null
}

private fun mapSubtitleMime(contentType: String?): String? {
    val normalized = contentType
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase()
        ?: return null

    return when (normalized) {
        "application/x-subrip",
        "application/srt",
        "text/srt",
        "text/plain" -> MimeTypes.APPLICATION_SUBRIP
        "text/vtt",
        "application/vtt" -> MimeTypes.TEXT_VTT
        "text/x-ssa",
        "text/ssa",
        "text/ass",
        "application/x-ssa" -> MimeTypes.TEXT_SSA
        "application/ttml+xml",
        "text/xml",
        "application/xml" -> MimeTypes.APPLICATION_TTML
        else -> null
    }
}

private fun filenameFromContentDisposition(contentDisposition: String?): String? =
    contentDisposition
        ?.substringAfter("filename=", missingDelimiterValue = "")
        ?.trim()
        ?.trim('"')
        ?.takeIf { it.isNotEmpty() }

private fun guessSubtitleMime(url: String): String {
    val lower = url.lowercase()
    return when {
        lower.contains(".srt") -> MimeTypes.APPLICATION_SUBRIP
        lower.contains(".vtt") || lower.contains(".webvtt") -> MimeTypes.TEXT_VTT
        lower.contains(".ass") || lower.contains(".ssa") -> MimeTypes.TEXT_SSA
        lower.contains(".ttml") || lower.contains(".dfxp") || lower.contains(".xml") -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.TEXT_VTT
    }
}

private fun isLoopbackPlaybackSource(value: String): Boolean = runCatching {
    when (Uri.parse(value).host.orEmpty().lowercase()) {
        "127.0.0.1", "localhost", "::1" -> true
        else -> false
    }
}.getOrDefault(false)

internal class SubtitleRequestHeaderDataSourceFactory(
    private val upstreamFactory: DataSource.Factory,
    private val externalSubtitles: List<com.nuvio.app.features.streams.StreamSubtitle>,
) : DataSource.Factory {
    override fun createDataSource(): DataSource =
        SubtitleRequestHeaderDataSource(
            upstream = upstreamFactory.createDataSource(),
            externalSubtitles = externalSubtitles,
        )
}

internal class SubtitleRequestHeaderDataSource(
    private val upstream: DataSource,
    private val externalSubtitles: List<com.nuvio.app.features.streams.StreamSubtitle>,
) : DataSource {
    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val url = dataSpec.uri.toString()
        val subtitle = externalSubtitles.find { it.url == url }
        val headers = subtitle?.headers
        
        return if (headers.isNullOrEmpty()) {
            upstream.open(dataSpec)
        } else {
            val mergedHeaders = dataSpec.httpRequestHeaders.toMutableMap()
            headers.forEach { (key, value) ->
                mergedHeaders[key] = value
            }
            upstream.open(dataSpec.buildUpon().setHttpRequestHeaders(mergedHeaders).build())
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        upstream.read(buffer, offset, length)

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        upstream.close()
    }
}
