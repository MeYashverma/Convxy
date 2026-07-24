/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens.settings

import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.appTopBarWindowInsets
import android.net.Uri
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.activity.compose.rememberLauncherForActivityResult
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.activity.result.PickVisualMediaRequest
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.activity.result.contract.ActivityResultContracts
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.background
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Box
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Column
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.Spacer
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.aspectRatio
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.height
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.padding
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.size
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.draw.blur
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.draw.clip
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.graphics.Color
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.layout.ContentScale
import com.convx.music.ui.utils.appTopBarWindowInsets
import coil3.compose.AsyncImage
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.rememberScrollState
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.foundation.verticalScroll
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Icon
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.MaterialTheme
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Slider
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Switch
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.SwitchDefaults
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.Text
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TextButton
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TopAppBar
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.material3.TopAppBarScrollBehavior
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.Composable
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.getValue
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.mutableFloatStateOf
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.mutableStateOf
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.remember
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.rememberCoroutineScope
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.saveable.rememberSaveable
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.runtime.setValue
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.Alignment
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.Modifier
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.platform.LocalContext
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.res.painterResource
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.res.stringResource
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.compose.ui.unit.dp
import com.convx.music.ui.utils.appTopBarWindowInsets
import androidx.navigation.NavController
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.LocalPlayerAwareWindowInsets
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.R
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.HomeBackgroundAnimateKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.HomeBackgroundBlurKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.HomeBackgroundDimKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.HomeBackgroundEnabledKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.constants.HomeBackgroundPathKey
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.DefaultDialog
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.IconButton as AppIconButton
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.Material3SettingsGroup
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.component.Material3SettingsItem
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.ui.utils.backToMain
import com.convx.music.ui.utils.appTopBarWindowInsets
import com.convx.music.utils.rememberPreference
import com.convx.music.ui.utils.appTopBarWindowInsets
import kotlinx.coroutines.Dispatchers
import com.convx.music.ui.utils.appTopBarWindowInsets
import kotlinx.coroutines.launch
import com.convx.music.ui.utils.appTopBarWindowInsets
import kotlinx.coroutines.withContext
import com.convx.music.ui.utils.appTopBarWindowInsets
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
    val (animate, onAnimateChange) = rememberPreference(HomeBackgroundAnimateKey, defaultValue = false)

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
        // Live preview: image with the same blur + dim the home screen applies.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            if (path.isNotEmpty()) {
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blur.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = dim)),
                )
            } else {
                Text(
                    text = stringResource(R.string.home_background_image_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

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
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.home_background_animate)) },
                    description = { Text(stringResource(R.string.home_background_animate_desc)) },
                    trailingContent = {
                        Switch(
                            checked = animate,
                            onCheckedChange = onAnimateChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (animate) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAnimateChange(!animate) }
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
            windowInsets = appTopBarWindowInsets(),
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
