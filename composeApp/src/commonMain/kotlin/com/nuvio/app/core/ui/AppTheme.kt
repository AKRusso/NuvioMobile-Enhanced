package com.nuvio.app.core.ui

import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.theme_amber
import nuvio.composeapp.generated.resources.theme_aurora
import nuvio.composeapp.generated.resources.theme_custom
import nuvio.composeapp.generated.resources.theme_crimson
import nuvio.composeapp.generated.resources.theme_emerald
import nuvio.composeapp.generated.resources.theme_ocean
import nuvio.composeapp.generated.resources.theme_nebula
import nuvio.composeapp.generated.resources.theme_opal
import nuvio.composeapp.generated.resources.theme_prism
import nuvio.composeapp.generated.resources.theme_rose
import nuvio.composeapp.generated.resources.theme_violet
import nuvio.composeapp.generated.resources.theme_white
import org.jetbrains.compose.resources.StringResource

enum class AppTheme {
    CRIMSON,
    OCEAN,
    VIOLET,
    EMERALD,
    AMBER,
    ROSE,
    AURORA,
    PRISM,
    NEBULA,
    OPAL,
    CUSTOM,
    WHITE,
}

val AppTheme.isEnhanced: Boolean
    get() = this == AppTheme.AURORA ||
        this == AppTheme.PRISM ||
        this == AppTheme.NEBULA ||
        this == AppTheme.OPAL ||
        this == AppTheme.CUSTOM

val AppTheme.labelRes: StringResource
    get() = when (this) {
        AppTheme.CRIMSON -> Res.string.theme_crimson
        AppTheme.OCEAN -> Res.string.theme_ocean
        AppTheme.VIOLET -> Res.string.theme_violet
        AppTheme.EMERALD -> Res.string.theme_emerald
        AppTheme.AMBER -> Res.string.theme_amber
        AppTheme.ROSE -> Res.string.theme_rose
        AppTheme.AURORA -> Res.string.theme_aurora
        AppTheme.PRISM -> Res.string.theme_prism
        AppTheme.NEBULA -> Res.string.theme_nebula
        AppTheme.OPAL -> Res.string.theme_opal
        AppTheme.CUSTOM -> Res.string.theme_custom
        AppTheme.WHITE -> Res.string.theme_white
    }
