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
    val recordedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val sequenceIndex: Int = 0,
    @ColumnInfo(defaultValue = "''") val actionId: String = "",
    @ColumnInfo(defaultValue = "''") val stateBefore: String = "",
    @ColumnInfo(defaultValue = "''") val stateAfter: String = "",
    @ColumnInfo(defaultValue = "'MANUAL'") val source: String = "MANUAL"
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

    @Query("SELECT * FROM demonstrations WHERE gameId = :gameId ORDER BY sequenceIndex ASC, recordedAt ASC")
    suspend fun demonstrations(gameId: String): List<DemonstrationEntity>
}

@Database(
    entities = [GameProfileEntity::class, PracticeSessionEntity::class, DemonstrationEntity::class],
    version = 3,
    exportSchema = false
)
abstract class GameLabDatabase : RoomDatabase() {
    abstract fun dao(): GameLabDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS demonstrations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, gameId TEXT NOT NULL, controlName TEXT NOT NULL, observation TEXT NOT NULL, recordedAt INTEGER NOT NULL)")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE demonstrations ADD COLUMN sequenceIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE demonstrations ADD COLUMN actionId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE demonstrations ADD COLUMN stateBefore TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE demonstrations ADD COLUMN stateAfter TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE demonstrations ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'")
            }
        }

        @Volatile private var INSTANCE: GameLabDatabase? = null
        fun get(context: Context): GameLabDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                GameLabDatabase::class.java,
                "pirate_game_lab.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
        }
    }
}
