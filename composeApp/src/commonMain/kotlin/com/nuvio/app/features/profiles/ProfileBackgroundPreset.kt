package com.nuvio.app.features.profiles

import androidx.compose.ui.graphics.Color
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.theme_arctic_blue
import nuvio.composeapp.generated.resources.theme_gold
import nuvio.composeapp.generated.resources.theme_graphite
import nuvio.composeapp.generated.resources.theme_jade
import nuvio.composeapp.generated.resources.theme_rose_gold
import org.jetbrains.compose.resources.StringResource

private const val ProfileBackgroundPresetPrefix = "enhanced-mesh://"

enum class ProfileBackgroundPreset(
    val key: String,
    val labelRes: StringResource,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
) {
    GOLD(
        key = "gold",
        labelRes = Res.string.theme_gold,
        primary = Color(0xFFE8A91C),
        secondary = Color(0xFFFFD45C),
        tertiary = Color(0xFF9A6200),
    ),
    JADE(
        key = "jade",
        labelRes = Res.string.theme_jade,
        primary = Color(0xFF22D37C),
        secondary = Color(0xFF7BF08D),
        tertiary = Color(0xFF0BBF9A),
    ),
    ROSE_GOLD(
        key = "rose-gold",
        labelRes = Res.string.theme_rose_gold,
        primary = Color(0xFFEC70A9),
        secondary = Color(0xFFFFB37A),
        tertiary = Color(0xFFB75AFF),
    ),
    ARCTIC_BLUE(
        key = "arctic-blue",
        labelRes = Res.string.theme_arctic_blue,
        primary = Color(0xFF3185F5),
        secondary = Color(0xFF4DE3FF),
        tertiary = Color(0xFF4D55E8),
    ),
    GRAPHITE(
        key = "graphite",
        labelRes = Res.string.theme_graphite,
        primary = Color(0xFFAAB2BE),
        secondary = Color(0xFFF3F5F7),
        tertiary = Color(0xFF687381),
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
    }
}

fun profileBackgroundPreset(profile: NuvioProfile): ProfileBackgroundPreset? =
    ProfileBackgroundPreset.fromStoredValue(profile.backgroundUrl)
