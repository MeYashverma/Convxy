package com.music.vivi.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.LiquidGlassGlobalEnabledKey
import com.music.vivi.constants.UseFloatingNavBarKey
import com.music.vivi.ui.component.IconButton as AppIconButton
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassEffectSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    // Liquid glass is on by default; the toggle stays as the single option.
    val (globalEnabled, onGlobalEnabledChange) = rememberPreference(
        LiquidGlassGlobalEnabledKey, defaultValue = true
    )
    val (useFloatingNavBar, onUseFloatingNavBarChange) = rememberPreference(
        UseFloatingNavBarKey, defaultValue = true
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = stringResource(R.string.nav_bar_style),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.nav_bar),
                    title = { Text(stringResource(R.string.floating_nav_bar)) },
                    description = { Text(stringResource(R.string.floating_nav_bar_desc)) },
                    trailingContent = {
                        Switch(
                            checked = useFloatingNavBar,
                            onCheckedChange = onUseFloatingNavBarChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (useFloatingNavBar) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onUseFloatingNavBarChange(!useFloatingNavBar) }
                ),
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.liquid_glass),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.check),
                    title = { Text(stringResource(R.string.liquid_glass_global_enabled)) },
                    description = {
                        Text(
                            stringResource(
                                // Glass is part of the floating nav bar experience and only
                                // takes effect while that bar is enabled.
                                if (useFloatingNavBar) {
                                    R.string.liquid_glass_performance_warning
                                } else {
                                    R.string.liquid_glass_requires_floating_nav_bar
                                }
                            )
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = globalEnabled && useFloatingNavBar,
                            onCheckedChange = onGlobalEnabledChange,
                            enabled = useFloatingNavBar,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (globalEnabled && useFloatingNavBar) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { if (useFloatingNavBar) onGlobalEnabledChange(!globalEnabled) }
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
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
