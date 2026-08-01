package com.convx.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import com.convx.music.ui.component.GlassSwitchCompat as Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.convx.music.LocalPlayerAwareWindowInsets
import com.convx.music.R
import com.convx.music.constants.LiquidGlassChromaticAberrationKey
import com.convx.music.constants.LiquidGlassDepthEffectKey
import com.convx.music.constants.LiquidGlassBlurRadiusKey
import com.convx.music.constants.LiquidGlassLensAmountKey
import com.convx.music.constants.LiquidGlassLensHeightKey
import com.convx.music.constants.LiquidGlassMiniPlayerEnabledKey
import com.convx.music.constants.LiquidGlassNavBarEnabledKey
import com.convx.music.constants.LiquidGlassSidePanelEnabledKey
import com.convx.music.constants.LiquidGlassSidePanelVibrancyKey
import com.convx.music.constants.LiquidGlassSidePanelBlurRadiusKey
import com.convx.music.constants.LiquidGlassSidePanelLensHeightKey
import com.convx.music.constants.LiquidGlassSidePanelLensAmountKey
import com.convx.music.constants.LiquidGlassSidePanelColorKey
import com.convx.music.constants.LiquidGlassSidePanelSurfaceOpacityKey
import com.convx.music.constants.LiquidGlassSidePanelTextColorKey
import com.convx.music.constants.LiquidGlassSurfaceOpacityKey
import com.convx.music.constants.LiquidGlassSurfaceTintColorKey
import com.convx.music.constants.LiquidGlassTextColorKey
import com.convx.music.constants.LiquidGlassVibrancyKey
import com.convx.music.ui.component.ColorPickerDialog
import com.convx.music.ui.component.DefaultDialog
import com.convx.music.ui.component.IconButton as AppIconButton
import com.convx.music.ui.component.Material3SettingsGroup
import com.convx.music.ui.component.Material3SettingsItem
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.backToMain
import com.convx.music.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassEffectSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (vibrancy, onVibrancyChange) = rememberPreference(
        LiquidGlassVibrancyKey, defaultValue = 1.2f
    )
    val (blurRadius, onBlurRadiusChange) = rememberPreference(
        LiquidGlassBlurRadiusKey, defaultValue = 2f
    )
    val (lensHeight, onLensHeightChange) = rememberPreference(
        LiquidGlassLensHeightKey, defaultValue = 0.4f
    )
    val (lensAmount, onLensAmountChange) = rememberPreference(
        LiquidGlassLensAmountKey, defaultValue = 0.6f
    )
    val (chromaticAberration, onChromaticAberrationChange) = rememberPreference(
        LiquidGlassChromaticAberrationKey, defaultValue = false
    )
    val (depthEffect, onDepthEffectChange) = rememberPreference(
        LiquidGlassDepthEffectKey, defaultValue = false
    )
    // 0 marks the theme-adaptive default tint (see MainActivity); the picker then
    // shows the color the current theme resolves to.
    val (surfaceTintColorInt, onSurfaceTintColorChange) = rememberPreference(
        LiquidGlassSurfaceTintColorKey, defaultValue = Color(0xFF1A1A1A).toArgb()
    )
    val adaptiveTintColor = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFAFAFA)
    } else {
        Color(0xFF4A4A4E)
    }
    val surfaceTintColor = if (surfaceTintColorInt == 0) {
        adaptiveTintColor
    } else {
        Color(surfaceTintColorInt)
    }
    val (surfaceOpacity, onSurfaceOpacityChange) = rememberPreference(
        LiquidGlassSurfaceOpacityKey, defaultValue = 0.5f
    )
    val (textColorInt, onTextColorChange) = rememberPreference(
        LiquidGlassTextColorKey, defaultValue = Color.White.toArgb()
    )
    val textColor = remember(textColorInt) { Color(textColorInt) }
    val (miniPlayerEnabled, onMiniPlayerEnabledChange) = rememberPreference(
        LiquidGlassMiniPlayerEnabledKey, defaultValue = true
    )
    val (navBarEnabled, onNavBarEnabledChange) = rememberPreference(
        LiquidGlassNavBarEnabledKey, defaultValue = true
    )
    val (sidePanelEnabled, onSidePanelEnabledChange) = rememberPreference(
        LiquidGlassSidePanelEnabledKey, defaultValue = true
    )
    val (sidePanelVibrancy, onSidePanelVibrancyChange) = rememberPreference(
        LiquidGlassSidePanelVibrancyKey, defaultValue = 1.2f
    )
    val (sidePanelBlurRadius, onSidePanelBlurRadiusChange) = rememberPreference(
        LiquidGlassSidePanelBlurRadiusKey, defaultValue = 2f
    )
    val (sidePanelLensHeight, onSidePanelLensHeightChange) = rememberPreference(
        LiquidGlassSidePanelLensHeightKey, defaultValue = 0.4f
    )
    val (sidePanelLensAmount, onSidePanelLensAmountChange) = rememberPreference(
        LiquidGlassSidePanelLensAmountKey, defaultValue = 0.6f
    )
    val (sidePanelColorInt, onSidePanelColorChange) = rememberPreference(
        LiquidGlassSidePanelColorKey, defaultValue = 0
    )
    val (sidePanelSurfaceOpacity, onSidePanelSurfaceOpacityChange) = rememberPreference(
        LiquidGlassSidePanelSurfaceOpacityKey, defaultValue = 0.5f
    )
    val (sidePanelTextColorInt, onSidePanelTextColorChange) = rememberPreference(
        LiquidGlassSidePanelTextColorKey, defaultValue = Color.White.toArgb()
    )
    val sidePanelTextColor = remember(sidePanelTextColorInt) { Color(sidePanelTextColorInt) }
    val sidePanelTintColor = if (sidePanelColorInt == 0) adaptiveTintColor else Color(sidePanelColorInt)

    // Enabling the side panel override starts it as a copy of the current
    // global values (rather than fixed defaults that may already differ from
    // global), so the look doesn't jump and there's an actual starting point
    // to dial independently from. Nav bar / mini bar has no override — it
    // always mirrors the global settings directly.
    val toggleSidePanel: (Boolean) -> Unit = { enabling ->
        if (enabling) {
            onSidePanelVibrancyChange(vibrancy)
            onSidePanelBlurRadiusChange(blurRadius)
            onSidePanelLensHeightChange(lensHeight)
            onSidePanelLensAmountChange(lensAmount)
            onSidePanelColorChange(surfaceTintColorInt)
            onSidePanelSurfaceOpacityChange(surfaceOpacity)
            onSidePanelTextColorChange(textColorInt)
        }
        onSidePanelEnabledChange(enabling)
    }

    var showVibrancyDialog by rememberSaveable { mutableStateOf(false) }
    var showBlurRadiusDialog by rememberSaveable { mutableStateOf(false) }
    var showLensHeightDialog by rememberSaveable { mutableStateOf(false) }
    var showLensAmountDialog by rememberSaveable { mutableStateOf(false) }
    var showSurfaceOpacityDialog by rememberSaveable { mutableStateOf(false) }
    var showSurfaceTintDialog by rememberSaveable { mutableStateOf(false) }
    var showSidePanelVibrancyDialog by rememberSaveable { mutableStateOf(false) }
    var showSidePanelBlurRadiusDialog by rememberSaveable { mutableStateOf(false) }
    var showSidePanelLensHeightDialog by rememberSaveable { mutableStateOf(false) }
    var showSidePanelLensAmountDialog by rememberSaveable { mutableStateOf(false) }
    var showSidePanelColorDialog by rememberSaveable { mutableStateOf(false) }
    var showSidePanelSurfaceOpacityDialog by rememberSaveable { mutableStateOf(false) }
    var showSidePanelTextColorDialog by rememberSaveable { mutableStateOf(false) }
    var showTextColorDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = stringResource(R.string.liquid_glass_effects),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass_vibrancy)) },
                    description = { Text(stringResource(R.string.liquid_glass_vibrancy_desc)) },
                    onClick = { showVibrancyDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.sliders),
                    title = { Text(stringResource(R.string.liquid_glass_blur_radius)) },
                    description = { Text(stringResource(R.string.liquid_glass_blur_radius_desc)) },
                    onClick = { showBlurRadiusDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass_lens_height)) },
                    onClick = { showLensHeightDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass_lens_amount)) },
                    onClick = { showLensAmountDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass_chromatic_aberration)) },
                    trailingContent = {
                        Switch(
                            checked = chromaticAberration,
                            onCheckedChange = onChromaticAberrationChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (chromaticAberration) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onChromaticAberrationChange(!chromaticAberration) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass_depth_effect)) },
                    trailingContent = {
                        Switch(
                            checked = depthEffect,
                            onCheckedChange = onDepthEffectChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (depthEffect) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onDepthEffectChange(!depthEffect) }
                ),
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.liquid_glass_appearance),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.liquid_glass_surface_tint)) },
                    description = { Text(stringResource(R.string.liquid_glass_surface_tint_desc)) },
                    onClick = { showSurfaceTintDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.liquid_glass_surface_opacity)) },
                    description = { Text(stringResource(R.string.liquid_glass_surface_opacity_desc)) },
                    onClick = { showSurfaceOpacityDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.liquid_glass_text_color)) },
                    description = { Text(stringResource(R.string.liquid_glass_text_color_desc)) },
                    onClick = { showTextColorDialog = true }
                ),
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.liquid_glass_per_component),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.music_note),
                    title = { Text(stringResource(R.string.liquid_glass_mini_player)) },
                    trailingContent = {
                        Switch(
                            checked = miniPlayerEnabled,
                            onCheckedChange = onMiniPlayerEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (miniPlayerEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onMiniPlayerEnabledChange(!miniPlayerEnabled) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text(stringResource(R.string.liquid_glass_nav_bar)) },
                    trailingContent = {
                        Switch(
                            checked = navBarEnabled,
                            onCheckedChange = onNavBarEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (navBarEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onNavBarEnabledChange(!navBarEnabled) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text(stringResource(R.string.liquid_glass_side_panel)) },
                    description = { Text(stringResource(R.string.liquid_glass_side_panel_desc)) },
                    trailingContent = {
                        Switch(
                            checked = sidePanelEnabled,
                            onCheckedChange = toggleSidePanel,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (sidePanelEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { toggleSidePanel(!sidePanelEnabled) }
                ),
            )
        )

        if (sidePanelEnabled) {
            Spacer(modifier = Modifier.height(27.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.liquid_glass_side_panel_overrides),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text(stringResource(R.string.liquid_glass_vibrancy)) },
                        onClick = { showSidePanelVibrancyDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.sliders),
                        title = { Text(stringResource(R.string.liquid_glass_blur_radius)) },
                        onClick = { showSidePanelBlurRadiusDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text(stringResource(R.string.liquid_glass_lens_height)) },
                        onClick = { showSidePanelLensHeightDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text(stringResource(R.string.liquid_glass_lens_amount)) },
                        onClick = { showSidePanelLensAmountDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.liquid_glass_surface_tint)) },
                        onClick = { showSidePanelColorDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text(stringResource(R.string.liquid_glass_surface_opacity)) },
                        onClick = { showSidePanelSurfaceOpacityDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.liquid_glass_text_color)) },
                        onClick = { showSidePanelTextColorDialog = true }
                    ),
                )
            )
        }


        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showVibrancyDialog) {
        var tempValue by remember { mutableFloatStateOf(vibrancy) }
        DefaultDialog(
            onDismiss = { tempValue = vibrancy; showVibrancyDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 1f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = vibrancy; showVibrancyDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onVibrancyChange(tempValue); showVibrancyDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_vibrancy), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.2f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..2f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showBlurRadiusDialog) {
        var tempValue by remember { mutableFloatStateOf(blurRadius) }
        DefaultDialog(
            onDismiss = { tempValue = blurRadius; showBlurRadiusDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 8f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = blurRadius; showBlurRadiusDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onBlurRadiusChange(tempValue); showBlurRadiusDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_blur_radius), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.0f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..100f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showLensHeightDialog) {
        var tempValue by remember { mutableFloatStateOf(lensHeight) }
        DefaultDialog(
            onDismiss = { tempValue = lensHeight; showLensHeightDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 0.5f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = lensHeight; showLensHeightDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onLensHeightChange(tempValue); showLensHeightDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_lens_height), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.2f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showLensAmountDialog) {
        var tempValue by remember { mutableFloatStateOf(lensAmount) }
        DefaultDialog(
            onDismiss = { tempValue = lensAmount; showLensAmountDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 0.5f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = lensAmount; showLensAmountDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onLensAmountChange(tempValue); showLensAmountDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_lens_amount), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.2f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showSidePanelVibrancyDialog) {
        var tempValue by remember { mutableFloatStateOf(sidePanelVibrancy) }
        DefaultDialog(
            onDismiss = { tempValue = sidePanelVibrancy; showSidePanelVibrancyDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 1.2f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = sidePanelVibrancy; showSidePanelVibrancyDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onSidePanelVibrancyChange(tempValue); showSidePanelVibrancyDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_vibrancy), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.2f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..2f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showSidePanelBlurRadiusDialog) {
        var tempValue by remember { mutableFloatStateOf(sidePanelBlurRadius) }
        DefaultDialog(
            onDismiss = { tempValue = sidePanelBlurRadius; showSidePanelBlurRadiusDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 2f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = sidePanelBlurRadius; showSidePanelBlurRadiusDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onSidePanelBlurRadiusChange(tempValue); showSidePanelBlurRadiusDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_blur_radius), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.0f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..100f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showSidePanelLensHeightDialog) {
        var tempValue by remember { mutableFloatStateOf(sidePanelLensHeight) }
        DefaultDialog(
            onDismiss = { tempValue = sidePanelLensHeight; showSidePanelLensHeightDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 0.4f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = sidePanelLensHeight; showSidePanelLensHeightDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onSidePanelLensHeightChange(tempValue); showSidePanelLensHeightDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_lens_height), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.2f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showSidePanelLensAmountDialog) {
        var tempValue by remember { mutableFloatStateOf(sidePanelLensAmount) }
        DefaultDialog(
            onDismiss = { tempValue = sidePanelLensAmount; showSidePanelLensAmountDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 0.6f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = sidePanelLensAmount; showSidePanelLensAmountDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onSidePanelLensAmountChange(tempValue); showSidePanelLensAmountDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_lens_amount), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.2f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showSidePanelSurfaceOpacityDialog) {
        var tempValue by remember { mutableFloatStateOf(sidePanelSurfaceOpacity) }
        DefaultDialog(
            onDismiss = { tempValue = sidePanelSurfaceOpacity; showSidePanelSurfaceOpacityDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 0.5f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = sidePanelSurfaceOpacity; showSidePanelSurfaceOpacityDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onSidePanelSurfaceOpacityChange(tempValue); showSidePanelSurfaceOpacityDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_surface_opacity), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.2f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showSidePanelColorDialog) {
        ColorPickerDialog(
            initialColor = sidePanelTintColor,
            title = stringResource(R.string.liquid_glass_surface_tint),
            onDismiss = { showSidePanelColorDialog = false },
            onConfirm = { color ->
                onSidePanelColorChange(color.toArgb())
                showSidePanelColorDialog = false
            },
            onReset = {
                onSidePanelColorChange(0)
                showSidePanelColorDialog = false
            },
        )
    }

    if (showSidePanelTextColorDialog) {
        ColorPickerDialog(
            initialColor = sidePanelTextColor,
            title = stringResource(R.string.liquid_glass_text_color),
            onDismiss = { showSidePanelTextColorDialog = false },
            onConfirm = { color ->
                onSidePanelTextColorChange(color.toArgb())
                showSidePanelTextColorDialog = false
            },
            defaultColor = Color.White,
        )
    }

    if (showSurfaceOpacityDialog) {
        var tempValue by remember { mutableFloatStateOf(surfaceOpacity) }
        DefaultDialog(
            onDismiss = { tempValue = surfaceOpacity; showSurfaceOpacityDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 0.3f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = surfaceOpacity; showSurfaceOpacityDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onSurfaceOpacityChange(tempValue); showSurfaceOpacityDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.liquid_glass_surface_opacity), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.2f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showSurfaceTintDialog) {
        ColorPickerDialog(
            initialColor = surfaceTintColor,
            title = stringResource(R.string.liquid_glass_surface_tint),
            onDismiss = { showSurfaceTintDialog = false },
            onConfirm = { color ->
                onSurfaceTintColorChange(color.toArgb())
                showSurfaceTintDialog = false
            },
            // Reset restores the theme-adaptive default rather than a fixed color.
            onReset = {
                onSurfaceTintColorChange(0)
                showSurfaceTintDialog = false
            },
        )
    }

    if (showTextColorDialog) {
        ColorPickerDialog(
            initialColor = textColor,
            title = stringResource(R.string.liquid_glass_text_color),
            onDismiss = { showTextColorDialog = false },
            onConfirm = { color ->
                onTextColorChange(color.toArgb())
                showTextColorDialog = false
            },
            defaultColor = Color.White,
        )
    }

    TopAppBar(
            windowInsets = appTopBarWindowInsets(),
        title = { Text(stringResource(R.string.liquid_glass_settings)) },
        navigationIcon = {
            AppIconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}
