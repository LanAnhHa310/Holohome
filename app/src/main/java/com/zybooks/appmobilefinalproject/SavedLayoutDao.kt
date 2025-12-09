package com.zybooks.appmobilefinalproject

import androidx.room.*

@Dao
interface SavedLayoutDao {
    @Query("SELECT * FROM saved_layouts")
    suspend fun getAll(): List<SavedLayoutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(layout: SavedLayoutEntity)

    @Delete
    suspend fun delete(layout: SavedLayoutEntity)
}
