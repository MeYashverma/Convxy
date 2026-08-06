/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convx.music.constants.BrandFontEnabledKey
import com.convx.music.constants.DynamicThemeKey
import com.convx.music.constants.GridColumnsOverrideKey
import com.convx.music.constants.GridItemsSizeKey
import com.convx.music.constants.GridSpacingKey
import com.convx.music.constants.HomeBackgroundAnimateKey
import com.convx.music.constants.HomeBackgroundBlurKey
import com.convx.music.constants.HomeBackgroundDimKey
import com.convx.music.constants.HomeBackgroundEnabledKey
import com.convx.music.constants.HomeBackgroundPathKey
import com.convx.music.constants.LibraryBackgroundModeKey
import com.convx.music.constants.PureBlackHeroBackgroundKey
import com.convx.music.constants.SavedThemePacksKey
import com.convx.music.constants.SelectedFontKey
import com.convx.music.constants.SelectedThemeColorKey
import com.convx.music.constants.SpeedDialColumnsOverrideKey
import com.convx.music.constants.ThemeCategory
import com.convx.music.constants.ThemePack
import com.convx.music.constants.fromBase64
import com.convx.music.constants.toBase64
import com.convx.music.constants.toJson
import com.convx.music.constants.toJsonArray
import com.convx.music.constants.toThemePackOrNull
import com.convx.music.constants.toThemePacks
import com.convx.music.ui.theme.DefaultThemeColor
import com.convx.music.utils.dataStore
import com.convx.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ThemePackViewModel @Inject constructor() : ViewModel() {

    private val _themePacks = MutableStateFlow<List<ThemePack>>(emptyList())
    val themePacks = _themePacks.asStateFlow()

    fun loadThemePacks(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _themePacks.value = context.dataStore.get(SavedThemePacksKey, "[]").toThemePacks()
        }
    }

    /** Snapshots every current theme-relevant preference into a named [ThemePack]. */
    private fun captureCurrentTheme(context: Context, name: String): ThemePack {
        val ds = context.dataStore
        val backgroundPath = ds.get(HomeBackgroundPathKey, "")
        val imageBase64 = backgroundPath.takeIf { it.isNotEmpty() }
            ?.let { path -> runCatching { File(path).readBytes().toBase64() }.getOrNull() }

        return ThemePack(
            name = name,
            accentColor = ds.get(SelectedThemeColorKey, DefaultThemeColor.toArgb()),
            dynamicTheme = ds.get(DynamicThemeKey, true),
            pureBlackHeroBackground = ds.get(PureBlackHeroBackgroundKey, false),
            gridItemSize = ds.get(GridItemsSizeKey, "SMALL"),
            gridColumnsOverride = ds.get(GridColumnsOverrideKey, 0),
            gridSpacing = ds.get(GridSpacingKey, 16),
            speedDialColumnsOverride = ds.get(SpeedDialColumnsOverrideKey, 0),
            selectedFont = ds.get(SelectedFontKey, "system"),
            brandFontEnabled = ds.get(BrandFontEnabledKey, true),
            libraryBackgroundMode = ds.get(LibraryBackgroundModeKey, "THUMBNAIL_BLUR"),
            homeBackgroundEnabled = ds.get(HomeBackgroundEnabledKey, false),
            homeBackgroundBlur = ds.get(HomeBackgroundBlurKey, 20f),
            homeBackgroundDim = ds.get(HomeBackgroundDimKey, 0.4f),
            homeBackgroundAnimate = ds.get(HomeBackgroundAnimateKey, false),
            homeBackgroundImageBase64 = imageBase64,
        )
    }

    /** Saves the CURRENT app settings as a new named theme pack in the local list. */
    fun saveCurrentAsThemePack(context: Context, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val pack = captureCurrentTheme(context, name)
            val updated = _themePacks.value.filterNot { it.name == name } + pack
            context.dataStore.edit { it[SavedThemePacksKey] = updated.toJsonArray() }
            _themePacks.value = updated
        }
    }

    fun deleteThemePack(context: Context, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _themePacks.value.filterNot { it.name == name }
            context.dataStore.edit { it[SavedThemePacksKey] = updated.toJsonArray() }
            _themePacks.value = updated
        }
    }

    /** Writes the selected [categories] of [pack] back into the live preferences.
     *  Defaults to every category, matching the old all-or-nothing behavior. */
    fun applyThemePack(
        context: Context,
        pack: ThemePack,
        categories: Set<ThemeCategory> = ThemeCategory.entries.toSet(),
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val applyHomeBackground = ThemeCategory.HOME_BACKGROUND in categories
            val newBackgroundPath = if (applyHomeBackground) {
                pack.homeBackgroundImageBase64?.let { base64 ->
                    runCatching {
                        val dest = File(context.filesDir, "home_background_${System.currentTimeMillis()}.jpg")
                        dest.writeBytes(base64.fromBase64())
                        dest.absolutePath
                    }.getOrNull()
                }
            } else null
            val oldBackgroundPath = context.dataStore.get(HomeBackgroundPathKey, "")

            context.dataStore.edit { settings ->
                if (ThemeCategory.COLORS in categories) {
                    settings[SelectedThemeColorKey] = pack.accentColor
                    settings[DynamicThemeKey] = pack.dynamicTheme
                }
                if (ThemeCategory.PURE_BLACK in categories) {
                    settings[PureBlackHeroBackgroundKey] = pack.pureBlackHeroBackground
                }
                if (ThemeCategory.GRID_SIZING in categories) {
                    settings[GridItemsSizeKey] = pack.gridItemSize
                    settings[GridColumnsOverrideKey] = pack.gridColumnsOverride
                    settings[GridSpacingKey] = pack.gridSpacing
                    settings[SpeedDialColumnsOverrideKey] = pack.speedDialColumnsOverride
                }
                if (ThemeCategory.FONT in categories) {
                    settings[SelectedFontKey] = pack.selectedFont
                    settings[BrandFontEnabledKey] = pack.brandFontEnabled
                }
                if (ThemeCategory.LIBRARY_BACKGROUND in categories) {
                    settings[LibraryBackgroundModeKey] = pack.libraryBackgroundMode
                }
                if (applyHomeBackground) {
                    settings[HomeBackgroundEnabledKey] = pack.homeBackgroundEnabled
                    settings[HomeBackgroundBlurKey] = pack.homeBackgroundBlur
                    settings[HomeBackgroundDimKey] = pack.homeBackgroundDim
                    settings[HomeBackgroundAnimateKey] = pack.homeBackgroundAnimate
                    if (newBackgroundPath != null) {
                        settings[HomeBackgroundPathKey] = newBackgroundPath
                    } else if (pack.homeBackgroundImageBase64 == null) {
                        settings[HomeBackgroundPathKey] = ""
                    }
                }
            }
            if (newBackgroundPath != null && oldBackgroundPath.isNotEmpty()) {
                runCatching { File(oldBackgroundPath).delete() }
            }
        }
    }

    fun exportThemePack(context: Context, pack: ThemePack, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(pack.toJson().toByteArray())
                }
            }
        }
    }

    /** Fire-and-forget wrapper for callers outside a coroutine scope (Compose click handlers). */
    fun importThemePackAsync(context: Context, uri: Uri, onResult: (ThemePack?) -> Unit) {
        viewModelScope.launch {
            onResult(importThemePack(context, uri))
        }
    }

    /** Reads and parses a theme file, saving it into the local list on success. */
    suspend fun importThemePack(context: Context, uri: Uri): ThemePack? = withContext(Dispatchers.IO) {
        val json = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        }.getOrNull() ?: return@withContext null

        val pack = json.toThemePackOrNull() ?: return@withContext null
        val updated = _themePacks.value.filterNot { it.name == pack.name } + pack
        context.dataStore.edit { it[SavedThemePacksKey] = updated.toJsonArray() }
        _themePacks.value = updated
        pack
    }
}
