/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.constants

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * A named snapshot of every user-adjustable visual preference — accent color,
 * grid/card sizing, pure-black option, font, and the Home background image
 * (embedded as base64 so a theme file is portable across devices, not just a
 * path that only exists on the device that made it).
 */
data class ThemePack(
    val name: String,
    val accentColor: Int,
    val dynamicTheme: Boolean,
    val pureBlackHeroBackground: Boolean,
    val gridItemSize: String,
    val gridColumnsOverride: Int,
    val gridSpacing: Int,
    val speedDialColumnsOverride: Int,
    val selectedFont: String,
    val brandFontEnabled: Boolean,
    val libraryBackgroundMode: String,
    val homeBackgroundEnabled: Boolean,
    val homeBackgroundBlur: Float,
    val homeBackgroundDim: Float,
    val homeBackgroundAnimate: Boolean,
    /** Base64 JPEG bytes, or null when no Home background image is set. */
    val homeBackgroundImageBase64: String?,
)

fun ThemePack.toJson(): String = JSONObject().apply {
    put("name", name)
    put("accentColor", accentColor)
    put("dynamicTheme", dynamicTheme)
    put("pureBlackHeroBackground", pureBlackHeroBackground)
    put("gridItemSize", gridItemSize)
    put("gridColumnsOverride", gridColumnsOverride)
    put("gridSpacing", gridSpacing)
    put("speedDialColumnsOverride", speedDialColumnsOverride)
    put("selectedFont", selectedFont)
    put("brandFontEnabled", brandFontEnabled)
    put("libraryBackgroundMode", libraryBackgroundMode)
    put("homeBackgroundEnabled", homeBackgroundEnabled)
    put("homeBackgroundBlur", homeBackgroundBlur.toDouble())
    put("homeBackgroundDim", homeBackgroundDim.toDouble())
    put("homeBackgroundAnimate", homeBackgroundAnimate)
    put("homeBackgroundImageBase64", homeBackgroundImageBase64 ?: JSONObject.NULL)
}.toString()

fun String.toThemePackOrNull(): ThemePack? = runCatching {
    val obj = JSONObject(this)
    ThemePack(
        name = obj.getString("name"),
        accentColor = obj.getInt("accentColor"),
        dynamicTheme = obj.getBoolean("dynamicTheme"),
        pureBlackHeroBackground = obj.optBoolean("pureBlackHeroBackground", false),
        gridItemSize = obj.optString("gridItemSize", "SMALL"),
        gridColumnsOverride = obj.optInt("gridColumnsOverride", 0),
        gridSpacing = obj.optInt("gridSpacing", 16),
        speedDialColumnsOverride = obj.optInt("speedDialColumnsOverride", 0),
        selectedFont = obj.optString("selectedFont", "system"),
        brandFontEnabled = obj.optBoolean("brandFontEnabled", true),
        libraryBackgroundMode = obj.optString("libraryBackgroundMode", "THUMBNAIL_BLUR"),
        homeBackgroundEnabled = obj.optBoolean("homeBackgroundEnabled", false),
        homeBackgroundBlur = obj.optDouble("homeBackgroundBlur", 20.0).toFloat(),
        homeBackgroundDim = obj.optDouble("homeBackgroundDim", 0.4).toFloat(),
        homeBackgroundAnimate = obj.optBoolean("homeBackgroundAnimate", false),
        homeBackgroundImageBase64 = obj.optString("homeBackgroundImageBase64").ifEmpty { null }
            .takeIf { obj.isNull("homeBackgroundImageBase64").not() },
    )
}.getOrNull()

fun List<ThemePack>.toJsonArray(): String {
    val arr = JSONArray()
    forEach { arr.put(JSONObject(it.toJson())) }
    return arr.toString()
}

fun String.toThemePacks(): List<ThemePack> = runCatching {
    val arr = JSONArray(this)
    (0 until arr.length()).mapNotNull { i -> arr.getJSONObject(i).toString().toThemePackOrNull() }
}.getOrElse { emptyList() }

fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
