package com.siren.player.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

// Download status for songs
enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    DOWNLOAD_FAILED
}

// Task status for download queue
enum class TaskStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED;

    val isActive: Boolean get() = this == PENDING || this == DOWNLOADING || this == PAUSED
    val isCompleted: Boolean get() = this == COMPLETED
    val isFailed: Boolean get() = this == FAILED
}

// ---- Album Entity ----
@Entity(tableName = "albums")
data class Album(
    @PrimaryKey val cid: String,
    val name: String,
    val coverUrl: String,
    val artists: String,
    val intro: String = ""
)

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums WHERE cid = :cid LIMIT 1")
    suspend fun get(cid: String): Album?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: Album)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(albums: List<Album>)

    @Query("DELETE FROM albums WHERE cid = :cid")
    suspend fun delete(cid: String)

    @Query("SELECT * FROM albums")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<Album>>

    @Query("SELECT * FROM albums")
    suspend fun getAllList(): List<Album>

    @Query("SELECT COUNT(*) FROM albums")
    suspend fun count(): Int
}

// ---- Song Entity ----
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val cid: String,
    val name: String,
    val albumCid: String,
    val artists: String,
    val status: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val localPath: String? = null,
    val order: Int = 0
)

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE cid = :cid LIMIT 1")
    suspend fun get(cid: String): Song?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Song>)

    @Query("UPDATE songs SET status = :status WHERE cid = :cid")
    suspend fun updateStatus(cid: String, status: DownloadStatus)

    @Query("UPDATE songs SET localPath = :path, status = :status WHERE cid = :cid")
    suspend fun updateLocalPath(cid: String, path: String, status: DownloadStatus)

    @Query("SELECT * FROM songs WHERE albumCid = :albumCid")
    fun getAlbumSongs(albumCid: String): kotlinx.coroutines.flow.Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE albumCid = :albumCid ORDER BY `order` DESC")
    suspend fun getAlbumSongsList(albumCid: String): List<Song>

    @Query("SELECT localPath FROM songs WHERE cid = :cid LIMIT 1")
    suspend fun getLocalPath(cid: String): String?

    @Query("DELETE FROM songs WHERE cid = :cid")
    suspend fun delete(cid: String)

    @Query("SELECT COUNT(*) FROM songs WHERE albumCid = :albumCid")
    suspend fun countAlbumSongs(albumCid: String): Int

    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<Song>

    @Query("SELECT * FROM songs WHERE status = 'DOWNLOADED'")
    suspend fun getDownloadedSongs(): List<Song>
}

// ---- DownloadTask Entity ----
@Entity(tableName = "download_tasks")
data class DownloadTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songCid: String,
    val status: TaskStatus = TaskStatus.PENDING,
    val progress: Float = 0f,
    val pausePoint: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_tasks WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): DownloadTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DownloadTask): Long

    @Query("UPDATE download_tasks SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Float)

    @Query("UPDATE download_tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TaskStatus)

    @Query("UPDATE download_tasks SET status = :status, pausePoint = :pausePoint WHERE id = :id")
    suspend fun pauseTask(id: Long, status: TaskStatus = TaskStatus.PAUSED, pausePoint: Long)

    @Query("SELECT * FROM download_tasks WHERE status IN ('PENDING', 'DOWNLOADING', 'PAUSED')")
    fun getActiveTasks(): kotlinx.coroutines.flow.Flow<List<DownloadTask>>

    @Query("SELECT * FROM download_tasks WHERE songCid = :songCid LIMIT 1")
    suspend fun getBySongCid(songCid: String): DownloadTask?

    @Query("SELECT * FROM download_tasks WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED') ORDER BY createdAt DESC")
    fun getCompletedTasks(): kotlinx.coroutines.flow.Flow<List<DownloadTask>>

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun delete(id: Long)
}

// ---- Database Migration ----
val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create albums table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS albums (
                cid TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                coverUrl TEXT NOT NULL,
                artists TEXT NOT NULL
            )
        """)

        // Create songs table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS songs (
                cid TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                albumCid TEXT NOT NULL,
                artists TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'NOT_DOWNLOADED',
                localPath TEXT
            )
        """)

        // Create download_tasks table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS download_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                songCid TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'PENDING',
                progress REAL NOT NULL DEFAULT 0,
                pausePoint INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
        """)

        // Migrate existing cached_songs data to songs table
        db.execSQL("""
            INSERT INTO songs (cid, name, albumCid, artists, status, localPath)
            SELECT cid, name, albumCid, artists, 'DOWNLOADED', localPath
            FROM cached_songs
        """)

        // Drop old cached_songs table
        db.execSQL("DROP TABLE IF EXISTS cached_songs")
    }
}

// ---- Database Migration 2 -> 3 (add order column to songs) ----
val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE songs ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0")
    }
}

// ---- Database ----
@Database(
    entities = [Album::class, Song::class, DownloadTask::class],
    version = 3,
    exportSchema = false
)
abstract class SirenDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun songDao(): SongDao
    abstract fun downloadTaskDao(): DownloadTaskDao

    companion object {
        fun create(context: Context): SirenDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SirenDatabase::class.java,
                "siren_cache.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }
    }
}
