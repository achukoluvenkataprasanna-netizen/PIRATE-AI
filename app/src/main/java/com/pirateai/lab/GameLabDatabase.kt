package com.pirateai.lab

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

@Entity(tableName = "demonstrations")
data class DemonstrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: String,
    val controlName: String,
    val observation: String,
    val recordedAt: Long = System.currentTimeMillis()
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

    @Insert
    suspend fun saveDemonstration(item: DemonstrationEntity)

    @Query("SELECT * FROM demonstrations WHERE gameId = :gameId ORDER BY recordedAt DESC")
    suspend fun demonstrations(gameId: String): List<DemonstrationEntity>
}

@Database(entities = [GameProfileEntity::class, PracticeSessionEntity::class, DemonstrationEntity::class], version = 2, exportSchema = false)
abstract class GameLabDatabase : RoomDatabase() {
    abstract fun dao(): GameLabDao
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS demonstrations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, gameId TEXT NOT NULL, controlName TEXT NOT NULL, observation TEXT NOT NULL, recordedAt INTEGER NOT NULL)")
            }
        }
        @Volatile private var INSTANCE: GameLabDatabase? = null
        fun get(context: Context): GameLabDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, GameLabDatabase::class.java, "pirate_game_lab.db").addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
