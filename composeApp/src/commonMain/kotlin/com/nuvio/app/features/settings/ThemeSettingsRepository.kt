package com.nuvio.app.features.settings

import com.nuvio.app.core.ui.AppTheme
import com.nuvio.app.core.ui.NativeTabBridge
import com.nuvio.app.core.ui.ThemeColors
import com.nuvio.app.core.ui.ThemeAccentColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeSettingsRepository {
    private val _selectedTheme = MutableStateFlow(AppTheme.WHITE)
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme.asStateFlow()

    private val _customThemeFirstColor = MutableStateFlow(ThemeAccentColor.PINK)
    val customThemeFirstColor: StateFlow<ThemeAccentColor> = _customThemeFirstColor.asStateFlow()

    private val _customThemeSecondColor = MutableStateFlow(ThemeAccentColor.CYAN)
    val customThemeSecondColor: StateFlow<ThemeAccentColor> = _customThemeSecondColor.asStateFlow()

    private val _amoledEnabled = MutableStateFlow(false)
    val amoledEnabled: StateFlow<Boolean> = _amoledEnabled.asStateFlow()

    private val _liquidGlassNativeTabBarEnabled = MutableStateFlow(false)
    val liquidGlassNativeTabBarEnabled: StateFlow<Boolean> = _liquidGlassNativeTabBarEnabled.asStateFlow()

    private val _liquidGlassAutoHideOnScrollEnabled = MutableStateFlow(false)
    val liquidGlassAutoHideOnScrollEnabled: StateFlow<Boolean> = _liquidGlassAutoHideOnScrollEnabled.asStateFlow()

    private val _selectedAppLanguage = MutableStateFlow(AppLanguage.DEVICE)
    val selectedAppLanguage: StateFlow<AppLanguage> = _selectedAppLanguage.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _selectedTheme.value = AppTheme.WHITE
        _customThemeFirstColor.value = ThemeAccentColor.PINK
        _customThemeSecondColor.value = ThemeAccentColor.CYAN
        _amoledEnabled.value = false
        _liquidGlassNativeTabBarEnabled.value = false
        _liquidGlassAutoHideOnScrollEnabled.value = false
        NativeTabBridge.publishAccentColor(AppTheme.WHITE.nativeTabAccentHex())
        NativeTabBridge.publishLiquidGlassEnabled(false)
        _selectedAppLanguage.value = AppLanguage.DEVICE
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val stored = ThemeSettingsStorage.loadSelectedTheme()
        val theme = if (stored != null) {
            try {
                AppTheme.valueOf(stored)
            } catch (_: IllegalArgumentException) {
                AppTheme.WHITE
            }
        } else {
            AppTheme.WHITE
        }
        _selectedTheme.value = theme
        _customThemeFirstColor.value = ThemeSettingsStorage.loadCustomThemeFirstColor()
            .toThemeAccentColor(ThemeAccentColor.PINK)
        _customThemeSecondColor.value = ThemeSettingsStorage.loadCustomThemeSecondColor()
            .toThemeAccentColor(ThemeAccentColor.CYAN)
        NativeTabBridge.publishAccentColor(theme.nativeTabAccentHex(_customThemeFirstColor.value))
        _amoledEnabled.value = ThemeSettingsStorage.loadAmoledEnabled() ?: false
        val liquidGlassEnabled = ThemeSettingsStorage.loadLiquidGlassNativeTabBarEnabled() ?: false
        _liquidGlassNativeTabBarEnabled.value = liquidGlassEnabled
        _liquidGlassAutoHideOnScrollEnabled.value =
            ThemeSettingsStorage.loadLiquidGlassAutoHideOnScrollEnabled() ?: false
        NativeTabBridge.publishLiquidGlassEnabled(liquidGlassEnabled)
        val appLanguage = AppLanguage.fromCode(ThemeSettingsStorage.loadSelectedAppLanguage())
        ThemeSettingsStorage.applySelectedAppLanguage(appLanguage.code)
        _selectedAppLanguage.value = appLanguage
    }

    fun setTheme(theme: AppTheme) {
        ensureLoaded()
        if (_selectedTheme.value == theme) return
        _selectedTheme.value = theme
        ThemeSettingsStorage.saveSelectedTheme(theme.name)
        NativeTabBridge.publishAccentColor(theme.nativeTabAccentHex(_customThemeFirstColor.value))
    }

    fun setCustomThemeFirstColor(color: ThemeAccentColor) {
        ensureLoaded()
        if (_customThemeFirstColor.value == color) return
        _customThemeFirstColor.value = color
        ThemeSettingsStorage.saveCustomThemeFirstColor(color.name)
        if (_selectedTheme.value == AppTheme.CUSTOM) {
            NativeTabBridge.publishAccentColor(AppTheme.CUSTOM.nativeTabAccentHex(color))
        }
    }

    fun setCustomThemeSecondColor(color: ThemeAccentColor) {
        ensureLoaded()
        if (_customThemeSecondColor.value == color) return
        _customThemeSecondColor.value = color
        ThemeSettingsStorage.saveCustomThemeSecondColor(color.name)
    }

    fun setAmoled(enabled: Boolean) {
        ensureLoaded()
        if (_amoledEnabled.value == enabled) return
        _amoledEnabled.value = enabled
        ThemeSettingsStorage.saveAmoledEnabled(enabled)
    }

    fun setLiquidGlassNativeTabBar(enabled: Boolean) {
        ensureLoaded()
        if (_liquidGlassNativeTabBarEnabled.value == enabled) return
        _liquidGlassNativeTabBarEnabled.value = enabled
        ThemeSettingsStorage.saveLiquidGlassNativeTabBarEnabled(enabled)
        NativeTabBridge.publishLiquidGlassEnabled(enabled)
    }

    fun setLiquidGlassAutoHideOnScroll(enabled: Boolean) {
        ensureLoaded()
        if (_liquidGlassAutoHideOnScrollEnabled.value == enabled) return
        _liquidGlassAutoHideOnScrollEnabled.value = enabled
        ThemeSettingsStorage.saveLiquidGlassAutoHideOnScrollEnabled(enabled)
    }

    fun setAppLanguage(language: AppLanguage) {
        ensureLoaded()
        if (_selectedAppLanguage.value == language) return
        ThemeSettingsStorage.saveSelectedAppLanguage(language.code)
        ThemeSettingsStorage.applySelectedAppLanguage(language.code)
        _selectedAppLanguage.value = language
    }
}

private fun AppTheme.nativeTabAccentHex(customFirst: ThemeAccentColor = ThemeAccentColor.PINK): String =
    ThemeColors.getColorPalette(this, customFirst = customFirst).nativeAccentHex

private fun String?.toThemeAccentColor(fallback: ThemeAccentColor): ThemeAccentColor =
    this?.let { stored -> ThemeAccentColor.entries.firstOrNull { it.name == stored } } ?: fallback
