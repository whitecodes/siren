package com.siren.player.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.siren.player.db.DownloadTask
import com.siren.player.db.SirenDatabase
import com.siren.player.db.TaskStatus
import com.siren.player.ui.SirenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueScreen(viewModel: SirenViewModel) {
    val database = viewModel.database
    val activeTasks by database.downloadTaskDao().getActiveTasks().collectAsState(initial = emptyList())
    val completedTasks by database.downloadTaskDao().getCompletedTasks().collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Active Downloads Section
        item {
            Text(
                text = "进行中 (${activeTasks.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (activeTasks.isEmpty()) {
            item {
                Text(
                    text = "暂无下载任务",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            items(activeTasks) { task ->
                ActiveDownloadItem(task = task)
            }
        }

        // Completed Downloads Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "已完成 (${completedTasks.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        if (completedTasks.isEmpty()) {
            item {
                Text(
                    text = "暂无历史下载",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            items(completedTasks) { task ->
                CompletedDownloadItem(task = task)
            }
        }
    }
}

@Composable
fun ActiveDownloadItem(task: DownloadTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (task.status) {
                    TaskStatus.PENDING -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    TaskStatus.DOWNLOADING -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    TaskStatus.PAUSED -> {
                        Icon(
                            Icons.Default.Pause,
                            contentDescription = "暂停",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "任务 #${task.id}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = task.status.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            if (task.status == TaskStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${(task.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun CompletedDownloadItem(task: DownloadTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (task.status == TaskStatus.COMPLETED) Icons.Default.CheckCircle
                else Icons.Default.Error,
                contentDescription = null,
                tint = if (task.status == TaskStatus.COMPLETED) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "任务 #${task.id}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = task.status.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
