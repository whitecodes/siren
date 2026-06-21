package com.siren.player.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siren.player.R
import com.siren.player.ui.SirenViewModel
import com.siren.player.ui.theme.LanguageManager
import com.siren.player.ui.theme.LanguageMode
import com.siren.player.ui.theme.ThemeManager
import com.siren.player.ui.theme.ThemeMode
import kotlinx.coroutines.launch

fun uriToPath(uri: Uri): String {
    val docId = DocumentsContract.getTreeDocumentId(uri)
    if (docId.startsWith("primary:")) {
        return Environment.getExternalStorageDirectory().absolutePath + "/" + docId.removePrefix("primary:")
    }
    val split = docId.split(":")
    if (split.size >= 2) {
        val volId = split[0]
        val path = split[1]
        return "/storage/$volId/$path"
    }
    return uri.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SirenViewModel) {
    val scope = rememberCoroutineScope()
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showLanguageChangeDialog by remember { mutableStateOf(false) }
    var pendingLanguageMode by remember { mutableStateOf<LanguageMode?>(null) }
    val downloadPath by viewModel.downloadPath.collectAsState()
    val themeMode by ThemeManager.themeMode.collectAsState()
    val languageMode by LanguageManager.languageMode.collectAsState()
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = uriToPath(it)
            viewModel.setDownloadPath(path)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Language Section
        Text(
            text = stringResource(R.string.language),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LanguageMode.entries.forEach { mode ->
                val isSelected = languageMode == mode
                val label = when (mode) {
                    LanguageMode.CHINESE -> stringResource(R.string.lang_chinese)
                    LanguageMode.ENGLISH -> stringResource(R.string.lang_english)
                    LanguageMode.SYSTEM -> stringResource(R.string.lang_system)
                }
                Button(
                    onClick = {
                        Log.d("SettingsScreen", "Language button clicked: $mode, current: $languageMode, isSelected: $isSelected")
                        if (!isSelected) {
                            pendingLanguageMode = mode
                            showLanguageChangeDialog = true
                            Log.d("SettingsScreen", "showLanguageChangeDialog set to true")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme Section
        Text(
            text = stringResource(R.string.theme),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                val isSelected = themeMode == mode
                val label = when (mode) {
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                }
                Button(
                    onClick = { ThemeManager.setThemeMode(mode) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cache Section
        Text(
            text = stringResource(R.string.cache_management),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showClearCacheDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text(stringResource(R.string.clear_cache))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Download Path Section
        Text(
            text = stringResource(R.string.download_path),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = downloadPath,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { folderPicker.launch(null) }
                .padding(12.dp)
        )

        Text(
            text = stringResource(R.string.click_to_change_path),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }

    // Clear cache dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_cache)) },
            text = { Text(stringResource(R.string.clear_cache_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.clearCache()
                        }
                        showClearCacheDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Language change dialog
    if (showLanguageChangeDialog) {
        Log.d("SettingsScreen", "Showing language change dialog")
        AlertDialog(
            onDismissRequest = {
                showLanguageChangeDialog = false
                pendingLanguageMode = null
            },
            title = { Text(stringResource(R.string.language_change_title)) },
            text = { Text(stringResource(R.string.language_change_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingLanguageMode?.let { mode ->
                            LanguageManager.setLanguageMode(mode)
                        }
                        showLanguageChangeDialog = false
                        pendingLanguageMode = null
                        // Exit app completely
                        (context as? ComponentActivity)?.finishAffinity()
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLanguageChangeDialog = false
                        pendingLanguageMode = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}