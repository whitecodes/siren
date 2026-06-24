package com.siren.player.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
fun SettingsScreen(
    viewModel: SirenViewModel,
    onLanguageChange: (LanguageMode) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showRebuildDatabaseDialog by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showThemeDropdown by remember { mutableStateOf(false) }
    var rebuildResult by remember { mutableStateOf<String?>(null) }
    val downloadPath by viewModel.downloadPath.collectAsState()
    val themeMode by ThemeManager.themeMode.collectAsState()
    val languageMode by LanguageManager.languageMode.collectAsState()
    val context = LocalContext.current
    val isInternalStorage = viewModel.downloadManager.isInternalStoragePath()

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setDownloadUri(it)
        }
    }

    val currentLanguageLabel = when (languageMode) {
        LanguageMode.CHINESE -> stringResource(R.string.lang_chinese)
        LanguageMode.ENGLISH -> stringResource(R.string.lang_english)
        LanguageMode.SYSTEM -> stringResource(R.string.lang_system)
    }

    val currentThemeLabel = when (themeMode) {
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
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

        // Language Setting - single row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Box {
                Row(
                    modifier = Modifier.clickable { showLanguageDropdown = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentLanguageLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = showLanguageDropdown,
                    onDismissRequest = { showLanguageDropdown = false }
                ) {
                    LanguageMode.entries.forEach { mode ->
                        val label = when (mode) {
                            LanguageMode.CHINESE -> stringResource(R.string.lang_chinese)
                            LanguageMode.ENGLISH -> stringResource(R.string.lang_english)
                            LanguageMode.SYSTEM -> stringResource(R.string.lang_system)
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    color = if (languageMode == mode) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                showLanguageDropdown = false
                                if (mode != languageMode) {
                                    onLanguageChange(mode)
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme Setting - single row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.theme),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Box {
                Row(
                    modifier = Modifier.clickable { showThemeDropdown = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentThemeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = showThemeDropdown,
                    onDismissRequest = { showThemeDropdown = false }
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val label = when (mode) {
                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    color = if (themeMode == mode) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                showThemeDropdown = false
                                if (mode != themeMode) {
                                    ThemeManager.setThemeMode(mode)
                                }
                            }
                        )
                    }
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

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showRebuildDatabaseDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(stringResource(R.string.rebuild_database))
        }

        if (rebuildResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = rebuildResult!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
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
            text = {
                Text(
                    stringResource(
                        if (isInternalStorage) R.string.clear_cache_confirm_internal
                        else R.string.clear_cache_confirm_external
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        android.util.Log.d("SettingsScreen", "Clear cache clicked, isInternalStorage=$isInternalStorage")
                        scope.launch {
                            try {
                                android.util.Log.d("SettingsScreen", "Starting clearCache...")
                                viewModel.clearCache()
                                android.util.Log.d("SettingsScreen", "clearCache completed")
                                // 清理完成后重启应用
                                android.util.Log.d("SettingsScreen", "Restarting app...")
                                val packageManager = context.packageManager
                                val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                                android.util.Log.d("SettingsScreen", "Launch intent: $intent")
                                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                android.util.Log.d("SettingsScreen", "startActivity called, calling exit(0)")
                                Runtime.getRuntime().exit(0)
                            } catch (e: Exception) {
                                android.util.Log.e("SettingsScreen", "Error in clearCache: ${e.message}", e)
                            }
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

    // Rebuild database dialog
    if (showRebuildDatabaseDialog) {
        AlertDialog(
            onDismissRequest = { showRebuildDatabaseDialog = false },
            title = { Text(stringResource(R.string.rebuild_database)) },
            text = { Text(stringResource(R.string.rebuild_database_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val updatedCount = viewModel.rebuildDatabase()
                                rebuildResult = context.getString(R.string.rebuild_database_success, updatedCount)
                            } catch (e: Exception) {
                                android.util.Log.e("SettingsScreen", "Error in rebuildDatabase: ${e.message}", e)
                                rebuildResult = "Error: ${e.message}"
                            }
                        }
                        showRebuildDatabaseDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebuildDatabaseDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.app_slogan),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.version),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AboutFeatureItem(
                icon = Icons.Default.Favorite,
                label = stringResource(R.string.feature_love),
                color = MaterialTheme.colorScheme.error
            )
            AboutFeatureItem(
                icon = Icons.Default.MusicNote,
                label = stringResource(R.string.feature_music),
                color = MaterialTheme.colorScheme.primary
            )
            AboutFeatureItem(
                icon = Icons.Default.Star,
                label = stringResource(R.string.feature_quality),
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/whitecodes/siren")
                    )
                    context.startActivity(intent)
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.github),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.github_url),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.made_with),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun AboutFeatureItem(
    icon: ImageVector,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.1f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = color
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
