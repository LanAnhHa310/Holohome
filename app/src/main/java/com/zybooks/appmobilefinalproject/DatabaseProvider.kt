package com.zybooks.appmobilefinalproject


import android.content.Context
import androidx.room.Room

// Helper object to provide the Room database.
// This keeps DB creation in one place and makes it easy
// to reuse from different activities.
object DatabaseProvider {
    @Volatile private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "furniture_db"
            ).build().also { INSTANCE = it }
        }
    }
}