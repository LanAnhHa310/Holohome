package com.zybooks.appmobilefinalproject

// Android + Room imports
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Room database for this app.
// Right now it stores only furniture items, but in the future
// can add more entities (e.g., users, saved layouts).
@Database(
    entities = [FurnitureEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // DAO (Data Access Object) that exposes queries for FurnitureEntity.
    abstract fun furnitureDao(): FurnitureDao

}
