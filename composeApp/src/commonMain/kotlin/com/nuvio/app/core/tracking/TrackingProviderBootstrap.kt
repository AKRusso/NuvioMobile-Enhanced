package com.nuvio.app.core.tracking

import com.nuvio.app.features.anime.AniListTrackingRepository
import com.nuvio.app.features.anime.MyAnimeListTrackingRepository
import com.nuvio.app.features.anime.AnimeTrackingSettingsRepository
import com.nuvio.app.features.simkl.SimklAuthRepository
import com.nuvio.app.features.simkl.SimklMutationRepository
import com.nuvio.app.features.simkl.SimklLibraryRepository
import com.nuvio.app.features.simkl.SimklProgressRepository
import com.nuvio.app.features.simkl.SimklTrackingLibraryProvider
import com.nuvio.app.features.simkl.SimklTrackingProgressProvider
import com.nuvio.app.features.simkl.SimklWatchedSyncAdapter
import com.nuvio.app.features.simkl.SimklSyncRepository
import com.nuvio.app.features.tracking.TrackingProviderRegistry
import com.nuvio.app.features.trakt.TraktAuthRepository
import com.nuvio.app.features.trakt.TraktScrobbleRepository
import com.nuvio.app.features.trakt.TraktTrackingLibraryProvider
import com.nuvio.app.features.trakt.TraktTrackingProgressProvider
import com.nuvio.app.features.watching.sync.TraktWatchedSyncAdapter

fun ensureTrackingProvidersRegistered() {
    TraktAuthRepository.descriptor
    TraktScrobbleRepository.ensureRegistered()
    SimklAuthRepository.descriptor
    SimklSyncRepository.state
    SimklLibraryRepository.uiState
    SimklProgressRepository.uiState
    SimklMutationRepository.ensureRegistered()
    AniListTrackingRepository.descriptor
    MyAnimeListTrackingRepository.descriptor
    AnimeTrackingSettingsRepository.ensureLoaded()
    TrackingProviderRegistry.registerLibraryProvider(TraktTrackingLibraryProvider)
    TrackingProviderRegistry.registerLibraryProvider(SimklTrackingLibraryProvider)
    TrackingProviderRegistry.registerWatchedProvider(TraktWatchedSyncAdapter)
    TrackingProviderRegistry.registerWatchedProvider(SimklWatchedSyncAdapter)
    TrackingProviderRegistry.registerProgressProvider(TraktTrackingProgressProvider)
    TrackingProviderRegistry.registerProgressProvider(SimklTrackingProgressProvider)
}
