/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens.settings

import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.Image
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.background
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.border
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.bounceClick
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.combinedBounceClick
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Arrangement
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Box
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Column
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Row
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Spacer
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.height
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.only
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.padding
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.size
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.width
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.rememberScrollState
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.shape.CircleShape
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.verticalScroll
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material.icons.Icons
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material.icons.filled.BugReport
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material.icons.filled.Favorite
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material.icons.filled.History
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Icon
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.IconButton
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.MaterialShapes
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.MaterialTheme
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Surface
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Text
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TopAppBar
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TopAppBarScrollBehavior
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.toShape
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.Composable
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.remember
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.Alignment
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.Modifier
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.draw.clip
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.graphics.BlendMode
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.graphics.ColorFilter
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.layout.ContentScale
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.platform.LocalContext
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.platform.LocalUriHandler
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.res.painterResource
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.res.stringResource
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.text.font.FontWeight
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.unit.dp
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.unit.sp
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.navigation.NavController
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.BuildConfig
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.LocalPlayerAwareWindowInsets
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.R
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.IconButton
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.Material3SettingsGroup
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.Material3SettingsItem
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.backToMain
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.safeOpenUri
import com.convx.music.ui.utils.appTopBarWindowInsets
import java.text.SimpleDateFormat
import com.convx.music.ui.utils.appTopBarWindowInsets
import java.util.Date
import com.convx.music.ui.utils.appTopBarWindowInsets
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: (() -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val unknownString = stringResource(R.string.unknown)

    val cookieShape = MaterialShapes.Cookie7Sided.toShape()
    
    val installedDate = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val installTime = packageInfo.firstInstallTime
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(installTime))
        } catch (_: Exception) {
            unknownString
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.vivi_music_title),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.vivimusicnotification),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )

                Text(
                    text = "v${BuildConfig.VERSION_NAME} • ${stringResource(if (BuildConfig.IS_NIGHTLY) R.string.build_nightly else R.string.build_stable)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Developer Section
        Material3SettingsGroup(
            title = stringResource(R.string.developer_section),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.dev),
                    title = { Text(stringResource(R.string.developer_name)) },
                    description = { Text(stringResource(R.string.app_developer), color = MaterialTheme.colorScheme.primary) },
                    tintIcon = false,
                    iconShape = cookieShape,
                    onClick = { uriHandler.safeOpenUri(context, "https://github.com/cosmictaserdev-creator") }
                )
            )
        )
        Spacer(modifier = Modifier.height(27.dp))

        // Community Section
        Material3SettingsGroup(
            title = stringResource(R.string.community_section),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.github),
                    title = { Text(stringResource(R.string.github_repository)) },
                    description = { Text(stringResource(R.string.view_source_code)) },
                    onClick = { uriHandler.safeOpenUri(context, "https://github.com/cosmictaserdev-creator/Convx") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.discord),
                    title = { Text(stringResource(R.string.discord_channel)) },
                    description = { Text(stringResource(R.string.join_discord)) },
                    onClick = { uriHandler.safeOpenUri(context, "https://discord.gg/Ejeb4cmzfd") }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        // App Information Section
        Material3SettingsGroup(
            title = stringResource(R.string.app_info_section),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.deployed_app_update),
                    title = { Text(stringResource(R.string.installed_date_title)) },
                    description = { Text(installedDate) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.info),
                    title = { Text(stringResource(R.string.version_code)) },
                    description = { Text(BuildConfig.VERSION_CODE.toString()) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.license_vivi),
                    title = { Text(stringResource(R.string.license)) },
                    description = { Text("GPL-3.0 • Free Open Source Software") },
                    onClick = { uriHandler.safeOpenUri(context, "https://github.com/cosmictaserdev-creator/Convx/blob/main/LICENSE") }
                ),
            )
        )
        Spacer(modifier = Modifier.height(20.dp))
    }

    TopAppBar(
            windowInsets = appTopBarWindowInsets(),
        title = { Text(stringResource(R.string.about)) },
        navigationIcon = {
            IconButton(
                onClick = { onBack?.invoke() ?: navController.navigateUp() },
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}