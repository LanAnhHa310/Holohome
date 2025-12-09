package com.zybooks.appmobilefinalproject

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.*

@Database(
    entities = [FurnitureEntity::class, SavedLayoutEntity::class],
    version = 2, // bumped from 1 → 2 since schema changed
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // DAO (Data Access Object) that exposes queries for FurnitureEntity.
    abstract fun furnitureDao(): FurnitureDao

    // DAO for saved layouts (the new table)
    abstract fun savedLayoutDao(): SavedLayoutDao
}
