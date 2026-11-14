package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class, SavedTabEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VectraDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var instance: VectraDatabase? = null

        fun getInstance(context: Context): VectraDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VectraDatabase::class.java,
                    "vectra_browser.db"
                ).build().also { instance = it }
            }
        }
    }
}
