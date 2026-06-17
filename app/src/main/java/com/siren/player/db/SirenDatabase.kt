package com.siren.player.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "cached_songs")
data class CachedSong(
    @PrimaryKey val cid: String,
    val name: String,
    val albumCid: String,
    val artists: String,
    val sourceUrl: String,
    val localPath: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Dao
interface SongCacheDao {
    @Query("SELECT * FROM cached_songs WHERE cid = :cid LIMIT 1")
    suspend fun get(cid: String): CachedSong?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: CachedSong)

    @Query("DELETE FROM cached_songs WHERE cid = :cid")
    suspend fun delete(cid: String)

    @Query("SELECT localPath FROM cached_songs WHERE cid = :cid LIMIT 1")
    suspend fun getLocalPath(cid: String): String?
}

@Database(entities = [CachedSong::class], version = 1, exportSchema = false)
abstract class SirenDatabase : RoomDatabase() {
    abstract fun songCacheDao(): SongCacheDao

    companion object {
        fun create(context: Context): SirenDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SirenDatabase::class.java,
                "siren_cache.db"
            ).build()
        }
    }
}
