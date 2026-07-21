/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.HomeBackgroundBlurKey
import com.music.vivi.constants.HomeBackgroundDimKey
import com.music.vivi.constants.HomeBackgroundEnabledKey
import com.music.vivi.constants.HomeBackgroundPathKey
import com.music.vivi.ui.component.DefaultDialog
import com.music.vivi.ui.component.IconButton as AppIconButton
import com.music.vivi.ui.component.Material3SettingsGroup
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Copies a picked image into app storage so the background survives without a
 *  persistable URI permission. Unique filename cache-busts Coil. Returns the
 *  absolute path, or null on failure. */
private fun copyBackgroundImage(context: android.content.Context, source: Uri): String? = runCatching {
    val dest = File(context.filesDir, "home_background_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(source)?.use { input ->
        dest.outputStream().use { output -> input.copyTo(output) }
    } ?: return null
    dest.absolutePath
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBackgroundSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val (enabled, onEnabledChange) = rememberPreference(HomeBackgroundEnabledKey, defaultValue = false)
    val (path, onPathChange) = rememberPreference(HomeBackgroundPathKey, defaultValue = "")
    val (blur, onBlurChange) = rememberPreference(HomeBackgroundBlurKey, defaultValue = 20f)
    val (dim, onDimChange) = rememberPreference(HomeBackgroundDimKey, defaultValue = 0.4f)

    var showBlurDialog by rememberSaveable { mutableStateOf(false) }
    var showDimDialog by rememberSaveable { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val previous = path
        scope.launch {
            val newPath = withContext(Dispatchers.IO) { copyBackgroundImage(context, uri) }
            if (newPath != null) {
                onPathChange(newPath)
                if (!enabled) onEnabledChange(true)
                if (previous.isNotEmpty()) withContext(Dispatchers.IO) { File(previous).delete() }
            }
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = stringResource(R.string.home_background),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.home_background_enable)) },
                    trailingContent = {
                        Switch(
                            checked = enabled,
                            onCheckedChange = onEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (enabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onEnabledChange(!enabled) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.image),
                    title = { Text(stringResource(R.string.home_background_image)) },
                    description = {
                        Text(
                            if (path.isEmpty()) stringResource(R.string.home_background_image_none)
                            else stringResource(R.string.home_background_image_set)
                        )
                    },
                    onClick = {
                        pickLauncher.launch(
                            PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.sliders),
                    title = { Text(stringResource(R.string.home_background_blur)) },
                    onClick = { showBlurDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.home_background_dim)) },
                    onClick = { showDimDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.delete),
                    title = { Text(stringResource(R.string.home_background_remove)) },
                    onClick = {
                        val current = path
                        if (current.isNotEmpty()) scope.launch(Dispatchers.IO) { File(current).delete() }
                        onPathChange("")
                        onEnabledChange(false)
                    }
                ),
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showBlurDialog) {
        var tempValue by remember { mutableFloatStateOf(blur) }
        DefaultDialog(
            onDismiss = { tempValue = blur; showBlurDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 20f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = blur; showBlurDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onBlurChange(tempValue); showBlurDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.home_background_blur), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.0f".format(tempValue), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..50f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showDimDialog) {
        var tempValue by remember { mutableFloatStateOf(dim) }
        DefaultDialog(
            onDismiss = { tempValue = dim; showDimDialog = false },
            buttons = {
                TextButton(onClick = { tempValue = 0.4f }) { Text(stringResource(R.string.reset)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { tempValue = dim; showDimDialog = false }) { Text(stringResource(android.R.string.cancel)) }
                TextButton(onClick = { onDimChange(tempValue); showDimDialog = false }) { Text(stringResource(android.R.string.ok)) }
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.home_background_dim), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = "%.0f%%".format(tempValue * 100), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))
                Slider(value = tempValue, onValueChange = { tempValue = it }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.home_background)) },
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
