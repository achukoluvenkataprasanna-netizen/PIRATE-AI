package com.pirateai.lab

import android.content.Context
import androidx.room.*

@Entity(tableName = "game_profiles")
data class GameProfileEntity(
    @PrimaryKey val gameId: String,
    val title: String,
    val genre: String,
    val objective: String,
    val controlsJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "practice_sessions")
data class PracticeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val attempts: Int = 0,
    val successes: Int = 0,
    val mistakes: Int = 0,
    val notes: String = ""
)

@Dao
interface GameLabDao {
    @Query("SELECT * FROM game_profiles ORDER BY updatedAt DESC")
    suspend fun profiles(): List<GameProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: GameProfileEntity)

    @Insert
    suspend fun saveSession(session: PracticeSessionEntity)

    @Query("SELECT * FROM practice_sessions WHERE gameId = :gameId ORDER BY startedAt DESC")
    suspend fun sessions(gameId: String): List<PracticeSessionEntity>
}

@Database(entities = [GameProfileEntity::class, PracticeSessionEntity::class], version = 1, exportSchema = false)
abstract class GameLabDatabase : RoomDatabase() {
    abstract fun dao(): GameLabDao
    companion object {
        @Volatile private var INSTANCE: GameLabDatabase? = null
        fun get(context: Context): GameLabDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, GameLabDatabase::class.java, "pirate_game_lab.db").build().also { INSTANCE = it }
        }
    }
}
