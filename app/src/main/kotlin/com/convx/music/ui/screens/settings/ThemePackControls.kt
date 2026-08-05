/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import com.convx.music.R
import com.convx.music.constants.ThemePack
import com.convx.music.ui.component.IconButton
import com.convx.music.ui.component.Material3SettingsGroup
import com.convx.music.ui.component.Material3SettingsItem
import com.convx.music.ui.component.TextFieldDialog
import com.convx.music.viewmodels.ThemePackViewModel
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Named theme presets: save the current accent/grid/font/background settings
 * as a portable file, apply a saved one instantly, or import one someone else
 * shared. Embedded inside [ThemeScreen] — same reasoning as
 * [HomeBackgroundControls], theme packs are a facet of "theme", not a separate
 * settings destination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePackControls() {
    val context = LocalContext.current
    val viewModel: ThemePackViewModel = hiltViewModel()
    val themePacks by viewModel.themePacks.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadThemePacks(context) }

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var pendingExportPack by remember { mutableStateOf<ThemePack?>(null) }
    val importSuccessMessage = stringResource(R.string.theme_pack_imported)
    val importFailedMessage = stringResource(R.string.theme_pack_import_failed)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val pack = pendingExportPack
        pendingExportPack = null
        if (uri != null && pack != null) {
            viewModel.exportThemePack(context, pack, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.importThemePackAsync(context, uri) { imported ->
            Toast.makeText(
                context,
                if (imported != null) importSuccessMessage else importFailedMessage,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    if (showSaveDialog) {
        TextFieldDialog(
            title = { Text(stringResource(R.string.theme_pack_name)) },
            initialTextFieldValue = TextFieldValue(""),
            onDismiss = { showSaveDialog = false },
            onDone = { name ->
                if (name.isNotBlank()) viewModel.saveCurrentAsThemePack(context, name.trim())
                showSaveDialog = false
            },
        )
    }

    Material3SettingsGroup(
        title = stringResource(R.string.theme_packs),
        items = buildList {
            add(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.theme_pack_save_current)) },
                    onClick = { showSaveDialog = true },
                )
            )
            add(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.restore),
                    title = { Text(stringResource(R.string.theme_pack_import)) },
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                )
            )
            themePacks.forEach { pack ->
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(pack.name) },
                        description = { Text(stringResource(R.string.theme_pack_apply_hint)) },
                        trailingContent = {
                            Row {
                                IconButton(
                                    onClick = {
                                        pendingExportPack = pack
                                        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                                        exportLauncher.launch(
                                            "${pack.name}_${LocalDateTime.now().format(formatter)}.json"
                                        )
                                    },
                                    onLongClick = {},
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(painterResource(R.drawable.backup), contentDescription = null)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteThemePack(context, pack.name) },
                                    onLongClick = {},
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Icon(
                                        painterResource(R.drawable.delete),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        },
                        onClick = { viewModel.applyThemePack(context, pack) },
                    )
                )
            }
        },
    )
}
