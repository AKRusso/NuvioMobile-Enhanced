package com.nuvio.app.features.profiles

import com.nuvio.app.core.ui.AppTheme
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.profile_background_arctic_blue
import nuvio.composeapp.generated.resources.profile_background_default
import nuvio.composeapp.generated.resources.profile_background_gold
import nuvio.composeapp.generated.resources.profile_background_graphite
import nuvio.composeapp.generated.resources.profile_background_jade
import nuvio.composeapp.generated.resources.profile_background_rose_gold
import nuvio.composeapp.generated.resources.theme_arctic_blue
import nuvio.composeapp.generated.resources.theme_gold
import nuvio.composeapp.generated.resources.theme_graphite
import nuvio.composeapp.generated.resources.theme_jade
import nuvio.composeapp.generated.resources.theme_rose_gold
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

private const val ProfileBackgroundPresetPrefix = "enhanced-mesh://"

enum class ProfileBackgroundPreset(
    val key: String,
    val labelRes: StringResource,
    val backgroundRes: DrawableResource,
) {
    GOLD(
        key = "gold",
        labelRes = Res.string.theme_gold,
        backgroundRes = Res.drawable.profile_background_gold,
    ),
    JADE(
        key = "jade",
        labelRes = Res.string.theme_jade,
        backgroundRes = Res.drawable.profile_background_jade,
    ),
    ROSE_GOLD(
        key = "rose-gold",
        labelRes = Res.string.theme_rose_gold,
        backgroundRes = Res.drawable.profile_background_rose_gold,
    ),
    ARCTIC_BLUE(
        key = "arctic-blue",
        labelRes = Res.string.theme_arctic_blue,
        backgroundRes = Res.drawable.profile_background_arctic_blue,
    ),
    GRAPHITE(
        key = "graphite",
        labelRes = Res.string.theme_graphite,
        backgroundRes = Res.drawable.profile_background_graphite,
    ),
    ;

    val storedValue: String
        get() = "$ProfileBackgroundPresetPrefix$key"

    companion object {
        fun fromStoredValue(value: String?): ProfileBackgroundPreset? {
            val key = value?.trim()?.takeIf { it.startsWith(ProfileBackgroundPresetPrefix) }
                ?.removePrefix(ProfileBackgroundPresetPrefix)
                ?: return null
            return entries.firstOrNull { it.key == key }
        }

        fun fromTheme(theme: AppTheme): ProfileBackgroundPreset? = when (theme) {
            AppTheme.GOLD -> GOLD
            AppTheme.JADE -> JADE
            AppTheme.ROSE_GOLD -> ROSE_GOLD
            AppTheme.ARCTIC_BLUE -> ARCTIC_BLUE
            AppTheme.GRAPHITE -> GRAPHITE
            else -> null
        }
    }
}

val DefaultProfileBackgroundResource: DrawableResource = Res.drawable.profile_background_default

data class EffectiveProfileBackground(
    val preset: ProfileBackgroundPreset?,
    val customImageUrl: String?,
)

fun profileBackgroundPreset(profile: NuvioProfile): ProfileBackgroundPreset? =
    ProfileBackgroundPreset.fromStoredValue(profile.backgroundUrl)

fun effectiveProfileBackgroundPreset(
    profile: NuvioProfile,
    theme: AppTheme,
): ProfileBackgroundPreset? = effectiveProfileBackground(profile, theme).preset

fun effectiveProfileBackground(
    profile: NuvioProfile?,
    theme: AppTheme,
): EffectiveProfileBackground {
    val customImageUrl = profile?.let(::profileBackgroundImageUrl)
    return EffectiveProfileBackground(
        preset = if (customImageUrl == null) {
            ProfileBackgroundPreset.fromTheme(theme) ?: profile?.let(::profileBackgroundPreset)
        } else {
            null
        },
        customImageUrl = customImageUrl,
    )
}
