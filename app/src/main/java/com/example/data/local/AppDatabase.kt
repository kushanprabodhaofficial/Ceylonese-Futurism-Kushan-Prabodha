package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BlogPostEntity::class, BlogPageEntity::class, BlogMetaEntity::class, StoryScoreEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun blogDao(): BlogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE blog_meta ADD COLUMN adsensePublisherId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE blog_meta ADD COLUMN adsenseSlotIdsDelimited TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS story_scores (
                        postId TEXT NOT NULL PRIMARY KEY,
                        storyId TEXT NOT NULL,
                        postTitle TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        maxScore INTEGER NOT NULL,
                        correctAnswers INTEGER NOT NULL,
                        totalQuestions INTEGER NOT NULL,
                        earnedAtMillis INTEGER NOT NULL,
                        syncedToCloud INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ceylonese_futurism_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
