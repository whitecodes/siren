package com.siren.player

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.siren.player.db.SirenDatabase
import com.siren.player.ui.theme.LanguageManager
import com.siren.player.ui.theme.ThemeManager

class SirenApp : Application() {

    val database by lazy { SirenDatabase.create(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ThemeManager.init(this)
        LanguageManager.init(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "塞壬唱片音乐播放控制"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "siren_music_channel"
    }
}
