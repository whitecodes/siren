package com.siren.player.ui.screens

import com.siren.player.db.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadQueueScreenTest {

    @Test
    fun taskStatuses() {
        val statuses = TaskStatus.entries
        assertEquals(6, statuses.size)
        assertTrue(statuses.contains(TaskStatus.PENDING))
        assertTrue(statuses.contains(TaskStatus.DOWNLOADING))
        assertTrue(statuses.contains(TaskStatus.PAUSED))
        assertTrue(statuses.contains(TaskStatus.COMPLETED))
        assertTrue(statuses.contains(TaskStatus.FAILED))
        assertTrue(statuses.contains(TaskStatus.CANCELLED))
    }

    @Test
    fun isActiveStatus() {
        assertTrue(TaskStatus.PENDING.isActive)
        assertTrue(TaskStatus.DOWNLOADING.isActive)
        assertTrue(TaskStatus.PAUSED.isActive)
    }

    @Test
    fun isCompletedStatus() {
        assertTrue(TaskStatus.COMPLETED.isCompleted)
    }

    @Test
    fun isFailedStatus() {
        assertTrue(TaskStatus.FAILED.isFailed)
    }
}
